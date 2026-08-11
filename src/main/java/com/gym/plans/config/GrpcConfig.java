package com.gym.plans.config;

import com.gym.common.grpc.interceptor.AuthServerInterceptor;
import com.gym.common.grpc.interceptor.ExceptionInterceptor;
import com.gym.common.grpc.interceptor.GrpcMethodRegistry;
import com.gym.common.grpc.interceptor.LoggingInterceptor;
import com.gym.common.grpc.interceptor.MetricsInterceptor;
import com.gym.common.grpc.interceptor.TracingInterceptor;
import com.gym.common.grpc.interceptor.ValidationInterceptor;
import com.gym.common.grpc.security.WorkloadIdentityVerifier;
import com.gym.plans.adapter.in.grpc.PlansGrpcHandler;
import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerInterceptor;
import io.grpc.TlsServerCredentials;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
public class GrpcConfig {

    private static final String GET_ACTIVE_GYM = "plans.v1.PlansService/GetActiveGym";
    private static final String RESOLVE_PURCHASABLE_PLAN = "plans.v1.PlansService/ResolvePurchasablePlan";

    private final PlansGrpcHandler plansGrpcHandler;
    private final List<ServerInterceptor> interceptors;

    @Value("${grpc.server.port:50051}")
    private int grpcPort;

    @Value("${grpc.server.tls.enabled:true}")
    private boolean tlsEnabled;

    @Value("${grpc.server.tls.allow-plaintext:false}")
    private boolean allowPlaintext;

    @Value("${grpc.server.tls.certificate-chain:}")
    private String certificateChain;

    @Value("${grpc.server.tls.private-key:}")
    private String privateKey;

    @Value("${grpc.server.tls.client-ca:}")
    private String clientCa;

    private Server server;

    @Bean("workloadAuthServerInterceptor")
    public static AuthServerInterceptor authServerInterceptor(
            GrpcMethodRegistry registry, WorkloadIdentityVerifier workloadIdentityVerifier) {
        return new AuthServerInterceptor(registry, workloadIdentityVerifier);
    }

    @Bean
    public static WorkloadIdentityVerifier workloadIdentityVerifier() {
        Map<String, Set<String>> methodAllowlist = Map.of(
                GET_ACTIVE_GYM, workloadSans("ms-gym-identifier"),
                RESOLVE_PURCHASABLE_PLAN, workloadSans("ms-gym-member"));
        return call -> PeerCertificateIdentity.hasAllowedSan(
                call, methodAllowlist.get(call.getMethodDescriptor().getFullMethodName()));
    }

    @Bean
    public static KongIdentityServerInterceptor kongIdentityServerInterceptor(GrpcMethodRegistry registry) {
        return new KongIdentityServerInterceptor(registry);
    }

    private static Set<String> workloadSans(String service) {
        return Set.of(
                service,
                "spiffe://gym.cluster.local/ns/default/sa/" + service,
                "spiffe://gym.cluster.local/ns/gym-system/sa/" + service);
    }

    public GrpcConfig(
            PlansGrpcHandler plansGrpcHandler,
            @org.springframework.beans.factory.annotation.Qualifier("workloadAuthServerInterceptor")
                    @org.springframework.context.annotation.Lazy
                    AuthServerInterceptor authServerInterceptor,
            KongIdentityServerInterceptor kongIdentityServerInterceptor,
            ExceptionInterceptor exceptionInterceptor,
            LoggingInterceptor loggingInterceptor,
            TracingInterceptor tracingInterceptor,
            MetricsInterceptor metricsInterceptor,
            ValidationInterceptor validationInterceptor) {
        this.plansGrpcHandler = plansGrpcHandler;
        this.interceptors = List.of(
                tracingInterceptor,
                loggingInterceptor,
                metricsInterceptor,
                exceptionInterceptor,
                authServerInterceptor,
                kongIdentityServerInterceptor,
                validationInterceptor);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startGrpcServer() throws IOException {
        server = serverBuilder()
                .addService(ServerInterceptors.intercept(plansGrpcHandler, interceptors))
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();
        log.info("gRPC server started on port {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server...");
            if (server != null) {
                server.shutdown();
            }
        }));
    }

    ServerBuilder<?> serverBuilder() throws IOException {
        if (!tlsEnabled) {
            if (!allowPlaintext) {
                throw new IllegalStateException("gRPC mTLS may only be disabled with explicit test configuration");
            }
            return ServerBuilder.forPort(grpcPort);
        }
        if (certificateChain.isBlank() || privateKey.isBlank() || clientCa.isBlank()) {
            throw new IllegalStateException("gRPC mTLS requires certificate-chain, private-key, and client-ca");
        }
        var credentials = TlsServerCredentials.newBuilder()
                .keyManager(new File(certificateChain), new File(privateKey))
                .trustManager(new File(clientCa))
                .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                .build();
        return Grpc.newServerBuilderForPort(grpcPort, credentials);
    }
}

package com.gym.plans.config;

import com.gym.common.grpc.interceptor.AuthServerInterceptor;
import com.gym.common.grpc.interceptor.ExceptionInterceptor;
import com.gym.common.grpc.interceptor.GrpcMethodRegistry;
import com.gym.common.grpc.interceptor.LoggingInterceptor;
import com.gym.common.grpc.interceptor.MetricsInterceptor;
import com.gym.common.grpc.interceptor.TracingInterceptor;
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

import javax.net.ssl.SSLSession;
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
                GET_ACTIVE_GYM,
                Set.of(
                        "ms-gym-identifier",
                        "spiffe://gym.cluster.local/ns/default/sa/ms-gym-identifier",
                        "spiffe://gym.cluster.local/ns/gym-system/sa/ms-gym-identifier"),
                RESOLVE_PURCHASABLE_PLAN,
                Set.of(
                        "ms-gym-member",
                        "spiffe://gym.cluster.local/ns/default/sa/ms-gym-member",
                        "spiffe://gym.cluster.local/ns/gym-system/sa/ms-gym-member"));
        return call -> {
            Set<String> allowed = methodAllowlist.get(call.getMethodDescriptor().getFullMethodName());
            if (allowed == null) {
                return false;
            }
            SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
            if (sslSession == null) {
                return false;
            }
            try {
                java.security.cert.Certificate[] certs = sslSession.getPeerCertificates();
                if (certs.length == 0 || !(certs[0] instanceof java.security.cert.X509Certificate x509)) {
                    return false;
                }
                var sanList = x509.getSubjectAlternativeNames();
                if (sanList == null) {
                    return false;
                }
                for (List<?> san : sanList) {
                    if (san.size() >= 2
                            && san.get(0) instanceof Integer type
                            && (type == 2 || type == 6)
                            && san.get(1) instanceof String sanValue
                            && allowed.contains(sanValue)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to verify mTLS workload identity", e);
            }
            return false;
        };
    }

    public GrpcConfig(
            PlansGrpcHandler plansGrpcHandler,
            @org.springframework.beans.factory.annotation.Qualifier("workloadAuthServerInterceptor")
                    @org.springframework.context.annotation.Lazy
                    AuthServerInterceptor authServerInterceptor,
            ExceptionInterceptor exceptionInterceptor,
            LoggingInterceptor loggingInterceptor,
            TracingInterceptor tracingInterceptor,
            MetricsInterceptor metricsInterceptor) {
        this.plansGrpcHandler = plansGrpcHandler;
        this.interceptors = List.of(
                tracingInterceptor,
                loggingInterceptor,
                metricsInterceptor,
                exceptionInterceptor,
                authServerInterceptor);
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

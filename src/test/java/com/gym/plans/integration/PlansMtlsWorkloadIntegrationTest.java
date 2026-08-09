package com.gym.plans.integration;

import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.PlansServiceGrpc;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live mTLS handshake matrix against local certs/local material.
 * Requires scripts/generate-local-certs.sh output under certs/local.
 */
@SpringBootTest
@ActiveProfiles({"test", "mtls"})
@EnabledIf("localCertsPresent")
class PlansMtlsWorkloadIntegrationTest {

    private static final Path CERT_DIR = Path.of("certs/local");

    static boolean localCertsPresent() {
        return Files.isRegularFile(CERT_DIR.resolve("server.crt"))
                && Files.isRegularFile(CERT_DIR.resolve("server.key"))
                && Files.isRegularFile(CERT_DIR.resolve("ca.crt"))
                && Files.isRegularFile(CERT_DIR.resolve("client-identifier.crt"))
                && Files.isRegularFile(CERT_DIR.resolve("client-identifier.key"))
                && Files.isRegularFile(CERT_DIR.resolve("client-member.crt"))
                && Files.isRegularFile(CERT_DIR.resolve("client-member.key"))
                && Files.isRegularFile(CERT_DIR.resolve("client-postman.crt"))
                && Files.isRegularFile(CERT_DIR.resolve("client-postman.key"));
    }

    @Value("${grpc.server.port}")
    private int grpcPort;

    @Autowired
    private GymLocationService gymLocationService;

    @Autowired
    private MembershipPlanService membershipPlanService;

    private GymLocationDto gym;
    private MembershipPlanDto plan;
    private ManagedChannel channel;

    @BeforeEach
    void setUpCatalog() {
        gym = gymLocationService.create("chain-mtls", "mTLS Gym", "1 St", "Hanoi");
        plan = membershipPlanService.create(gym.id(), "Monthly", "MONTHLY", 30, 100_000L, "", true);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow();
            channel.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void givenIdentifierCert_whenGetActiveGym_thenAllows() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-identifier");

        // When
        var response = stub.getActiveGym(
                GetActiveGymRequest.newBuilder().setGymId(gym.id()).build());

        // Then
        assertEquals(gym.id(), response.getId());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void givenMemberCert_whenResolvePurchasablePlan_thenAllows() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-member");

        // When
        var response = stub.resolvePurchasablePlan(ResolvePurchasablePlanRequest.newBuilder()
                .setPlanId(plan.id())
                .setGymId(gym.id())
                .build());

        // Then
        assertEquals(plan.id(), response.getPlanId());
        assertEquals(gym.id(), response.getGymId());
        assertEquals(100_000L, response.getPriceVnd());
    }

    @Test
    void givenMemberCert_whenGetActiveGym_thenPermissionDenied() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-member");

        // When
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> stub.getActiveGym(
                        GetActiveGymRequest.newBuilder().setGymId(gym.id()).build()));

        // Then
        assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
    }

    @Test
    void givenIdentifierCert_whenResolvePurchasablePlan_thenPermissionDenied() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-identifier");

        // When
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> stub.resolvePurchasablePlan(ResolvePurchasablePlanRequest.newBuilder()
                        .setPlanId(plan.id())
                        .setGymId(gym.id())
                        .build()));

        // Then
        assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
    }

    @Test
    void givenPostmanCert_whenGetActiveGym_thenPermissionDenied() throws Exception {
        // Given — valid CA client, wrong workload SAN
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-postman");

        // When
        StatusRuntimeException ex = assertThrows(
                StatusRuntimeException.class,
                () -> stub.getActiveGym(
                        GetActiveGymRequest.newBuilder().setGymId(gym.id()).build()));

        // Then
        assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
    }

    @Test
    void givenNoClientCert_whenConnect_thenHandshakeFails() {
        // Given / When
        Exception ex = assertThrows(Exception.class, () -> {
            ManagedChannel insecure = NettyChannelBuilder.forAddress("localhost", grpcPort)
                    .usePlaintext()
                    .build();
            try {
                PlansServiceGrpc.newBlockingStub(insecure)
                        .withDeadlineAfter(2, TimeUnit.SECONDS)
                        .getActiveGym(GetActiveGymRequest.newBuilder().setGymId(gym.id()).build());
            } finally {
                insecure.shutdownNow();
            }
        });

        // Then — plaintext against REQUIRE client-auth mTLS must fail
        assertTrue(
                ex instanceof StatusRuntimeException || ex.getCause() != null,
                "expected transport failure, got: " + ex);
    }

    private PlansServiceGrpc.PlansServiceBlockingStub stubWith(String clientName) throws SSLException {
        File ca = CERT_DIR.resolve("ca.crt").toFile();
        File cert = CERT_DIR.resolve(clientName + ".crt").toFile();
        File key = CERT_DIR.resolve(clientName + ".key").toFile();
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(ca)
                .keyManager(cert, key)
                .build();
        channel = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .sslContext(sslContext)
                .overrideAuthority("localhost")
                .build();
        return PlansServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
    }
}

package com.gym.plans.integration;

import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.GetGymLocationRequest;
import com.gym.proto.plans.v1.GetMembershipPlanRequest;
import com.gym.proto.plans.v1.ListGymLocationsRequest;
import com.gym.proto.plans.v1.ListMembershipPlansRequest;
import com.gym.proto.plans.v1.PlansServiceGrpc;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import com.gym.proto.plans.v1.ValidateCheckInGymRequest;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
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
import java.util.ArrayList;
import java.util.List;
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
                && clientCertPresent("client-gateway")
                && clientCertPresent("client-kong")
                && clientCertPresent("client-identifier")
                && clientCertPresent("client-member")
                && clientCertPresent("client-checkin")
                && clientCertPresent("client-notification")
                && clientCertPresent("client-postman");
    }

    private static boolean clientCertPresent(String clientName) {
        return Files.isRegularFile(CERT_DIR.resolve(clientName + ".crt"))
                && Files.isRegularFile(CERT_DIR.resolve(clientName + ".key"));
    }

    @Value("${grpc.server.port}")
    private int grpcPort;

    @Autowired
    private GymLocationService gymLocationService;

    @Autowired
    private MembershipPlanService membershipPlanService;

    private GymLocationDto gym;
    private MembershipPlanDto plan;
    private final List<ManagedChannel> channels = new ArrayList<>();

    @BeforeEach
    void setUpCatalog() {
        gym = gymLocationService.create("chain-mtls", "mTLS Gym", "1 St", "Hanoi");
        plan = membershipPlanService.create(gym.id(), "Monthly", "MONTHLY", 30, 100_000L, "", true);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        for (ManagedChannel channel : channels) {
            channel.shutdownNow();
            channel.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void givenGatewayCertAndValidClaims_whenCallPublicRpcs_thenAllowsAllOperations() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWithClaims("client-gateway", "super-1", "SUPER_ADMIN");

        // When
        var createdGym = stub.createGymLocation(CreateGymLocationRequest.newBuilder()
                .setChainId("chain-kong")
                .setName("Kong Gym")
                .setAddress("2 St")
                .setCity("Hanoi")
                .build());
        stub.updateGymLocation(UpdateGymLocationRequest.newBuilder()
                .setId(gym.id())
                .setChainId("chain-mtls")
                .setName("Updated mTLS Gym")
                .setAddress("1 St")
                .setCity("Hanoi")
                .setStatus(com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE)
                .build());
        var createdPlan = stub.createMembershipPlan(CreateMembershipPlanRequest.newBuilder()
                .setGymId(gym.id())
                .setName("Kong Monthly")
                .setPlanType(com.gym.proto.common.v1.PlanType.PLAN_TYPE_MONTHLY)
                .setDurationDays(30)
                .setPriceVnd(110_000L)
                .setDescription("Kong catalog plan")
                .setActive(true)
                .build());
        stub.updateMembershipPlan(UpdateMembershipPlanRequest.newBuilder()
                .setId(plan.id())
                .setName("Updated Monthly")
                .setPlanType(com.gym.proto.common.v1.PlanType.PLAN_TYPE_MONTHLY)
                .setDurationDays(30)
                .setPriceVnd(120_000L)
                .setDescription("Updated catalog plan")
                .setActive(true)
                .build());
        var gymResult = stub.getGymLocation(GetGymLocationRequest.newBuilder().setId(gym.id()).build());
        var gymsResult = stub.listGymLocations(ListGymLocationsRequest.newBuilder().setChainId("chain-mtls").build());
        var planResult = stub.getMembershipPlan(GetMembershipPlanRequest.newBuilder().setId(plan.id()).build());
        var plansResult = stub.listMembershipPlans(ListMembershipPlansRequest.newBuilder().setGymId(gym.id()).build());

        // Then
        assertEquals("Kong Gym", createdGym.getName());
        assertEquals("Kong Monthly", createdPlan.getName());
        assertEquals("Updated mTLS Gym", gymResult.getName());
        assertTrue(gymsResult.getLocationsCount() >= 1);
        assertEquals("Updated Monthly", planResult.getName());
        assertTrue(plansResult.getPlansCount() >= 2);
    }

    @Test
    void givenNonGatewayCertAndForgedSuperAdminClaims_whenCreateGymLocation_thenPermissionDenied() throws Exception {
        // Given
        CreateGymLocationRequest request = CreateGymLocationRequest.newBuilder()
                .setChainId("chain-forged")
                .setName("Forged Gym")
                .setAddress("3 St")
                .setCity("Hanoi")
                .build();

        // When / Then
        for (String clientName : List.of(
                "client-kong", "client-identifier", "client-member", "client-checkin", "client-notification", "client-postman")) {
            PlansServiceGrpc.PlansServiceBlockingStub stub = stubWithClaims(clientName, "attacker", "SUPER_ADMIN");
            StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.createGymLocation(request));
            assertEquals(Status.Code.PERMISSION_DENIED, exception.getStatus().getCode(), clientName);
        }
    }

    @Test
    void givenGatewayCertWithMissingOrConflictingClaims_whenCreateGymLocation_thenUnauthenticated() throws Exception {
        // Given
        CreateGymLocationRequest request = CreateGymLocationRequest.newBuilder()
                .setChainId("chain-metadata")
                .setName("Metadata Gym")
                .setAddress("4 St")
                .setCity("Hanoi")
                .build();

        // When / Then
        StatusRuntimeException missingClaims = assertThrows(
                StatusRuntimeException.class, () -> stubWith("client-gateway").createGymLocation(request));
        assertEquals(Status.Code.UNAUTHENTICATED, missingClaims.getStatus().getCode());

        Metadata conflictingClaims = claims("super-1", "SUPER_ADMIN");
        conflictingClaims.put(Metadata.Key.of("x-user-role", Metadata.ASCII_STRING_MARSHALLER), "CUSTOMER");
        PlansServiceGrpc.PlansServiceBlockingStub stub =
                stubWith("client-gateway").withInterceptors(MetadataUtils.newAttachHeadersInterceptor(conflictingClaims));
        StatusRuntimeException conflict = assertThrows(StatusRuntimeException.class, () -> stub.createGymLocation(request));
        assertEquals(Status.Code.UNAUTHENTICATED, conflict.getStatus().getCode());
    }

    @Test
    void givenGatewayCert_whenCallWorkloadRpc_thenPermissionDenied() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-gateway");

        // When / Then
        StatusRuntimeException getActiveGym = assertThrows(
                StatusRuntimeException.class,
                () -> stub.getActiveGym(GetActiveGymRequest.newBuilder().setGymId(gym.id()).build()));
        assertEquals(Status.Code.PERMISSION_DENIED, getActiveGym.getStatus().getCode());

        StatusRuntimeException resolvePlan = assertThrows(
                StatusRuntimeException.class,
                () -> stub.resolvePurchasablePlan(ResolvePurchasablePlanRequest.newBuilder()
                        .setPlanId(plan.id())
                        .setGymId(gym.id())
                        .build()));
        assertEquals(Status.Code.PERMISSION_DENIED, resolvePlan.getStatus().getCode());
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
        assertEquals(
                com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE,
                response.getStatus());
    }

    @Test
    void givenCheckinCert_whenValidateCheckInGym_thenAllowsCanonicalActiveGym() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-checkin");

        // When
        var response = stub.validateCheckInGym(
                ValidateCheckInGymRequest.newBuilder().setGymId(gym.id()).build());

        // Then
        assertEquals(gym.id(), response.getGymId());
        assertEquals(
                com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE,
                response.getStatus());
    }

    @Test
    void givenWrongWorkloadCert_whenValidateCheckInGym_thenPermissionDenied() throws Exception {
        // Given
        for (String clientName : List.of(
                "client-gateway", "client-kong", "client-identifier", "client-member", "client-notification", "client-postman")) {
            PlansServiceGrpc.PlansServiceBlockingStub stub = stubWithClaims(clientName, "forged-user", "SUPER_ADMIN");

            // When
            StatusRuntimeException exception = assertThrows(
                    StatusRuntimeException.class,
                    () -> stub.validateCheckInGym(ValidateCheckInGymRequest.newBuilder().setGymId(gym.id()).build()),
                    clientName);

            // Then
            assertEquals(Status.Code.PERMISSION_DENIED, exception.getStatus().getCode(), clientName);
        }
    }

    @Test
    void givenCheckinCertWithForgedClaims_whenValidateCheckInGym_thenAllowsByWorkloadIdentity() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWithClaims("client-checkin", "forged-user", "SUPER_ADMIN");

        // When
        var response = stub.validateCheckInGym(
                ValidateCheckInGymRequest.newBuilder().setGymId(gym.id()).build());

        // Then
        assertEquals(gym.id(), response.getGymId());
    }

    @Test
    void givenCheckinCertificate_whenValidateCheckInGymForMissingGym_thenNotFound() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-checkin");

        // When
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.validateCheckInGym(
                        ValidateCheckInGymRequest.newBuilder().setGymId("missing-gym").build()));

        // Then
        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
    }

    @Test
    void givenCheckinCertificate_whenValidateCheckInGymForClosedGym_thenFailedPrecondition() throws Exception {
        // Given
        gymLocationService.update(gym.id(), "chain-mtls", "mTLS Gym", "1 St", "Hanoi", "CLOSED");
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-checkin");

        // When
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.validateCheckInGym(
                        ValidateCheckInGymRequest.newBuilder().setGymId(gym.id()).build()));

        // Then
        assertEquals(Status.Code.FAILED_PRECONDITION, exception.getStatus().getCode());
    }

    @Test
    void givenCheckinCertificate_whenValidateCheckInGymWithoutGymId_thenInvalidArgument() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-checkin");

        // When
        StatusRuntimeException exception = assertThrows(
                StatusRuntimeException.class,
                () -> stub.validateCheckInGym(ValidateCheckInGymRequest.getDefaultInstance()));

        // Then
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    @Test
    void givenCheckinCertificate_whenCallOtherWorkloadRpcs_thenPermissionDenied() throws Exception {
        // Given
        PlansServiceGrpc.PlansServiceBlockingStub stub = stubWith("client-checkin");

        // When / Then
        StatusRuntimeException getActiveGym = assertThrows(
                StatusRuntimeException.class,
                () -> stub.getActiveGym(GetActiveGymRequest.newBuilder().setGymId(gym.id()).build()));
        StatusRuntimeException resolvePlan = assertThrows(
                StatusRuntimeException.class,
                () -> stub.resolvePurchasablePlan(ResolvePurchasablePlanRequest.newBuilder()
                        .setPlanId(plan.id())
                        .setGymId(gym.id())
                        .build()));
        assertEquals(Status.Code.PERMISSION_DENIED, getActiveGym.getStatus().getCode());
        assertEquals(Status.Code.PERMISSION_DENIED, resolvePlan.getStatus().getCode());
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
        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .sslContext(sslContext)
                .overrideAuthority("localhost")
                .build();
        channels.add(channel);
        return PlansServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private PlansServiceGrpc.PlansServiceBlockingStub stubWithClaims(String clientName, String userId, String role)
            throws SSLException {
        return stubWith(clientName).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(claims(userId, role)));
    }

    private static Metadata claims(String userId, String role) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), userId);
        metadata.put(Metadata.Key.of("x-user-role", Metadata.ASCII_STRING_MARSHALLER), role);
        return metadata;
    }
}

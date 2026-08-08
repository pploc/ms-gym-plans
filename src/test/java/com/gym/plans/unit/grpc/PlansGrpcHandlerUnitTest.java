package com.gym.plans.unit.grpc;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.grpc.PlansGrpcHandler;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.GetGymLocationRequest;
import com.gym.proto.plans.v1.GetMembershipPlanRequest;
import com.gym.proto.plans.v1.GymLocationResponse;
import com.gym.proto.plans.v1.GymLocationsResponse;
import com.gym.proto.plans.v1.ListGymLocationsRequest;
import com.gym.proto.plans.v1.ListMembershipPlansRequest;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.MembershipPlansResponse;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import com.gym.proto.plans.v1.ResolvedPlanResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlansGrpcHandlerUnitTest {

    @Mock
    private GymLocationService gymLocationService;

    @Mock
    private MembershipPlanService membershipPlanService;

    @InjectMocks
    private PlansGrpcHandler handler;

    @Test
    void givenCreateGymRequest_whenCreateGymLocation_thenMapsResponse() {
        // Given
        GymLocationDto dto =
                new GymLocationDto("g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE);
        when(gymLocationService.create("c1", "Central", "1 Main", "Hanoi")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<GymLocationResponse> observer = mock(StreamObserver.class);

        // When
        handler.createGymLocation(
                CreateGymLocationRequest.newBuilder()
                        .setChainId("c1")
                        .setName("Central")
                        .setAddress("1 Main")
                        .setCity("Hanoi")
                        .build(),
                observer);

        // Then
        ArgumentCaptor<GymLocationResponse> captor = ArgumentCaptor.forClass(GymLocationResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertEquals("g1", captor.getValue().getId());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void givenListPlansRequest_whenListMembershipPlans_thenMapsOptionalActive() {
        // Given
        MembershipPlanDto dto =
                new MembershipPlanDto("p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true);
        when(membershipPlanService.list("g1", "MONTHLY", true)).thenReturn(List.of(dto));
        @SuppressWarnings("unchecked")
        StreamObserver<MembershipPlansResponse> observer = mock(StreamObserver.class);

        // When
        handler.listMembershipPlans(
                ListMembershipPlansRequest.newBuilder()
                        .setGymId("g1")
                        .setPlanType("MONTHLY")
                        .setActive(true)
                        .build(),
                observer);

        // Then
        ArgumentCaptor<MembershipPlansResponse> captor =
                ArgumentCaptor.forClass(MembershipPlansResponse.class);
        verify(observer).onNext(captor.capture());
        assertEquals(1, captor.getValue().getPlansCount());
        assertEquals("p1", captor.getValue().getPlans(0).getId());
    }

    @Test
    void givenCreatePlanWithoutActive_whenCreateMembershipPlan_thenPassesNullActive() {
        // Given
        MembershipPlanDto dto =
                new MembershipPlanDto("p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true);
        when(membershipPlanService.create(eq("g1"), eq("Monthly"), eq("MONTHLY"), eq(30), eq(100L), eq("d"), isNull()))
                .thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<MembershipPlanResponse> observer = mock(StreamObserver.class);

        // When
        withSuperAdmin(() -> handler.createMembershipPlan(
                CreateMembershipPlanRequest.newBuilder()
                        .setGymId("g1")
                        .setName("Monthly")
                        .setPlanType("MONTHLY")
                        .setDurationDays(30)
                        .setPriceVnd(100L)
                        .setDescription("d")
                        .build(),
                observer));

        // Then
        verify(membershipPlanService)
                .create(eq("g1"), eq("Monthly"), eq("MONTHLY"), eq(30), eq(100L), eq("d"), isNull());
        verify(observer).onCompleted();
    }

    @Test
    void givenLifetimeResolvedPlan_whenResolvePurchasablePlan_thenOmitsDuration() {
        // Given
        ResolvedPlanDto dto = new ResolvedPlanDto("p1", "g1", PlanType.LIFETIME, null, 9_000L);
        when(membershipPlanService.resolvePurchasable("p1", "g1")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<ResolvedPlanResponse> observer = mock(StreamObserver.class);

        // When
        handler.resolvePurchasablePlan(
                ResolvePurchasablePlanRequest.newBuilder().setPlanId("p1").setGymId("g1").build(),
                observer);

        // Then
        ArgumentCaptor<ResolvedPlanResponse> captor = ArgumentCaptor.forClass(ResolvedPlanResponse.class);
        verify(observer).onNext(captor.capture());
        assertFalse(captor.getValue().hasDurationDays());
        assertEquals("LIFETIME", captor.getValue().getPlanType());
    }

    @Test
    void givenActiveGym_whenGetActiveGym_thenMapsResponse() {
        // Given
        GymLocationDto dto =
                new GymLocationDto("g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE);
        when(gymLocationService.getActive("g1")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<GymLocationResponse> observer = mock(StreamObserver.class);

        // When
        handler.getActiveGym(GetActiveGymRequest.newBuilder().setGymId("g1").build(), observer);

        // Then
        verify(observer).onNext(any(GymLocationResponse.class));
        verify(observer).onCompleted();
    }

    @Test
    void givenListGyms_whenListGymLocations_thenMapsCollection() {
        // Given
        when(gymLocationService.list("", "", ""))
                .thenReturn(List.of(
                        new GymLocationDto("g1", "c1", "A", "1", "HN", GymLocationStatus.ACTIVE)));
        @SuppressWarnings("unchecked")
        StreamObserver<GymLocationsResponse> observer = mock(StreamObserver.class);

        // When
        handler.listGymLocations(ListGymLocationsRequest.getDefaultInstance(), observer);

        // Then
        ArgumentCaptor<GymLocationsResponse> captor = ArgumentCaptor.forClass(GymLocationsResponse.class);
        verify(observer).onNext(captor.capture());
        assertEquals(1, captor.getValue().getLocationsCount());
    }

    @Test
    void givenGetPlan_whenGetMembershipPlan_thenMapsOptionalDuration() {
        // Given
        when(membershipPlanService.get("p1"))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true));
        @SuppressWarnings("unchecked")
        StreamObserver<MembershipPlanResponse> observer = mock(StreamObserver.class);

        // When
        handler.getMembershipPlan(GetMembershipPlanRequest.newBuilder().setId("p1").build(), observer);

        // Then
        ArgumentCaptor<MembershipPlanResponse> captor = ArgumentCaptor.forClass(MembershipPlanResponse.class);
        verify(observer).onNext(captor.capture());
        assertTrue(captor.getValue().hasDurationDays());
        assertEquals(30, captor.getValue().getDurationDays());
    }

    @Test
    void givenGetGym_whenGetGymLocation_thenMapsResponse() {
        // Given
        when(gymLocationService.get("g1"))
                .thenReturn(new GymLocationDto("g1", "c1", "A", "1", "HN", GymLocationStatus.ACTIVE));
        @SuppressWarnings("unchecked")
        StreamObserver<GymLocationResponse> observer = mock(StreamObserver.class);

        // When
        handler.getGymLocation(GetGymLocationRequest.newBuilder().setId("g1").build(), observer);

        // Then
        verify(observer).onCompleted();
    }

    @Test
    void givenUpdatePlan_whenUpdateMembershipPlan_thenMapsResponse() {
        // Given
        when(membershipPlanService.get("p1"))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true));
        when(membershipPlanService.update("p1", "Yearly", "YEARLY", 365, 200L, "y", false))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Yearly", PlanType.YEARLY, 365, 200L, "y", false));
        @SuppressWarnings("unchecked")
        StreamObserver<MembershipPlanResponse> observer = mock(StreamObserver.class);

        // When
        withSuperAdmin(() -> handler.updateMembershipPlan(
                UpdateMembershipPlanRequest.newBuilder()
                        .setId("p1")
                        .setName("Yearly")
                        .setPlanType("YEARLY")
                        .setDurationDays(365)
                        .setPriceVnd(200L)
                        .setDescription("y")
                        .setActive(false)
                        .build(),
                observer));

        // Then
        verify(observer).onCompleted();
    }

    @Test
    void givenUpdateGym_whenUpdateGymLocation_thenMapsResponse() {
        // Given
        when(gymLocationService.get("g1"))
                .thenReturn(new GymLocationDto("g1", "c1", "A", "1", "HN", GymLocationStatus.ACTIVE));
        when(gymLocationService.update("g1", "c1", "B", "2", "SG", "CLOSED"))
                .thenReturn(new GymLocationDto("g1", "c1", "B", "2", "SG", GymLocationStatus.CLOSED));
        @SuppressWarnings("unchecked")
        StreamObserver<GymLocationResponse> observer = mock(StreamObserver.class);

        // When
        withSuperAdmin(() -> handler.updateGymLocation(
                UpdateGymLocationRequest.newBuilder()
                        .setId("g1")
                        .setChainId("c1")
                        .setName("B")
                        .setAddress("2")
                        .setCity("SG")
                        .setStatus("CLOSED")
                        .build(),
                observer));

        // Then
        verify(observer).onCompleted();
    }

    private static void withSuperAdmin(Runnable action) {
        Context ctx = Context.current()
                .withValue(
                        GrpcSecurityContext.CLAIMS_KEY, new UserClaims("u1", "SUPER_ADMIN", "g1", "NONE"));
        Context previous = ctx.attach();
        try {
            action.run();
        } finally {
            ctx.detach(previous);
        }
    }
}

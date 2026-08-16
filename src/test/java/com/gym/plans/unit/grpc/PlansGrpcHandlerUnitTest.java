package com.gym.plans.unit.grpc;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.grpc.PlansGrpcHandler;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateGymLocationResponse;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanResponse;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.GetActiveGymResponse;
import com.gym.proto.plans.v1.GetGymLocationRequest;
import com.gym.proto.plans.v1.GetGymLocationResponse;
import com.gym.proto.plans.v1.GetMembershipPlanRequest;
import com.gym.proto.plans.v1.GetMembershipPlanResponse;
import com.gym.proto.plans.v1.ListGymLocationsRequest;
import com.gym.proto.plans.v1.ListGymLocationsResponse;
import com.gym.proto.plans.v1.ListMembershipPlansRequest;
import com.gym.proto.plans.v1.ListMembershipPlansResponse;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import com.gym.proto.plans.v1.ResolvePurchasablePlanResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateGymLocationResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanResponse;
import com.gym.proto.plans.v1.ValidateCheckInGymRequest;
import com.gym.proto.plans.v1.ValidateCheckInGymResponse;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mapstruct.factory.Mappers;
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

    private final GymLocationMapper gymLocationMapper = Mappers.getMapper(GymLocationMapper.class);
    private final MembershipPlanMapper membershipPlanMapper = Mappers.getMapper(MembershipPlanMapper.class);
    private PlansGrpcHandler handler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        handler = new PlansGrpcHandler(
                gymLocationService, membershipPlanService, gymLocationMapper, membershipPlanMapper);
    }

    @Test
    void givenCreateGymRequest_whenCreateGymLocation_thenMapsResponse() {
        GymLocationDto dto =
                new GymLocationDto("g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE, null, null);
        when(gymLocationService.create("c1", "Central", "1 Main", "Hanoi")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<CreateGymLocationResponse> observer = mock(StreamObserver.class);

        handler.createGymLocation(
                CreateGymLocationRequest.newBuilder()
                        .setChainId("c1")
                        .setName("Central")
                        .setAddress("1 Main")
                        .setCity("Hanoi")
                        .build(),
                observer);

        ArgumentCaptor<CreateGymLocationResponse> captor = ArgumentCaptor.forClass(CreateGymLocationResponse.class);
        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        assertEquals("g1", captor.getValue().getId());
        assertEquals(com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void givenListPlansRequest_whenListMembershipPlans_thenMapsOptionalActive() {
        MembershipPlanDto dto =
                new MembershipPlanDto("p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true, null, null);
        when(membershipPlanService.list("g1", "MONTHLY", true)).thenReturn(List.of(dto));
        @SuppressWarnings("unchecked")
        StreamObserver<ListMembershipPlansResponse> observer = mock(StreamObserver.class);

        handler.listMembershipPlans(
                ListMembershipPlansRequest.newBuilder()
                        .setGymId("g1")
                        .setPlanType(com.gym.proto.common.v1.PlanType.PLAN_TYPE_MONTHLY)
                        .setActive(true)
                        .build(),
                observer);

        ArgumentCaptor<ListMembershipPlansResponse> captor =
                ArgumentCaptor.forClass(ListMembershipPlansResponse.class);
        verify(observer).onNext(captor.capture());
        assertEquals(1, captor.getValue().getPlansCount());
        assertEquals("p1", captor.getValue().getPlans(0).getId());
    }

    @Test
    void givenCreatePlanWithoutActive_whenCreateMembershipPlan_thenPassesNullActive() {
        MembershipPlanDto dto =
                new MembershipPlanDto("p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true, null, null);
        when(membershipPlanService.create(eq("g1"), eq("Monthly"), eq("MONTHLY"), eq(30), eq(100L), eq("d"), isNull()))
                .thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<CreateMembershipPlanResponse> observer = mock(StreamObserver.class);

        withSuperAdmin(() -> handler.createMembershipPlan(
                CreateMembershipPlanRequest.newBuilder()
                        .setGymId("g1")
                        .setName("Monthly")
                        .setPlanType(com.gym.proto.common.v1.PlanType.PLAN_TYPE_MONTHLY)
                        .setDurationDays(30)
                        .setPriceVnd(100L)
                        .setDescription("d")
                        .build(),
                observer));

        verify(membershipPlanService)
                .create(eq("g1"), eq("Monthly"), eq("MONTHLY"), eq(30), eq(100L), eq("d"), isNull());
        verify(observer).onCompleted();
    }

    @Test
    void givenLifetimeResolvedPlan_whenResolvePurchasablePlan_thenOmitsDuration() {
        ResolvedPlanDto dto = new ResolvedPlanDto("p1", "g1", PlanType.LIFETIME, null, 9_000L);
        when(membershipPlanService.resolvePurchasable("p1", "g1")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<ResolvePurchasablePlanResponse> observer = mock(StreamObserver.class);

        handler.resolvePurchasablePlan(
                ResolvePurchasablePlanRequest.newBuilder().setPlanId("p1").setGymId("g1").build(),
                observer);

        ArgumentCaptor<ResolvePurchasablePlanResponse> captor = ArgumentCaptor.forClass(ResolvePurchasablePlanResponse.class);
        verify(observer).onNext(captor.capture());
        assertFalse(captor.getValue().hasDurationDays());
        assertEquals(com.gym.proto.common.v1.PlanType.PLAN_TYPE_LIFETIME, captor.getValue().getPlanType());
    }

    @Test
    void givenActiveGym_whenGetActiveGym_thenMapsResponse() {
        GymLocationDto dto =
                new GymLocationDto("g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE, null, null);
        when(gymLocationService.getActive("g1")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<GetActiveGymResponse> observer = mock(StreamObserver.class);

        handler.getActiveGym(GetActiveGymRequest.newBuilder().setGymId("g1").build(), observer);

        verify(observer).onNext(any(GetActiveGymResponse.class));
        verify(observer).onCompleted();
    }

    @Test
    void givenActiveGym_whenValidateCheckInGym_thenReturnsCanonicalActiveGym() {
        // given
        GymLocationDto dto =
                new GymLocationDto("gym-canonical", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE, null, null);
        when(gymLocationService.getActive("gym-request")).thenReturn(dto);
        @SuppressWarnings("unchecked")
        StreamObserver<ValidateCheckInGymResponse> observer = mock(StreamObserver.class);
        ArgumentCaptor<ValidateCheckInGymResponse> response = ArgumentCaptor.forClass(ValidateCheckInGymResponse.class);

        // when
        handler.validateCheckInGym(
                ValidateCheckInGymRequest.newBuilder().setGymId("gym-request").build(), observer);

        // then
        verify(observer).onNext(response.capture());
        verify(observer).onCompleted();
        assertEquals("gym-canonical", response.getValue().getGymId());
        assertEquals(
                com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE,
                response.getValue().getStatus());
    }

    @Test
    void givenListGyms_whenListGymLocations_thenMapsCollection() {
        when(gymLocationService.list("", "", null))
                .thenReturn(List.of(
                        new GymLocationDto("g1", "c1", "A", "1", "HN", GymLocationStatus.ACTIVE, null, null)));
        @SuppressWarnings("unchecked")
        StreamObserver<ListGymLocationsResponse> observer = mock(StreamObserver.class);

        handler.listGymLocations(ListGymLocationsRequest.getDefaultInstance(), observer);

        ArgumentCaptor<ListGymLocationsResponse> captor = ArgumentCaptor.forClass(ListGymLocationsResponse.class);
        verify(observer).onNext(captor.capture());
        assertEquals(1, captor.getValue().getLocationsCount());
    }

    @Test
    void givenGetPlan_whenGetMembershipPlan_thenMapsOptionalDuration() {
        when(membershipPlanService.get("p1"))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true, null, null));
        @SuppressWarnings("unchecked")
        StreamObserver<GetMembershipPlanResponse> observer = mock(StreamObserver.class);

        handler.getMembershipPlan(GetMembershipPlanRequest.newBuilder().setId("p1").build(), observer);

        ArgumentCaptor<GetMembershipPlanResponse> captor = ArgumentCaptor.forClass(GetMembershipPlanResponse.class);
        verify(observer).onNext(captor.capture());
        assertTrue(captor.getValue().hasDurationDays());
        assertEquals(30, captor.getValue().getDurationDays());
    }

    @Test
    void givenGetGym_whenGetGymLocation_thenMapsResponse() {
        when(gymLocationService.get("g1"))
                .thenReturn(new GymLocationDto("g1", "c1", "A", "1", "HN", GymLocationStatus.ACTIVE, null, null));
        @SuppressWarnings("unchecked")
        StreamObserver<GetGymLocationResponse> observer = mock(StreamObserver.class);

        handler.getGymLocation(GetGymLocationRequest.newBuilder().setId("g1").build(), observer);

        verify(observer).onCompleted();
    }

    @Test
    void givenUpdatePlan_whenUpdateMembershipPlan_thenMapsResponse() {
        when(membershipPlanService.update("p1", "Yearly", "YEARLY", 365, 200L, "y", false))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Yearly", PlanType.YEARLY, 365, 200L, "y", false, null, null));
        @SuppressWarnings("unchecked")
        StreamObserver<UpdateMembershipPlanResponse> observer = mock(StreamObserver.class);

        withSuperAdmin(() -> handler.updateMembershipPlan(
                UpdateMembershipPlanRequest.newBuilder()
                        .setId("p1")
                        .setName("Yearly")
                        .setPlanType(com.gym.proto.common.v1.PlanType.PLAN_TYPE_YEARLY)
                        .setDurationDays(365)
                        .setPriceVnd(200L)
                        .setDescription("y")
                        .setActive(false)
                        .build(),
                observer));

        verify(observer).onCompleted();
    }

    @Test
    void givenUpdateGym_whenUpdateGymLocation_thenMapsResponse() {
        when(gymLocationService.update("g1", "c1", "B", "2", "SG", "CLOSED"))
                .thenReturn(new GymLocationDto("g1", "c1", "B", "2", "SG", GymLocationStatus.CLOSED, null, null));
        @SuppressWarnings("unchecked")
        StreamObserver<UpdateGymLocationResponse> observer = mock(StreamObserver.class);

        withSuperAdmin(() -> handler.updateGymLocation(
                UpdateGymLocationRequest.newBuilder()
                        .setId("g1")
                        .setChainId("c1")
                        .setName("B")
                        .setAddress("2")
                        .setCity("SG")
                        .setStatus(com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_CLOSED)
                        .build(),
                observer));

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

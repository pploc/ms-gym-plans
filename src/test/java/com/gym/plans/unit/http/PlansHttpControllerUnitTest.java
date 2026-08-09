package com.gym.plans.unit.http;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.http.controller.GymLocationHttpController;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.adapter.in.http.controller.MembershipPlanHttpController;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.GymLocationResponse;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import io.grpc.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlansHttpControllerUnitTest {

    @Mock
    private GymLocationService gymLocationService;

    @Mock
    private MembershipPlanService membershipPlanService;

    private final GymLocationMapper gymLocationMapper = Mappers.getMapper(GymLocationMapper.class);
    private final MembershipPlanMapper membershipPlanMapper = Mappers.getMapper(MembershipPlanMapper.class);
    private GymLocationHttpController gymLocationHttpController;
    private MembershipPlanHttpController membershipPlanHttpController;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        gymLocationHttpController = new GymLocationHttpController(gymLocationService, gymLocationMapper);
        membershipPlanHttpController =
                new MembershipPlanHttpController(membershipPlanService, membershipPlanMapper);
    }

    @Test
    void givenCreateGymBody_whenCreate_thenMapsProtoResponse() {
        // Given
        when(gymLocationService.create("c1", "Central", "1 Main", "Hanoi"))
                .thenReturn(new GymLocationDto(
                        "g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE, null, null));

        // When
        GymLocationResponse response = gymLocationHttpController.create(
                CreateGymLocationRequest.newBuilder()
                        .setChainId("c1")
                        .setName("Central")
                        .setAddress("1 Main")
                        .setCity("Hanoi")
                        .build());

        // Then
        assertEquals("g1", response.getId());
        assertEquals("c1", response.getChainId());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void givenCreatePlanBody_whenCreate_thenUsesPathGymId() {
        // Given
        when(membershipPlanService.create("g1", "Monthly", "MONTHLY", 30, 100L, "d", null))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true, null, null));

        // When
        MembershipPlanResponse response = withSuperAdmin(() -> membershipPlanHttpController.create(
                "g1",
                CreateMembershipPlanRequest.newBuilder()
                        .setName("Monthly")
                        .setPlanType("MONTHLY")
                        .setDurationDays(30)
                        .setPriceVnd(100L)
                        .setDescription("d")
                        .build()));

        // Then
        assertEquals("p1", response.getId());
        assertEquals("g1", response.getGymId());
        assertEquals(30, response.getDurationDays());
        verify(membershipPlanService).create("g1", "Monthly", "MONTHLY", 30, 100L, "d", null);
    }

    @Test
    void givenUpdatePlanBody_whenUpdate_thenMapsProtoResponse() {
        // Given
        when(membershipPlanService.get("p1"))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true, null, null));
        when(membershipPlanService.update("p1", "Yearly", "YEARLY", 365, 200L, "yr", false))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Yearly", PlanType.YEARLY, 365, 200L, "yr", false, null, null));

        // When
        MembershipPlanResponse response = withSuperAdmin(() -> membershipPlanHttpController.update(
                "p1",
                UpdateMembershipPlanRequest.newBuilder()
                        .setName("Yearly")
                        .setPlanType("YEARLY")
                        .setDurationDays(365)
                        .setPriceVnd(200L)
                        .setDescription("yr")
                        .setActive(false)
                        .build()));

        // Then
        assertEquals("p1", response.getId());
        assertEquals("YEARLY", response.getPlanType());
        assertEquals(365, response.getDurationDays());
        assertFalse(response.getActive());
        verify(membershipPlanService).update("p1", "Yearly", "YEARLY", 365, 200L, "yr", false);
    }

    private static <T> T withSuperAdmin(java.util.function.Supplier<T> action) {
        Context ctx = Context.current()
                .withValue(
                        GrpcSecurityContext.CLAIMS_KEY, new UserClaims("u1", "SUPER_ADMIN", "g1", "NONE"));
        Context previous = ctx.attach();
        try {
            return action.get();
        } finally {
            ctx.detach(previous);
        }
    }
}

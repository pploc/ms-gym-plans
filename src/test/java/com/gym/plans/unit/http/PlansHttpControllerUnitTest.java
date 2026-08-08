package com.gym.plans.unit.http;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.http.GymLocationHttpController;
import com.gym.plans.adapter.in.http.MembershipPlanHttpController;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.CreateGymLocationRequest;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.GymLocationResponse;
import com.gym.plans.adapter.in.http.dto.MembershipPlanHttpDtos.CreateMembershipPlanRequest;
import com.gym.plans.adapter.in.http.dto.MembershipPlanHttpDtos.MembershipPlanResponse;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import io.grpc.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlansHttpControllerUnitTest {

    @Mock
    private GymLocationService gymLocationService;

    @Mock
    private MembershipPlanService membershipPlanService;

    @InjectMocks
    private GymLocationHttpController gymLocationHttpController;

    @InjectMocks
    private MembershipPlanHttpController membershipPlanHttpController;

    @Test
    void givenCreateGymBody_whenCreate_thenMapsSnakeCaseResponse() {
        // Given
        when(gymLocationService.create("c1", "Central", "1 Main", "Hanoi"))
                .thenReturn(new GymLocationDto(
                        "g1", "c1", "Central", "1 Main", "Hanoi", GymLocationStatus.ACTIVE));

        // When
        GymLocationResponse response = gymLocationHttpController.create(
                new CreateGymLocationRequest("c1", "Central", "1 Main", "Hanoi"));

        // Then
        assertEquals("g1", response.id());
        assertEquals("c1", response.chainId());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void givenCreatePlanBody_whenCreate_thenUsesPathGymId() {
        // Given
        when(membershipPlanService.create("g1", "Monthly", "MONTHLY", 30, 100L, "d", null))
                .thenReturn(new MembershipPlanDto(
                        "p1", "g1", "Monthly", PlanType.MONTHLY, 30, 100L, "d", true));

        // When
        MembershipPlanResponse response = withSuperAdmin(() -> membershipPlanHttpController.create(
                "g1",
                new CreateMembershipPlanRequest("Monthly", "MONTHLY", 30, 100L, "d", null)));

        // Then
        assertEquals("p1", response.id());
        assertEquals("g1", response.gymId());
        assertEquals(30, response.durationDays());
        verify(membershipPlanService).create("g1", "Monthly", "MONTHLY", 30, 100L, "d", null);
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

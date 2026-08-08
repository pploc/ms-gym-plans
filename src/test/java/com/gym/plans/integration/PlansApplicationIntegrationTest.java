package com.gym.plans.integration;

import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.domain.error.PlansDomainException;
import com.gym.plans.domain.error.PlansErrorCode;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PlansApplicationIntegrationTest {

    @Autowired
    private GymLocationService gymLocationService;

    @Autowired
    private MembershipPlanService membershipPlanService;

    @Test
    void givenPersistedCatalog_whenResolvePurchasable_thenReturnsFrozenTerms() {
        // Given
        GymLocationDto gym = gymLocationService.create("chain-it", "IT Gym", "1 St", "Hanoi");
        MembershipPlanDto plan = membershipPlanService.create(
                gym.id(), "Monthly", "MONTHLY", 30, 450_000L, "base", null);

        // When
        ResolvedPlanDto resolved = membershipPlanService.resolvePurchasable(plan.id(), gym.id());

        // Then
        assertEquals(plan.id(), resolved.planId());
        assertEquals(gym.id(), resolved.gymId());
        assertEquals(PlanType.MONTHLY, resolved.planType());
        assertEquals(30, resolved.durationDays());
        assertEquals(450_000L, resolved.priceVnd());
    }

    @Test
    void givenClosedGym_whenGetActive_thenRejects() {
        // Given
        GymLocationDto gym = gymLocationService.create("chain-it", "Closed Gym", "2 St", "Hanoi");
        gymLocationService.update(gym.id(), gym.chainId(), gym.name(), gym.address(), gym.city(), "CLOSED");

        // When
        PlansDomainException ex =
                assertThrows(PlansDomainException.class, () -> gymLocationService.getActive(gym.id()));

        // Then
        assertEquals(PlansErrorCode.GYM_INACTIVE, ex.getErrorCode());
        assertEquals(GymLocationStatus.CLOSED, gymLocationService.get(gym.id()).status());
    }

    @Test
    void givenDeactivatedPlan_whenResolve_thenRejects() {
        // Given
        GymLocationDto gym = gymLocationService.create("chain-it", "Active Gym", "3 St", "Hanoi");
        MembershipPlanDto plan = membershipPlanService.create(
                gym.id(), "Yearly", "YEARLY", 365, 3_000_000L, "yr", true);
        membershipPlanService.update(
                plan.id(), plan.name(), "YEARLY", 365, plan.priceVnd(), plan.description(), false);

        // When
        PlansDomainException ex = assertThrows(
                PlansDomainException.class,
                () -> membershipPlanService.resolvePurchasable(plan.id(), gym.id()));

        // Then
        assertEquals(PlansErrorCode.PLAN_INACTIVE, ex.getErrorCode());
        assertTrue(membershipPlanService.list(gym.id(), null, false).stream()
                .anyMatch(p -> p.id().equals(plan.id())));
    }
}

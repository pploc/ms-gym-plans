package com.gym.plans.integration;

import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PlansFilterIntegrationTest {

    @Autowired
    private GymLocationService gymLocationService;

    @Autowired
    private MembershipPlanService membershipPlanService;

    @Test
    void givenMixedLocations_whenListWithChainCityStatus_thenReturnsOnlyMatches() {
        // Given
        GymLocationDto keep = gymLocationService.create("chain-a", "A Hanoi", "1 St", "Hanoi");
        gymLocationService.create("chain-a", "A HCMC", "2 St", "HCMC");
        gymLocationService.create("chain-b", "B Hanoi", "3 St", "Hanoi");
        gymLocationService.update(
                keep.id(), keep.chainId(), keep.name(), keep.address(), keep.city(), "ACTIVE");
        GymLocationDto closed = gymLocationService.create("chain-a", "A Closed", "4 St", "Hanoi");
        gymLocationService.update(
                closed.id(), closed.chainId(), closed.name(), closed.address(), closed.city(), "CLOSED");

        // When
        List<GymLocationDto> result = gymLocationService.list("chain-a", "Hanoi", "ACTIVE");

        // Then
        assertEquals(1, result.size());
        assertEquals(keep.id(), result.getFirst().id());
        assertEquals(GymLocationStatus.ACTIVE, result.getFirst().status());
    }

    @Test
    void givenMixedPlans_whenListWithGymTypeActive_thenReturnsOnlyMatches() {
        // Given
        GymLocationDto gym = gymLocationService.create("chain-f", "Filter Gym", "1 St", "Hanoi");
        MembershipPlanDto monthlyActive = membershipPlanService.create(
                gym.id(), "Monthly", "MONTHLY", 30, 100L, "", true);
        membershipPlanService.create(gym.id(), "Yearly", "YEARLY", 365, 200L, "", true);
        MembershipPlanDto monthlyOff = membershipPlanService.create(
                gym.id(), "Monthly Off", "MONTHLY", 30, 50L, "", false);

        // When
        List<MembershipPlanDto> result = membershipPlanService.list(gym.id(), "MONTHLY", true);

        // Then
        assertEquals(1, result.size());
        assertEquals(monthlyActive.id(), result.getFirst().id());
        assertEquals(PlanType.MONTHLY, result.getFirst().planType());
        assertTrue(result.getFirst().active());
        assertTrue(membershipPlanService.list(gym.id(), "MONTHLY", false).stream()
                .anyMatch(p -> p.id().equals(monthlyOff.id())));
    }
}

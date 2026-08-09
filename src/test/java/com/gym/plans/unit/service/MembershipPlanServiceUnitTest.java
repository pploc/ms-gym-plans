package com.gym.plans.unit.service;

import com.gym.common.error.NotFoundException;
import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.adapter.out.persistence.repository.MembershipPlanJpaRepository;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.domain.error.PlansDomainException;
import com.gym.plans.domain.error.PlansErrorCode;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPlanServiceUnitTest {

    @Mock
    private MembershipPlanJpaRepository planRepository;

    @Mock
    private GymLocationService gymLocationService;

    @InjectMocks
    private MembershipPlanService membershipPlanService;

    private String gymId;
    private String planId;
    private GymLocationEntity gym;
    private MembershipPlanEntity plan;

    @BeforeEach
    void setUp() {
        gymId = UUID.randomUUID().toString();
        planId = UUID.randomUUID().toString();

        gym = new GymLocationEntity();
        gym.setId(gymId);
        gym.setStatus(GymLocationStatus.ACTIVE);

        plan = new MembershipPlanEntity();
        plan.setId(planId);
        plan.setGymId(gymId);
        plan.setName("Monthly");
        plan.setPlanType(PlanType.MONTHLY);
        plan.setDurationDays(30);
        plan.setPriceVnd(500_000L);
        plan.setDescription("std");
        plan.setActive(true);
    }

    @Test
    void givenValidMonthlyPlan_whenCreate_thenDefaultsActiveAndPersists() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);
        when(planRepository.save(any())).thenAnswer(inv -> {
            MembershipPlanEntity entity = inv.getArgument(0);
            entity.setId(planId);
            return entity;
        });

        // When
        MembershipPlanDto dto =
                membershipPlanService.create(gymId, "Monthly", "MONTHLY", 30, 500_000L, "std", null);

        // Then
        assertEquals(planId, dto.id());
        assertTrue(dto.active());
        assertEquals(30, dto.durationDays());
    }

    @Test
    void givenLifetimeWithDuration_whenCreate_thenRejects() {
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        assertThrows(
                IllegalArgumentException.class,
                () -> membershipPlanService.create(gymId, "Life", "LIFETIME", 30, 1_000L, "", true));
    }

    @Test
    void givenMonthlyWithoutDuration_whenCreate_thenRejects() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> membershipPlanService.create(gymId, "Monthly", "MONTHLY", null, 100L, "", true));
        assertEquals("duration_days must be positive for MONTHLY", ex.getMessage());
    }

    @Test
    void givenBlankName_whenCreate_thenRejects() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> membershipPlanService.create(gymId, "  ", "MONTHLY", 30, 100L, "", true));
        assertEquals("name is required", ex.getMessage());
    }

    @Test
    void givenLifetimePlan_whenCreate_thenStoresNullDuration() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);
        when(planRepository.save(any())).thenAnswer(inv -> {
            MembershipPlanEntity entity = inv.getArgument(0);
            entity.setId(planId);
            return entity;
        });

        // When
        MembershipPlanDto dto =
                membershipPlanService.create(gymId, "Life", "LIFETIME", null, 9_000_000L, "", true);

        // Then
        assertNull(dto.durationDays());
        assertEquals(PlanType.LIFETIME, dto.planType());
    }

    @Test
    void givenExistingPlan_whenUpdate_thenReplacesFields() {
        // Given
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        MembershipPlanDto dto =
                membershipPlanService.update(planId, "Yearly", "YEARLY", 365, 4_000_000L, "yr", false);

        // Then
        assertEquals("Yearly", dto.name());
        assertEquals(PlanType.YEARLY, dto.planType());
        assertEquals(365, dto.durationDays());
        assertFalse(dto.active());
    }

    @Test
    void givenMissingPlan_whenGet_thenThrowsNotFound() {
        when(planRepository.findById(planId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> membershipPlanService.get(planId));
    }

    @Test
    void givenGymAndFilters_whenList_thenUsesSpecifications() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);
        when(planRepository.findAll(any(Specification.class))).thenReturn(List.of(plan));

        // When
        List<MembershipPlanDto> result = membershipPlanService.list(gymId, "MONTHLY", true);

        // Then
        assertEquals(1, result.size());
        verify(planRepository).findAll(any(Specification.class));
    }

    @Test
    void givenActivePlanOnActiveGym_whenResolve_thenReturnsFrozenTerms() {
        // Given
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When
        ResolvedPlanDto dto = membershipPlanService.resolvePurchasable(planId, gymId);

        // Then
        assertEquals(planId, dto.planId());
        assertEquals(gymId, dto.gymId());
        assertEquals(PlanType.MONTHLY, dto.planType());
        assertEquals(30, dto.durationDays());
        assertEquals(500_000L, dto.priceVnd());
    }

    @Test
    void givenPlanForOtherGym_whenResolve_thenThrowsPlanGymMismatch() {
        // Given
        plan.setGymId("other-gym");
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When
        PlansDomainException ex = assertThrows(
                PlansDomainException.class, () -> membershipPlanService.resolvePurchasable(planId, gymId));

        // Then
        assertEquals(PlansErrorCode.PLAN_GYM_MISMATCH, ex.getErrorCode());
    }

    @Test
    void givenInactivePlan_whenResolve_thenThrowsPlanInactive() {
        // Given
        plan.setActive(false);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When
        PlansDomainException ex = assertThrows(
                PlansDomainException.class, () -> membershipPlanService.resolvePurchasable(planId, gymId));

        // Then
        assertEquals(PlansErrorCode.PLAN_INACTIVE, ex.getErrorCode());
    }

    @Test
    void givenClosedGym_whenResolve_thenThrowsGymInactive() {
        // Given
        gym.setStatus(GymLocationStatus.CLOSED);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When
        PlansDomainException ex = assertThrows(
                PlansDomainException.class, () -> membershipPlanService.resolvePurchasable(planId, gymId));

        // Then
        assertEquals(PlansErrorCode.GYM_INACTIVE, ex.getErrorCode());
    }
}

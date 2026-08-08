package com.gym.plans.domain.dto;

import com.gym.plans.domain.model.PlanType;

public record MembershipPlanDto(
        String id,
        String gymId,
        String name,
        PlanType planType,
        Integer durationDays,
        long priceVnd,
        String description,
        boolean active) {}

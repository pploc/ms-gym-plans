package com.gym.plans.domain.dto;

import com.gym.plans.domain.model.PlanType;

public record ResolvedPlanDto(
        String planId,
        String gymId,
        PlanType planType,
        Integer durationDays,
        long priceVnd) {}

package com.gym.plans.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateMembershipPlanRequest(
        String name,
        @JsonProperty("plan_type") String planType,
        @JsonProperty("duration_days") Integer durationDays,
        @JsonProperty("price_vnd") Long priceVnd,
        String description,
        Boolean active) {}

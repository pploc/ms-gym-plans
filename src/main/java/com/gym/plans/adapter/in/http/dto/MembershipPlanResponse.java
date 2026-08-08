package com.gym.plans.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.plans.domain.dto.MembershipPlanDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MembershipPlanResponse(
        String id,
        @JsonProperty("gym_id") String gymId,
        String name,
        @JsonProperty("plan_type") String planType,
        @JsonProperty("duration_days") Integer durationDays,
        @JsonProperty("price_vnd") long priceVnd,
        String description,
        boolean active) {

    public static MembershipPlanResponse from(MembershipPlanDto dto) {
        return new MembershipPlanResponse(
                dto.id(),
                dto.gymId(),
                dto.name(),
                dto.planType().name(),
                dto.durationDays(),
                dto.priceVnd(),
                dto.description(),
                dto.active());
    }
}

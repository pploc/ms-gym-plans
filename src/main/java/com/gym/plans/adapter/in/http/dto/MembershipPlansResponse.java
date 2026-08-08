package com.gym.plans.adapter.in.http.dto;

import com.gym.plans.domain.dto.MembershipPlanDto;

import java.util.List;

public record MembershipPlansResponse(List<MembershipPlanResponse> plans) {

    public static MembershipPlansResponse from(List<MembershipPlanDto> dtos) {
        return new MembershipPlansResponse(dtos.stream().map(MembershipPlanResponse::from).toList());
    }
}

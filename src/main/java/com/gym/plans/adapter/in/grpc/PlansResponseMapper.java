package com.gym.plans.adapter.in.grpc;

import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.proto.plans.v1.GymLocationResponse;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.ResolvedPlanResponse;

public final class PlansResponseMapper {

    private PlansResponseMapper() {}

    public static GymLocationResponse toGymResponse(GymLocationDto dto) {
        return GymLocationResponse.newBuilder()
                .setId(dto.id())
                .setChainId(dto.chainId())
                .setName(dto.name())
                .setAddress(dto.address())
                .setCity(dto.city())
                .setStatus(dto.status().name())
                .build();
    }

    public static MembershipPlanResponse toPlanResponse(MembershipPlanDto dto) {
        MembershipPlanResponse.Builder builder = MembershipPlanResponse.newBuilder()
                .setId(dto.id())
                .setGymId(dto.gymId())
                .setName(dto.name())
                .setPlanType(dto.planType().name())
                .setPriceVnd(dto.priceVnd())
                .setDescription(dto.description() == null ? "" : dto.description())
                .setActive(dto.active());
        if (dto.durationDays() != null) {
            builder.setDurationDays(dto.durationDays());
        }
        return builder.build();
    }

    public static ResolvedPlanResponse toResolvedResponse(ResolvedPlanDto dto) {
        ResolvedPlanResponse.Builder builder = ResolvedPlanResponse.newBuilder()
                .setPlanId(dto.planId())
                .setGymId(dto.gymId())
                .setPlanType(dto.planType().name())
                .setPriceVnd(dto.priceVnd());
        if (dto.durationDays() != null) {
            builder.setDurationDays(dto.durationDays());
        }
        return builder.build();
    }
}

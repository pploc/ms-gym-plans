package com.gym.plans.adapter.out.persistence.mapper;

import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.shared.mapper.ProtoEnums;
import com.gym.proto.plans.v1.CreateMembershipPlanResponse;
import com.gym.proto.plans.v1.GetMembershipPlanResponse;
import com.gym.proto.plans.v1.ResolvePurchasablePlanResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface MembershipPlanMapper {

    MembershipPlanDto toDto(MembershipPlanEntity entity);

    @Mapping(target = "planId", source = "id")
    ResolvedPlanDto toResolvedDto(MembershipPlanEntity entity);

    default CreateMembershipPlanResponse toCreateResponse(MembershipPlanDto dto) {
        CreateMembershipPlanResponse.Builder b = CreateMembershipPlanResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setGymId(nullToEmpty(dto.gymId()))
                .setName(nullToEmpty(dto.name()))
                .setPlanType(ProtoEnums.toProto(dto.planType()))
                .setPriceVnd(dto.priceVnd())
                .setDescription(nullToEmpty(dto.description()))
                .setActive(dto.active());
        if (dto.durationDays() != null) {
            b.setDurationDays(dto.durationDays());
        }
        return b.build();
    }

    default UpdateMembershipPlanResponse toUpdateResponse(MembershipPlanDto dto) {
        UpdateMembershipPlanResponse.Builder b = UpdateMembershipPlanResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setGymId(nullToEmpty(dto.gymId()))
                .setName(nullToEmpty(dto.name()))
                .setPlanType(ProtoEnums.toProto(dto.planType()))
                .setPriceVnd(dto.priceVnd())
                .setDescription(nullToEmpty(dto.description()))
                .setActive(dto.active());
        if (dto.durationDays() != null) {
            b.setDurationDays(dto.durationDays());
        }
        return b.build();
    }

    default GetMembershipPlanResponse toGetResponse(MembershipPlanDto dto) {
        GetMembershipPlanResponse.Builder b = GetMembershipPlanResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setGymId(nullToEmpty(dto.gymId()))
                .setName(nullToEmpty(dto.name()))
                .setPlanType(ProtoEnums.toProto(dto.planType()))
                .setPriceVnd(dto.priceVnd())
                .setDescription(nullToEmpty(dto.description()))
                .setActive(dto.active());
        if (dto.durationDays() != null) {
            b.setDurationDays(dto.durationDays());
        }
        return b.build();
    }

    default ResolvePurchasablePlanResponse toResolvedResponse(ResolvedPlanDto dto) {
        ResolvePurchasablePlanResponse.Builder b = ResolvePurchasablePlanResponse.newBuilder()
                .setPlanId(nullToEmpty(dto.planId()))
                .setGymId(nullToEmpty(dto.gymId()))
                .setPlanType(ProtoEnums.toProto(dto.planType()))
                .setPriceVnd(dto.priceVnd());
        if (dto.durationDays() != null) {
            b.setDurationDays(dto.durationDays());
        }
        return b.build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package com.gym.plans.adapter.out.persistence.mapper;

import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.ResolvedPlanResponse;
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

    @Mapping(target = "planType", expression = "java(dto.planType() != null ? dto.planType().name() : \"\")")
    @Mapping(target = "description", source = "description", defaultValue = "")
    MembershipPlanResponse toResponse(MembershipPlanDto dto);

    @Mapping(target = "planType", expression = "java(dto.planType() != null ? dto.planType().name() : \"\")")
    ResolvedPlanResponse toResolvedResponse(ResolvedPlanDto dto);
}

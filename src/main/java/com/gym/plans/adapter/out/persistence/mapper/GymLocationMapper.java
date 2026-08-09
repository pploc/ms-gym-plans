package com.gym.plans.adapter.out.persistence.mapper;

import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.proto.plans.v1.GymLocationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface GymLocationMapper {

    GymLocationDto toDto(GymLocationEntity entity);

    @Mapping(target = "status", expression = "java(dto.status() != null ? dto.status().name() : \"\")")
    GymLocationResponse toResponse(GymLocationDto dto);
}

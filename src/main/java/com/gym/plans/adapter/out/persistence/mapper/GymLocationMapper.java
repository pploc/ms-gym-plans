package com.gym.plans.adapter.out.persistence.mapper;

import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.shared.mapper.ProtoEnums;
import com.gym.proto.plans.v1.CreateGymLocationResponse;
import com.gym.proto.plans.v1.GetActiveGymResponse;
import com.gym.proto.plans.v1.GetGymLocationResponse;
import com.gym.proto.plans.v1.UpdateGymLocationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface GymLocationMapper {

    GymLocationDto toDto(GymLocationEntity entity);

    default CreateGymLocationResponse toCreateResponse(GymLocationDto dto) {
        return CreateGymLocationResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setChainId(nullToEmpty(dto.chainId()))
                .setName(nullToEmpty(dto.name()))
                .setAddress(nullToEmpty(dto.address()))
                .setCity(nullToEmpty(dto.city()))
                .setStatus(ProtoEnums.toProto(dto.status()))
                .build();
    }

    default UpdateGymLocationResponse toUpdateResponse(GymLocationDto dto) {
        return UpdateGymLocationResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setChainId(nullToEmpty(dto.chainId()))
                .setName(nullToEmpty(dto.name()))
                .setAddress(nullToEmpty(dto.address()))
                .setCity(nullToEmpty(dto.city()))
                .setStatus(ProtoEnums.toProto(dto.status()))
                .build();
    }

    default GetGymLocationResponse toGetResponse(GymLocationDto dto) {
        return GetGymLocationResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setChainId(nullToEmpty(dto.chainId()))
                .setName(nullToEmpty(dto.name()))
                .setAddress(nullToEmpty(dto.address()))
                .setCity(nullToEmpty(dto.city()))
                .setStatus(ProtoEnums.toProto(dto.status()))
                .build();
    }

    default GetActiveGymResponse toActiveResponse(GymLocationDto dto) {
        return GetActiveGymResponse.newBuilder()
                .setId(nullToEmpty(dto.id()))
                .setChainId(nullToEmpty(dto.chainId()))
                .setName(nullToEmpty(dto.name()))
                .setAddress(nullToEmpty(dto.address()))
                .setCity(nullToEmpty(dto.city()))
                .setStatus(ProtoEnums.toProto(dto.status()))
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

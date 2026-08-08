package com.gym.plans.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.plans.domain.dto.GymLocationDto;

import java.util.List;

public final class GymLocationHttpDtos {

    private GymLocationHttpDtos() {}

    public record CreateGymLocationRequest(
            @JsonProperty("chain_id") String chainId,
            String name,
            String address,
            String city) {}

    public record UpdateGymLocationRequest(
            @JsonProperty("chain_id") String chainId,
            String name,
            String address,
            String city,
            String status) {}

    public record GymLocationResponse(
            String id,
            @JsonProperty("chain_id") String chainId,
            String name,
            String address,
            String city,
            String status) {
        public static GymLocationResponse from(GymLocationDto dto) {
            return new GymLocationResponse(
                    dto.id(),
                    dto.chainId(),
                    dto.name(),
                    dto.address(),
                    dto.city(),
                    dto.status().name());
        }
    }

    public record GymLocationsResponse(List<GymLocationResponse> locations) {
        public static GymLocationsResponse from(List<GymLocationDto> dtos) {
            return new GymLocationsResponse(dtos.stream().map(GymLocationResponse::from).toList());
        }
    }
}

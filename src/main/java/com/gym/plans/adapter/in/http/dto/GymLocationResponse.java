package com.gym.plans.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gym.plans.domain.dto.GymLocationDto;

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

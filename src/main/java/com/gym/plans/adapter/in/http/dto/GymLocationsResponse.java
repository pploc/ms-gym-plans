package com.gym.plans.adapter.in.http.dto;

import com.gym.plans.domain.dto.GymLocationDto;

import java.util.List;

public record GymLocationsResponse(List<GymLocationResponse> locations) {

    public static GymLocationsResponse from(List<GymLocationDto> dtos) {
        return new GymLocationsResponse(dtos.stream().map(GymLocationResponse::from).toList());
    }
}

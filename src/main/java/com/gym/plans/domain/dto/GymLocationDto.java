package com.gym.plans.domain.dto;

import com.gym.plans.domain.model.GymLocationStatus;

public record GymLocationDto(
        String id,
        String chainId,
        String name,
        String address,
        String city,
        GymLocationStatus status) {}

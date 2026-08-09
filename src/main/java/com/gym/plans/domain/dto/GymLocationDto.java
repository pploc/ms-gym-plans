package com.gym.plans.domain.dto;

import com.gym.plans.domain.model.GymLocationStatus;
import java.time.Instant;

public record GymLocationDto(
        String id,
        String chainId,
        String name,
        String address,
        String city,
        GymLocationStatus status,
        Instant createdAt,
        Instant updatedAt) {}

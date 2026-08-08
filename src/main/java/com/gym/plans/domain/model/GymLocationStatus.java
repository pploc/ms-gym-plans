package com.gym.plans.domain.model;

public enum GymLocationStatus {
    ACTIVE,
    CLOSED;

    public static GymLocationStatus fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return GymLocationStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be ACTIVE or CLOSED");
        }
    }
}

package com.gym.plans.domain.model;

public enum PlanType {
    MONTHLY,
    YEARLY,
    LIFETIME;

    public static PlanType fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("plan_type is required");
        }
        try {
            return PlanType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("plan_type must be MONTHLY, YEARLY, or LIFETIME");
        }
    }

    public boolean requiresDuration() {
        return this == MONTHLY || this == YEARLY;
    }
}

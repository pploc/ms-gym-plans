package com.gym.plans.shared.mapper;

import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;

public final class ProtoEnums {

    private ProtoEnums() {}

    public static com.gym.proto.plans.v1.GymLocationStatus toProto(GymLocationStatus status) {
        if (status == null) {
            return com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_UNSPECIFIED;
        }
        return switch (status) {
            case ACTIVE -> com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_ACTIVE;
            case CLOSED -> com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_CLOSED;
        };
    }

    public static GymLocationStatus toDomain(com.gym.proto.plans.v1.GymLocationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        return switch (status) {
            case GYM_LOCATION_STATUS_ACTIVE -> GymLocationStatus.ACTIVE;
            case GYM_LOCATION_STATUS_CLOSED -> GymLocationStatus.CLOSED;
            case GYM_LOCATION_STATUS_UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException("status must be ACTIVE or CLOSED");
        };
    }

    /** Optional list filter: UNSPECIFIED means no filter. */
    public static String statusFilter(com.gym.proto.plans.v1.GymLocationStatus status) {
        if (status == null
                || status == com.gym.proto.plans.v1.GymLocationStatus.GYM_LOCATION_STATUS_UNSPECIFIED
                || status == com.gym.proto.plans.v1.GymLocationStatus.UNRECOGNIZED) {
            return null;
        }
        return toDomain(status).name();
    }

    public static com.gym.proto.common.v1.PlanType toProto(PlanType planType) {
        if (planType == null) {
            return com.gym.proto.common.v1.PlanType.PLAN_TYPE_UNSPECIFIED;
        }
        return switch (planType) {
            case MONTHLY -> com.gym.proto.common.v1.PlanType.PLAN_TYPE_MONTHLY;
            case YEARLY -> com.gym.proto.common.v1.PlanType.PLAN_TYPE_YEARLY;
            case LIFETIME -> com.gym.proto.common.v1.PlanType.PLAN_TYPE_LIFETIME;
        };
    }

    public static PlanType toDomain(com.gym.proto.common.v1.PlanType planType) {
        if (planType == null) {
            throw new IllegalArgumentException("plan_type is required");
        }
        return switch (planType) {
            case PLAN_TYPE_MONTHLY -> PlanType.MONTHLY;
            case PLAN_TYPE_YEARLY -> PlanType.YEARLY;
            case PLAN_TYPE_LIFETIME -> PlanType.LIFETIME;
            case PLAN_TYPE_UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException("plan_type must be MONTHLY, YEARLY, or LIFETIME");
        };
    }

    /** Optional list filter: UNSPECIFIED means no filter. */
    public static String planTypeFilter(com.gym.proto.common.v1.PlanType planType) {
        if (planType == null
                || planType == com.gym.proto.common.v1.PlanType.PLAN_TYPE_UNSPECIFIED
                || planType == com.gym.proto.common.v1.PlanType.UNRECOGNIZED) {
            return null;
        }
        return toDomain(planType).name();
    }
}

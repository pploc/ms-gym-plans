package com.gym.plans.adapter.in.grpc;

import com.gym.common.error.ForbiddenException;
import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.plans.domain.error.PlansErrorCode;

import java.util.Objects;

public final class GrpcAccessPolicy {

    private GrpcAccessPolicy() {}

    public static void requireGym(String gymId) {
        if (isSuperAdmin()) {
            return;
        }
        if (!Objects.equals(GrpcSecurityContext.getGymId(), gymId)) {
            throw new ForbiddenException(
                    PlansErrorCode.FORBIDDEN, "Gym access is outside the authenticated scope");
        }
    }

    private static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(GrpcSecurityContext.getRole());
    }
}

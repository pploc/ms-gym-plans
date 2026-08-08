package com.gym.plans.unit.grpc;

import com.gym.common.error.ForbiddenException;
import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import io.grpc.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrpcAccessPolicyUnitTest {

    @Test
    void givenMatchingGymAdmin_whenRequireGym_thenAllows() {
        // Given
        Context ctx = Context.current()
                .withValue(GrpcSecurityContext.CLAIMS_KEY, new UserClaims("u1", "ADMIN", "gym-1", "NONE"));

        // When / Then
        Context previous = ctx.attach();
        try {
            assertDoesNotThrow(() -> GrpcAccessPolicy.requireGym("gym-1"));
        } finally {
            ctx.detach(previous);
        }
    }

    @Test
    void givenDifferentGymAdmin_whenRequireGym_thenForbidden() {
        // Given
        Context ctx = Context.current()
                .withValue(GrpcSecurityContext.CLAIMS_KEY, new UserClaims("u1", "ADMIN", "gym-1", "NONE"));

        // When / Then
        Context previous = ctx.attach();
        try {
            assertThrows(ForbiddenException.class, () -> GrpcAccessPolicy.requireGym("gym-2"));
        } finally {
            ctx.detach(previous);
        }
    }

    @Test
    void givenSuperAdmin_whenRequireGym_thenAllowsAnyGym() {
        // Given
        Context ctx = Context.current()
                .withValue(
                        GrpcSecurityContext.CLAIMS_KEY, new UserClaims("u1", "SUPER_ADMIN", "gym-1", "NONE"));

        // When / Then
        Context previous = ctx.attach();
        try {
            assertDoesNotThrow(() -> GrpcAccessPolicy.requireGym("gym-99"));
        } finally {
            ctx.detach(previous);
        }
    }
}

package com.gym.plans.unit.http;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.http.filter.HttpRoleInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HttpRoleInterceptorUnitTest {

    private final HttpRoleInterceptor interceptor = new HttpRoleInterceptor();

    @Test
    void givenNonHandlerMethod_whenPreHandle_thenAllows() {
        // Given
        Object handler = new Object();

        // When / Then
        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handler));
    }

    @Test
    void givenMethodWithoutRequireRole_whenPreHandle_thenAllows() throws Exception {
        // Given
        HandlerMethod method = new HandlerMethod(new OpenController(), OpenController.class.getMethod("open"));

        // When / Then
        assertTrue(interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), method));
    }

    @Test
    void givenClassLevelRequireRoleAndClaims_whenPreHandle_thenAllows() throws Exception {
        // Given
        HandlerMethod method =
                new HandlerMethod(new ClassScopedController(), ClassScopedController.class.getMethod("read"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        // claims attached via filter in HTTP stack; interceptor only needs no annotation path for open methods.
        // Class-level RequireRole still requires claims — use SUPER_ADMIN via Context.
        io.grpc.Context ctx = io.grpc.Context.current()
                .withValue(
                        com.gym.common.grpc.security.GrpcSecurityContext.CLAIMS_KEY,
                        new com.gym.common.grpc.security.UserClaims("u1", "SUPER_ADMIN", null, "NONE"));
        io.grpc.Context previous = ctx.attach();
        try {
            assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), method));
        } finally {
            ctx.detach(previous);
        }
    }

    static class OpenController {
        public void open() {}
    }

    @RequireRole("SUPER_ADMIN")
    static class ClassScopedController {
        public void read() {}
    }
}

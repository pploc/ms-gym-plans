package com.gym.plans.adapter.in.http;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.RequireRole;
import com.gym.common.grpc.security.UserClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class HttpRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole requireRole = AnnotationUtils.findAnnotation(method.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotationUtils.findAnnotation(method.getBeanType(), RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        UserClaims claims = GrpcSecurityContext.getCurrentClaims();
        if (claims == null || claims.userId() == null || claims.userId().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        boolean allowed = Arrays.asList(requireRole.value()).contains(claims.role());
        if (!allowed) {
            throw new AccessDeniedException("Insufficient permissions");
        }
        return true;
    }
}

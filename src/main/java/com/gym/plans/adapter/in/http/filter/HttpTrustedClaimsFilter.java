package com.gym.plans.adapter.in.http.filter;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import io.grpc.Context;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Attaches Kong-injected end-user claims for {@code /api/**} requests.
 * Authorization decisions stay in {@link HttpRoleInterceptor}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpTrustedClaimsFilter extends OncePerRequestFilter {

    private static final Set<String> ROLES = Set.of("CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UserClaims claims = tryClaims(request);
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }
        Context ctx = Context.current().withValue(GrpcSecurityContext.CLAIMS_KEY, claims);
        Context previous = ctx.attach();
        try {
            filterChain.doFilter(request, response);
        } finally {
            ctx.detach(previous);
        }
    }

    private static UserClaims tryClaims(HttpServletRequest request) {
        String userId = trim(request.getHeader("x-user-id"));
        String role = upper(request.getHeader("x-user-role"));
        if (userId == null || role == null || !ROLES.contains(role)) {
            return null;
        }
        return new UserClaims(userId, role, null, UserClaims.NONE);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upper(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }
}

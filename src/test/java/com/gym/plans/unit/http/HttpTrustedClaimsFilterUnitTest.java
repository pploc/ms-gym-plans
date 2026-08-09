package com.gym.plans.unit.http;

import com.gym.common.grpc.security.GrpcSecurityContext;
import com.gym.common.grpc.security.UserClaims;
import com.gym.plans.adapter.in.http.filter.HttpTrustedClaimsFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpTrustedClaimsFilterUnitTest {

    private final HttpTrustedClaimsFilter filter = new HttpTrustedClaimsFilter();

    @Test
    void givenNullRequestUri_whenShouldNotFilter_thenSkips() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(null);

        // When
        boolean skip = (boolean) ReflectionTestUtils.invokeMethod(filter, "shouldNotFilter", request);

        // Then
        assertTrue(skip);
    }

    @Test
    void givenInvalidMembershipStatus_whenDoFilter_thenDefaultsMembershipToNone() throws Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/v1/gyms");
        when(request.getHeader("x-user-id")).thenReturn("u1");
        when(request.getHeader("x-user-role")).thenReturn("CUSTOMER");
        when(request.getHeader("x-gym-id")).thenReturn("g1");
        when(request.getHeader("x-membership-status")).thenReturn("BOGUS");

        FilterChain chain = (req, res) -> {
            UserClaims claims = GrpcSecurityContext.getCurrentClaims();
            assertEquals("u1", claims.userId());
            assertEquals("CUSTOMER", claims.role());
            assertEquals("g1", claims.gymId());
            assertEquals(UserClaims.NONE, claims.membershipStatus());
        };

        // When
        filter.doFilter(request, response, chain);

        // Then — after filter, claims context is detached
        assertNull(GrpcSecurityContext.getCurrentClaims());
        verify(request).getHeader("x-membership-status");
    }

    @Test
    void givenBlankUserIdHeader_whenDoFilter_thenLeavesClaimsAbsent() throws Exception {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/v1/gyms");
        when(request.getHeader("x-user-id")).thenReturn("   ");
        when(request.getHeader("x-user-role")).thenReturn("CUSTOMER");

        FilterChain chain = (req, res) -> assertNull(GrpcSecurityContext.getCurrentClaims());

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertNull(GrpcSecurityContext.getCurrentClaims());
    }
}

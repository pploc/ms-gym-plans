package com.gym.plans.adapter.in.http.exception;

import com.gym.common.error.ErrorResponse;
import com.gym.common.error.ErrorResponseFactory;
import com.gym.plans.domain.error.PlansErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PlansHttpExceptionHandler {

    private final ErrorResponseFactory errorResponses;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(errorResponses.create(PlansErrorCode.INVALID_ARGUMENT, ex.getMessage(), request));
    }
}

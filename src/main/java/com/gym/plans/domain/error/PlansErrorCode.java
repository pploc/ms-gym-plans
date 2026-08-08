package com.gym.plans.domain.error;

import com.gym.common.error.ErrorCategory;
import com.gym.common.error.ErrorCode;

public enum PlansErrorCode implements ErrorCode {
    INVALID_ARGUMENT(ErrorCategory.VALIDATION),
    GYM_NOT_FOUND(ErrorCategory.NOT_FOUND),
    PLAN_NOT_FOUND(ErrorCategory.NOT_FOUND),
    GYM_INACTIVE(ErrorCategory.UNPROCESSABLE),
    PLAN_INACTIVE(ErrorCategory.UNPROCESSABLE),
    PLAN_GYM_MISMATCH(ErrorCategory.UNPROCESSABLE),
    FORBIDDEN(ErrorCategory.FORBIDDEN);

    private final ErrorCategory category;

    PlansErrorCode(ErrorCategory category) {
        this.category = category;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}

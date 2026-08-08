package com.gym.plans.domain.error;

import com.gym.common.error.DomainException;
import com.gym.common.error.ErrorCode;

public class PlansDomainException extends DomainException {
    public PlansDomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

package com.yowyob.erp.shared.domain.exception;

import lombok.Getter;

/**
 * Custom exception for business logic errors.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Getter
public class BusinessException extends RuntimeException {
    /** Error code identifying the type of business error */
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
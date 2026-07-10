package com.yowyob.erp.shared.domain.exception;

import lombok.Getter;

/**
 * Conflit de version / sync offline (HTTP 409).
 */
@Getter
public class ConflictException extends RuntimeException {
    private final String errorCode;

    public ConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }
}

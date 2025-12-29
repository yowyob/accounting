package com.yowyob.erp.common.exception;

/**
 * Exception thrown for tenant-related errors.
 * 
 * @author ALD
 * @date 30.09.25
 */
public class TenantException extends RuntimeException {

    public TenantException(String message) {
        super(message);
    }

    public TenantException(String message, Throwable cause) {
        super(message, cause);
    }
}
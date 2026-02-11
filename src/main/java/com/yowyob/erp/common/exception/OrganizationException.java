package com.yowyob.erp.common.exception;

/**
 * Exception thrown for organization-related errors.
 * 
 * @author ALD
 * @date 30.09.25
 */
public class OrganizationException extends RuntimeException {

    public OrganizationException(String message) {
        super(message);
    }

    public OrganizationException(String message, Throwable cause) {
        super(message, cause);
    }
}

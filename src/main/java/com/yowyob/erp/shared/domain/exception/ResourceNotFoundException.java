package com.yowyob.erp.shared.domain.exception;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * @author ALD
 * @date 30.09.25
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s with ID %s not found", resource, id));
    }
}
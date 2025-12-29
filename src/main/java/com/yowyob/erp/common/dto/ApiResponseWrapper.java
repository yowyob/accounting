package com.yowyob.erp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Standardized API response wrapper for all Yowyob ERP modules.
 * 
 * Benefits:
 * - Uniform format (success/error)
 * - Complete traceability (timestamp, traceId)
 * - Usable across all REST microservices and Kafka
 * 
 * @param <T> the type of data contained in the response
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseWrapper<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Logical status of the operation */
    private boolean success;

    /** HTTP or internal status code */
    @Builder.Default
    private int statusCode = 200;

    /** Explanatory message */
    @Builder.Default
    private String message = "Operation successful";

    /** Return data */
    private T data;

    /** Exact processing time */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Traceability identifier for inter-service correlation */
    @Builder.Default
    private String traceId = UUID.randomUUID().toString();

    /** (Optional) request path */
    private String path;

    /**
     * Creates a successful response with data.
     * 
     * @param <T>  the type of data
     * @param data the response data
     * @return successful API response
     */
    public static <T> ApiResponseWrapper<T> success(T data) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .statusCode(200)
                .message("Operation successful")
                .data(data)
                .build();
    }

    /**
     * Creates a successful response with data and custom message.
     * 
     * @param <T>     the type of data
     * @param data    the response data
     * @param message custom success message
     * @return successful API response
     */
    public static <T> ApiResponseWrapper<T> success(T data, String message) {
        return ApiResponseWrapper.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates an error response with message.
     * 
     * @param <T>     the type of data
     * @param message error message
     * @return error API response
     */
    public static <T> ApiResponseWrapper<T> error(String message) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .build();
    }

    /**
     * Creates an error response with message and custom status code.
     * 
     * @param <T>     the type of data
     * @param message error message
     * @param code    HTTP status code
     * @return error API response
     */
    public static <T> ApiResponseWrapper<T> error(String message, int code) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .statusCode(code)
                .message(message)
                .build();
    }

    /**
     * Creates an error response with message and data (e.g., validation errors).
     * 
     * @param <T>     the type of data
     * @param message error message
     * @param data    error details
     * @return error API response
     */
    public static <T> ApiResponseWrapper<T> error(String message, T data) {
        return ApiResponseWrapper.<T>builder()
                .success(false)
                .statusCode(400)
                .message(message)
                .data(data)
                .build();
    }
}

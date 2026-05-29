package com.yowyob.erp.shared.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper.
 * 
 * @param <T> the type of data contained in the response
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String error;

    /**
     * Creates a successful response with data.
     * 
     * @param <T>  the type of data
     * @param data the response data
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a successful response with data and message.
     * 
     * @param <T>     the type of data
     * @param data    the response data
     * @param message success message
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message.
     * 
     * @param <T>     the type of data
     * @param message error message
     * @return error API response
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message and detailed error.
     * 
     * @param <T>     the type of data
     * @param message error message
     * @param error   detailed error description
     * @return error API response
     */
    public static <T> ApiResponse<T> error(String message, String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

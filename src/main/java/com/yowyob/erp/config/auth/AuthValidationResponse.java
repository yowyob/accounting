package com.yowyob.erp.config.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for validation response

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthValidationResponse {
    private boolean valid;
    private String userId;
    private String organizationId;
    private String[] roles;

    public static AuthValidationResponse invalid() {
        return AuthValidationResponse.builder()
                .valid(false)
                .build();
    }
}
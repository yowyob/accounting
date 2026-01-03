// DTO for user information
package com.yowyob.erp.config.auth;

import lombok.Data;

@Data
public class UserInfo {
    private String id;
    private String username;
    private String email;
    private String tenantId; // To check
    private String[] roles;
    private boolean active;
}
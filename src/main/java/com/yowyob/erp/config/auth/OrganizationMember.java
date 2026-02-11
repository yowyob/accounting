package com.yowyob.erp.config.auth;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class OrganizationMember {
    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private UUID agencyId;
    private String agencyName;
    private UUID roleId;
    private String roleName;
    private LocalDateTime joinedAt;
    private boolean active;
}

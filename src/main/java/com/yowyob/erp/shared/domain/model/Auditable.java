package com.yowyob.erp.shared.domain.model;

import java.util.UUID;
import java.time.LocalDateTime;

public interface Auditable {
    UUID getOrganizationId();
    void setOrganizationId(UUID organizationId);
    LocalDateTime getCreatedAt();
    void setCreatedAt(LocalDateTime createdAt);
    LocalDateTime getUpdatedAt();
    void setUpdatedAt(LocalDateTime updatedAt);
    String getCreatedBy();
    void setCreatedBy(String createdBy);
    String getUpdatedBy();
    void setUpdatedBy(String updatedBy);
}

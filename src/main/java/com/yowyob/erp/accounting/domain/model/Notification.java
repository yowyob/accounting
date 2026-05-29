package com.yowyob.erp.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("user_id")
    private String userId;

    private String title;
    private String message;
    private String type;

    @Column("reference_id")
    private String referenceId;

    @Column("is_read")
    private Boolean isRead;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("read_at")
    private LocalDateTime readAt;
}

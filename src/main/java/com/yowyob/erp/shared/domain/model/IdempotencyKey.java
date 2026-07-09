package com.yowyob.erp.shared.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "idempotency_keys")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IdempotencyKey implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private UUID entityId;

    @Column("http_status")
    private Integer httpStatus;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

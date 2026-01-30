package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import com.yowyob.erp.common.persistence.SettablePersistable;

import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a Tenant (e.g. a specific company or subsidiary) for
 * R2DBC.
 */
@Table(name = "tenants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("code")
    private String code;

    @Column("nom")
    private String nom;

    @Column("description")
    private String description;

    @Column("adresse")
    private String adresse;

    @Column("email")
    private String email;

    @Column("telephone")
    private String telephone;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Transient
    private Organization organization;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }

    public Tenant(UUID tenantId) {
        this.id = tenantId;
    }
}

package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.common.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "axes_analytiques")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AxeAnalytique implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("code")
    private String code;

    @Column("libelle")
    private String libelle;

    @Column("type")
    private String type; // DEPARTEMENT, PROJET, ACTIVITE, CENTRE_COUT

    @Column("responsable")
    private String responsable;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}

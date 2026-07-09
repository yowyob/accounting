package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "cles_repartition")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CleRepartition implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("code")
    private String code;

    @Column("libelle")
    private String libelle;

    @Column("type")
    private String type; // FIXE, COUT_UNITAIRE, UNITE_OEUVRE

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("created_by")
    private String createdBy;

    @Transient
    private List<CleRepartitionLigne> lignes;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

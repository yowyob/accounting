package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "fiches_cout_standard")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FicheCoutStandard implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("produit_code")
    private String produitCode;

    @Column("produit_libelle")
    private String produitLibelle;

    @Column("periode_ref_id")
    private UUID periodeRefId;

    @Column("plan_analytique_id")
    private String planAnalytiqueId;

    @Builder.Default
    @Column("periode_commencee")
    private Boolean periodeCommencee = false;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    private List<LigneCoutStandard> lignes;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

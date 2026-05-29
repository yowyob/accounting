package com.yowyob.erp.accounting.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JournalAudit entity traces actions like creation, validation, deletion, and
 * modification for R2DBC.
 */
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit implements SettablePersistable<UUID> {

    @Id
    @Column("journal_audit_id")
    private UUID id;

    @NotNull
    @Column("organization_id")
    private UUID organizationId;

    @Column("ecriture_id")
    private UUID ecriture_comptable_id;

    @Size(max = 100)
    @Column("action")
    private String action;

    @Column("date_action")
    private LocalDateTime date_action;

    @Column("utilisateur")
    private String utilisateur;

    @Column("details")
    private String details;

    @Column("adresse_ip")
    private String adresse_ip;

    @Column("donnees_avant")
    private String donnees_avant;

    @Column("donnees_apres")
    private String donnees_apres;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column("created_by")
    private String created_by;

    @Size(max = 255)
    @Column("updated_by")
    private String updated_by;

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
}

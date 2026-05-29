package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité de configuration des modes de saisie comptable.
 */
@Table("accounting_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingSetting implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("objet_type")
    private BrouillardType objetType;

    @Builder.Default
    @Column("mode_saisie")
    private ModeSaisie modeSaisie = ModeSaisie.SEMI_AUTOMATIQUE;

    @Column("montant_seuil")
    private BigDecimal montantSeuil;

    @Column("journal_id")
    private UUID journalId;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("description")
    private String description;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}

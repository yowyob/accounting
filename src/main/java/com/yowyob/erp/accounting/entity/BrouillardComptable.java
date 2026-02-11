package com.yowyob.erp.accounting.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.erp.common.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un brouillard comptable (draft).
 */
@Table("brouillards_comptables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrouillardComptable implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("type")
    private BrouillardType type;

    @Builder.Default
    @Column("statut")
    private BrouillardStatut statut = BrouillardStatut.BROUILLON;

    @Column("source_id")
    private String sourceId;

    @Column("source_type")
    private String sourceType;

    @Column("numero_piece")
    private String numeroPiece;

    @Column("date_piece")
    private LocalDate datePiece;

    @Column("libelle")
    private String libelle;

    @Column("montant_total")
    private BigDecimal montantTotal;

    @Column("devise")
    private String devise;

    @Column("journal_id")
    private UUID journalId;

    @Column("periode_id")
    private UUID periodeId;

    @Column("data_json")
    private JsonNode dataJson; // Utilisation de JsonNode pour JSONB

    @Column("ecriture_id")
    private UUID ecritureId;

    @Column("attachment_ids")
    private JsonNode attachmentIds;

    @Column("notes")
    private String notes;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("validated_by")
    private String validatedBy;

    @Column("validated_at")
    private LocalDateTime validatedAt;

    @Column("rejected_by")
    private String rejectedBy;

    @Column("rejected_at")
    private LocalDateTime rejectedAt;

    @Column("rejection_reason")
    private String rejectionReason;

    @Transient
    private JournalComptable journal;

    @Transient
    private PeriodeComptable periode;

    @Transient
    private EcritureComptable ecriture;

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

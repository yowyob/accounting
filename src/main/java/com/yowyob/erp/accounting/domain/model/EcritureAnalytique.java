package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "ecritures_analytiques")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EcritureAnalytique implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("journal_id")
    private UUID journalId;

    @Column("periode_id")
    private UUID periodeId;

    @Column("numero_piece")
    private String numeroPiece;

    @Column("libelle")
    private String libelle;

    @Column("date_effet")
    private LocalDate dateEffet;

    @Builder.Default
    @Column("origine")
    private String origine = "MANUELLE"; // MANUELLE, IMPORT_CG

    @Builder.Default
    @Column("statut")
    private String statut = "BROUILLON"; // BROUILLON, VALIDEE, REJETEE

    @Column("ecriture_cg_ref")
    private UUID ecriturecgRef;

    @Column("montant_total")
    private BigDecimal montantTotal;

    @Column("nature_charge_id")
    private UUID natureChargeId;

    @Column("validated_at")
    private LocalDateTime validatedAt;

    @Column("validated_by")
    private String validatedBy;

    @Column("reject_reason")
    private String rejectReason;

    @Column("client_id")
    private String clientId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    private List<LigneImputation> lignes;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

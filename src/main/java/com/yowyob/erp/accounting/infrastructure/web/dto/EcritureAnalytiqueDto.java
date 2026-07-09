package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EcritureAnalytiqueDto {
    private UUID id;

    /** Identifiant stable généré côté client (sync offline). */
    private String clientId;

    /** Clé d'idempotence optionnelle (corps ou en-tête Idempotency-Key). */
    private String clientMutationId;

    @NotNull
    private UUID journalId;
    private String journalLibelle;

    private UUID periodeId;
    private String periodeLibelle;

    private String numeroPiece;

    @NotNull
    private String libelle;

    @NotNull
    private LocalDate dateEffet;

    @Builder.Default
    private String origine = "MANUELLE";

    private String statut;

    private UUID ecriturecgRef;

    @NotNull
    private BigDecimal montantTotal;

    private UUID natureChargeId;
    private String natureChargeLibelle;

    private LocalDateTime validatedAt;
    private String validatedBy;
    private String rejectReason;

    /** Lignes d'imputation par centre */
    private List<LigneImputationDto> lignes;
}

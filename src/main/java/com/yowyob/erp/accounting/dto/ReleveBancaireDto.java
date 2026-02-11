package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for bank statement lines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleveBancaireDto {
    private UUID id;
    private UUID organizationId;
    private UUID compteId;
    private LocalDateTime dateOperation;
    private LocalDateTime dateValeur;
    private String libelle;
    private String reference;
    private BigDecimal montant;
    private String sens;
    private String categorieDetectee;
    private boolean rapproche;
    private LocalDateTime dateRapprochement;
    private UUID detailEcritureId;
}

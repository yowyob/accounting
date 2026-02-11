package com.yowyob.erp.accounting.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.erp.accounting.entity.BrouillardStatut;
import com.yowyob.erp.accounting.entity.BrouillardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrouillardComptableDto {
    private UUID id;
    private BrouillardType type;
    private BrouillardStatut statut;
    private String sourceId;
    private String sourceType;
    private String numeroPiece;
    private LocalDate datePiece;
    private String libelle;
    private BigDecimal montantTotal;
    private String devise;
    private UUID journalId;
    private String journalCode;
    private String journalLibelle;
    private UUID periodeId;
    private String periodeCode;
    private JsonNode dataJson;
    private UUID ecritureId;
    private String ecritureNumero;
    private JsonNode attachmentIds;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}

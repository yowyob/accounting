package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrouillardFeedbackEvent {
    private UUID brouillardId;
    private String sourceId;
    private String sourceType;
    private String statut; // "VALIDE" or "REJETE"
    private String motifRejet;
    private UUID ecritureId; // Seul si validé
}

package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompteAnalytiqueDto {
    private UUID id;
    
    @NotNull(message = "Le code est obligatoire")
    private String code;
    
    @NotNull(message = "Le libellé est obligatoire")
    private String libelle;
    
    private String classe;
    
    private String nature; // CHARGE_DIRECTE, CHARGE_INDIRECTE, PRODUIT
    
    private UUID compteGeneralId;
    
    private String compteGeneralNo;
    private String compteGeneralLibelle;
    
    @Builder.Default
    private Boolean actif = true;

    private LocalDateTime updatedAt;
}

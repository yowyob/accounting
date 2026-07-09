package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AxeAnalytiqueDto {
    private UUID id;
    
    @NotNull(message = "Le code de l'axe est obligatoire")
    private String code;
    
    @NotNull(message = "Le libellé de l'axe est obligatoire")
    private String libelle;
    
    @NotNull(message = "Le type de l'axe est obligatoire")
    private String type; // DEPARTEMENT, PROJET, ACTIVITE, CENTRE_COUT
    
    private String responsable;
    
    private UUID parentId;
    private String typeCentre; // PRINCIPAL, AUXILIAIRE, FICTIF
    private java.math.BigDecimal budgetAnnuel;
    private String uniteOeuvreCode;
    
    @Builder.Default
    private Boolean actif = true;
    
    private List<UUID> compteIds;
    private List<String> compteLibelles;
}

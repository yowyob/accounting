package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CoutProduitDto {
    private UUID id;

    @NotBlank(message = "Le code produit est obligatoire")
    private String produitCode;

    @NotBlank(message = "Le libellé produit est obligatoire")
    private String produitLibelle;

    @NotNull(message = "Le coût d'achat est obligatoire")
    private BigDecimal coutAchat;

    @NotNull(message = "Le coût de production est obligatoire")
    private BigDecimal coutProduction;

    @NotNull(message = "Le coût de revient est obligatoire")
    private BigDecimal coutRevient;

    @NotBlank(message = "La méthode de stock est obligatoire")
    private String methodeStock;

    @NotNull(message = "La période est obligatoire")
    private UUID periodeId;
}

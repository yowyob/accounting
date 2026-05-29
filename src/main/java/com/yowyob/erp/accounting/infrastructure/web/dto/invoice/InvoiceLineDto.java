package com.yowyob.erp.accounting.infrastructure.web.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineDto {
    private UUID idLigne;
    private BigDecimal quantite;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private Boolean isTaxLine;
    private UUID idProduit;
    private String referenceProduit;
    private String nomProduit;
    private BigDecimal prixUnitaire;
    private BigDecimal montantTotal;
    private BigDecimal remisePourcentage;
    private BigDecimal remiseMontant;
    private Integer ordre;
}

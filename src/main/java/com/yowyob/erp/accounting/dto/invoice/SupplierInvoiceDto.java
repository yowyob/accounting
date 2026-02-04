package com.yowyob.erp.accounting.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Supplier Invoice (Purchase) accounting integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInvoiceDto {
    private UUID idFacture;
    private String numeroFacture;
    private LocalDate dateFacturation;
    private LocalDate dateEcheance;
    private LocalDateTime dateSysteme;
    private String etat;
    private String type;
    private UUID idFournisseur;
    private String nomFournisseru; // Matching user's typo in JSON
    private String adresseFournisseur;
    private String emailFournisseur;
    private String telephoneFournisseur;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantTotal;
    private BigDecimal montantRestant;
    private BigDecimal finalAmount;
    private BigDecimal remiseGlobalePourcentage;
    private BigDecimal remiseGlobaleMontant;
    private Boolean applyVat;
    private String devise;
    private BigDecimal tauxChange;
    private String modeReglement;
    private String conditionsPaiement;
    private Integer nbreEcheance;
    private String nosRef;
    private String vosRef;
    private String referenceCommande;
    private UUID idGRN;
    private String numeroGRN;
    private List<InvoiceLineDto> lignesFacture;
}

package com.yowyob.erp.accounting.infrastructure.web.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Customer Invoice (Sale) accounting integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInvoiceDto {
    private String idFacture;
    private String numeroFacture;
    private LocalDateTime dateFacturation;
    private LocalDateTime dateEcheance;
    private LocalDateTime dateSysteme;
    private String type;
    private String etat;
    private String idClient;
    private String nomClient;
    private String adresseClient;
    private String emailClient;
    private String telephoneClient;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantTotal;
    private BigDecimal montantRestant;
    private BigDecimal finalAmount;
    private Boolean applyVat;
    private String devise;
    private BigDecimal tauxChange;
    private String modeReglement;
    private String conditionsPaiement;
    private Integer nbreEcheance;
    private String nosRef;
    private String vosRef;
    private String referenceCommande;
    private String idDevisOrigine;
    private List<InvoiceLineDto> lignesFacture;
    private String notes;
    private String pdfPath;
    private List<UUID> attachmentIds;
    private Boolean envoyeParEmail;
    private LocalDateTime dateEnvoiEmail;
    private UUID referalClientId;
    private UUID organizationId;
    private BigDecimal remiseGlobalePourcentage;
    private BigDecimal remiseGlobaleMontant;
    private UUID createdBy;
    private String createdByUsername;
    private UUID validatedBy;
    private String validatedByUsername;
    private LocalDateTime validatedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

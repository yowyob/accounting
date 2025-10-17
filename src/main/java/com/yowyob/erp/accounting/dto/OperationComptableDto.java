package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationComptableDto {

    private UUID id;

    @NotBlank(message = "Le type d'opération ne peut pas être vide")
    @Size(max = 50, message = "Le type d'opération ne doit pas dépasser 50 caractères")
    @Pattern(regexp = "^(ACHAT|VENTE|SALAIRE|PAIEMENT|DIVERS)$", message = "Type d'opération doit être ACHAT, VENTE, SALAIRE, PAIEMENT ou DIVERS")
    private String typeOperation;

    @NotBlank(message = "Le mode de règlement ne peut pas être vide")
    @Size(max = 50, message = "Le mode de règlement ne doit pas dépasser 50 caractères")
    @Pattern(regexp = "^(ESPECE|CHEQUE|VIREMENT|MOBILE)$", message = "Mode de règlement doit être ESPECE, CHEQUE, VIREMENT ou MOBILE")
    private String modeReglement;

    @NotBlank(message = "Le compte principal ne peut pas être vide")
    @Size(max = 20, message = "Le compte principal ne doit pas dépasser 20 caractères")
    private String comptePrincipal; 

    @NotNull(message = "Le statut compte statique ne peut pas être nul")
    private Boolean estCompteStatique;

    @NotBlank(message = "Le sens principal ne peut pas être vide")
    @Pattern(regexp = "DEBIT|CREDIT", message = "Le sens principal doit être DEBIT ou CREDIT")
    private String sensPrincipal;

    @NotNull(message = "L'identifiant du journal comptable ne peut pas être nul")
    private UUID journalComptableId;

    @NotBlank(message = "Le type de montant ne peut pas être vide")
    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Le type de montant doit être HT, TTC, TVA ou PAU")
    private String typeMontant;

    @PositiveOrZero(message = "Le plafond client doit être positif ou zéro")
    private  BigDecimal plafondClient;

    @NotNull(message = "Le statut actif ne peut pas être nul")
    private Boolean actif;

    @Size(max = 255, message = "Les notes ne doivent pas dépasser 255 caractères")
    private String notes;

    private List<ContrepartieDto> contreparties;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
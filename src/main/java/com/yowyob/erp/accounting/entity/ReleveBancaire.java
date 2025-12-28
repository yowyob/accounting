package com.yowyob.erp.accounting.entity;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "releve_bancaire")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleveBancaire {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "releve_bancaire_id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @Column(name = "date_operation")
    private LocalDate dateOperation;

    @Column(name = "date_valeur")
    private LocalDate dateValeur;
    private String libelle;
    private String reference;           // N° chèque, virement, etc.
    private BigDecimal montant;         // positif = crédit, négatif = débit
    private String sens;                // "CREDIT" ou "DEBIT"
    @Column(name = "categorie")
    private String categorieDetectee;   // "VIREMENT", "CHEQUE", "FRAIS", etc.

    
}
package com.yowyob.erp.accounting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.yowyob.erp.common.enums.Sens;

/**
 * Entity representing an accounting entry detail.
 * Contains debit or credit amounts for a specific account within an entry.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "details_ecritures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailEcriture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "ecriture_id", nullable = false)
    private EcritureComptable ecriture;

    @ManyToOne
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(nullable = false, length = 255)
    private String libelle;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sens sens;

    @Column(name = "montant_debit", precision = 18, scale = 2)
    private BigDecimal montant_debit;

    @Column(name = "montant_credit", precision = 18, scale = 2)
    private BigDecimal montant_credit;

    @Column(name = "lettree")
    @Builder.Default
    private Boolean lettree = false;

    @Column(name = "date_lettrage")
    private LocalDateTime date_lettrage;

    @Column(name = "pointee")
    @Builder.Default
    private Boolean pointee = false;

    @Column(name = "reference_bancaire", length = 100)
    private String reference_bancaire;

    @Column(name = "date_ecriture")
    private LocalDateTime date_ecriture;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;

    /** Creator user */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String created_by;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

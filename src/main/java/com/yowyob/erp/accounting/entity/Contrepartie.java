package com.yowyob.erp.accounting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Counterparty for an accounting operation (Phase 2 of configuration).
 * Models the accounts to be moved for an accounting operation.
 * Joins with JournalComptable and OperationComptable for relational integrity.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@Entity
@Table(name = "contreparties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrepartie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_comptable_id", nullable = false)
    private OperationComptable operation_comptable;

    @Column(length = 20, nullable = false)
    private String compte;

    @Builder.Default
    @Column(name = "est_compte_tiers", nullable = false)
    private Boolean est_compte_tiers = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Direction must be DEBIT or CREDIT")
    @Column(length = 10, nullable = false)
    private String sens;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column(name = "type_montant", length = 10, nullable = false)
    private String type_montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_comptable_id", nullable = false)
    private JournalComptable journal_comptable;

    @Column(length = 255)
    private String notes;

    /** Created at timestamp */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;

    /** Last updated at timestamp */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;

    /** Creator user */
    @Size(max = 255)
    @Column(name = "created_by", length = 255, updatable = false)
    private String created_by;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    /**
     * Initializes timestamps before persistence.
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.created_at = now;
        this.updated_at = now;
    }

    /**
     * Updates the timestamp before update.
     */
    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

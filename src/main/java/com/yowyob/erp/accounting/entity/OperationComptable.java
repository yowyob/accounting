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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.yowyob.erp.config.tenant.TenantContext;

/**
 * Parametrable accounting operation (Phase 1 of OHADA configuration).
 * Joined with Tenant and JournalComptable for relational integrity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "operation_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "operation_id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "type_operation", length = 50, nullable = false)
    private String type_operation;

    @NotBlank
    @Column(name = "mode_reglement", length = 50, nullable = false)
    private String mode_reglement;

    @NotBlank
    @Column(name = "compte_principal", length = 20, nullable = false)
    private String compte_principal;

    @Builder.Default
    @Column(name = "est_compte_statique", nullable = false)
    private Boolean est_compte_statique = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Sens principal must be DEBIT or CREDIT")
    @Column(name = "sens_principal", length = 10)
    private String sens_principal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_comptable_id")
    private JournalComptable journal_comptable;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column(name = "type_montant", length = 10)
    private String type_montant;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "plafond_client")
    private BigDecimal plafond_client = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

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
    @Column(name = "created_by", length = 255)
    private String created_by;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    /**
     * Initializes timestamps and creator before persistence.
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.created_at = now;
        this.updated_at = now;
        this.created_by = TenantContext.getCurrentUser() != null ? TenantContext.getCurrentUser() : "system";
    }

    /**
     * Updates the timestamp and modifier before update.
     */
    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
        this.updated_by = TenantContext.getCurrentUser() != null ? TenantContext.getCurrentUser() : "system";
    }
}
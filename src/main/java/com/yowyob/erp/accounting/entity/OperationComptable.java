package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.yowyob.erp.config.tenant.TenantContext;

/**
 * Parametrable accounting operation (Phase 1 of OHADA configuration)
 * Audit integrated directly into the entity without using an interface.
 * Joins with Tenant and JournalComptable for relational integrity.
 *
 * @author ALD
 * @date 12/10/2025 02:27 PM WAT
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
    private String typeOperation;

    @NotBlank
    @Column(name = "mode_reglement", length = 50, nullable = false)
    private String modeReglement;

    @NotBlank
    @Column(name = "compte_principal", length = 20, nullable = false)
    private String comptePrincipal;

    @Builder.Default
    @Column(name = "est_compte_statique", nullable = false)
    private Boolean estCompteStatique = false;

    @Pattern(regexp = "DEBIT|CREDIT", message = "Sens principal must be DEBIT or CREDIT")
    @Column(name = "sens_principal", length = 10)
    private String sensPrincipal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_comptable_id")
    private JournalComptable journalComptable;

    @Pattern(regexp = "HT|TTC|TVA|PAU", message = "Type montant must be HT, TTC, TVA, or PAU")
    @Column(name = "type_montant", length = 10)
    private String typeMontant;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "plafond_client")
    private BigDecimal plafondClient = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    @Column(length = 255)
    private String notes;

    /** Audit dates */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Creator user */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = TenantContext.getCurrentUser() != null ? TenantContext.getCurrentUser() : "system";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = TenantContext.getCurrentUser() != null ? TenantContext.getCurrentUser() : "system";
    }
}
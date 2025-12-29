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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a tax declaration (VAT, IS, etc.) within the accounting system.
 * Includes embedded audit information for tracking changes.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@Entity
@Table(name = "declaration_fiscale")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationFiscale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "declaration_id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(name = "type_declaration", length = 50, nullable = false)
    private String type_declaration;

    @NotNull
    @Column(name = "periode_debut", nullable = false)
    private LocalDate periode_debut;

    @NotNull
    @Column(name = "periode_fin", nullable = false)
    private LocalDate periode_fin;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "montant_total", nullable = false)
    private Double montant_total = 0.0;

    @Column(name = "date_generation")
    private LocalDate date_generation;

    @Pattern(regexp = "DRAFT|SUBMITTED|VALIDATED", message = "Statut must be DRAFT, SUBMITTED, or VALIDATED")
    @Column(length = 50)
    private String statut;

    @Column(name = "numero_declaration", length = 100)
    private String numero_declaration;

    @Column(name = "donnees_declaration", columnDefinition = "TEXT")
    private String donnees_declaration;

    @Column(length = 255)
    private String notes;

    /** Created at timestamp */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;

    /** Last updated at timestamp */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;

    /** User who created the record */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String created_by;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.created_at = now;
        this.updated_at = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

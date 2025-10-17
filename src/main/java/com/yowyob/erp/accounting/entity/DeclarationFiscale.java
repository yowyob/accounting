package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Déclaration fiscale : TVA, IS, etc.
 * Audit intégré directement dans l'entité sans utiliser d'interface.
 * 
 * Author: Leonel Delmat AZANGUE
 * Date: 12/10/2025
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
    private String typeDeclaration;

    @NotNull
    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @NotNull
    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(name = "date_generation")
    private LocalDate dateGeneration;

    @Pattern(regexp = "DRAFT|SUBMITTED|VALIDATED", message = "Statut must be DRAFT, SUBMITTED, or VALIDATED")
    @Column(length = 50)
    private String statut;

    @Column(name = "numero_declaration", length = 100)
    private String numeroDeclaration;

    @Column(name = "donnees_declaration", columnDefinition = "TEXT")
    private String donneesDeclaration;

    @Column(length = 255)
    private String notes;

    /** Dates d'audit */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Utilisateur créateur */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String createdBy;

    /** Utilisateur ayant modifié la ressource */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

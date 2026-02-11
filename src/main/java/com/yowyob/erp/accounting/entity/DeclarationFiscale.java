package com.yowyob.erp.accounting.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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
@Table("declaration_fiscale")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeclarationFiscale {

    @Id
    @Column("declaration_id")
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Transient
    private Organization tenant;

    @NotBlank
    @Column("type_declaration")
    private String type_declaration;

    @NotNull
    @Column("periode_debut")
    private LocalDate periode_debut;

    @NotNull
    @Column("periode_fin")
    private LocalDate periode_fin;

    @PositiveOrZero
    @Builder.Default
    @Column("montant_total")
    private Double montant_total = 0.0;

    @Column("date_generation")
    private LocalDate date_generation;

    @Pattern(regexp = "DRAFT|SUBMITTED|VALIDATED", message = "Statut must be DRAFT, SUBMITTED, or VALIDATED")
    @Column("statut")
    private String statut;

    @Column("numero_declaration")
    private String numero_declaration;

    @Column("donnees_declaration")
    private String donnees_declaration;

    @Column("notes")
    private String notes;

    /** Created at timestamp */
    @Column("created_at")
    private LocalDateTime created_at;

    /** Last updated at timestamp */
    @Column("updated_at")
    private LocalDateTime updated_at;

    /** User who created the record */
    @Size(max = 255)
    @Column("created_by")
    private String created_by;

    /** User who last modified the resource */
    @Size(max = 255)
    @Column("updated_by")
    private String updated_by;
}

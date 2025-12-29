package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a tax (Taxe).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "taxes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Taxe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(length = 100, nullable = false)
    private String code; // ex: TVA19, AIB10, IRPP

    @NotBlank
    @Column(length = 200, nullable = false)
    private String libelle; // "TVA 19.25%", "Acompte sur Impôt sur les Bénéfices"

    @NotNull
    @Positive
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal taux; // 19.25, 10.00, etc.

    @Column(name = "compte_collecte", length = 20)
    private String compte_collecte; // ex: "445000"

    @Column(name = "compte_deductible", length = 20)
    private String compte_deductible; // ex: "441000"

    @Column(length = 50)
    private String pays; // "CM", "CI", "SN", "GA"

    @Column(name = "date_debut_validite")
    private LocalDate date_debut_validite;

    @Column(name = "date_fin_validite")
    private LocalDate date_fin_validite;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();
}
package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comptes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "no_compte",nullable = false, unique = true, length = 20)
    private String noCompte;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer classe; // OHADA class (1 to 7)

    @Column(nullable = false, length = 50)
    private String typeCompte; // ACTIF, PASSIF, CHARGE, PRODUIT

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    @Column(nullable = false, length = 50)
    private String createdBy;

    @Column(nullable = false, length = 50)
    private String updatedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSolde(BigDecimal newSolde) {
        this.solde = newSolde;
    }
}

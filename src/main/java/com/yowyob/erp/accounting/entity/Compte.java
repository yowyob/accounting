package com.yowyob.erp.accounting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting account (Compte).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
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

    @Column(name = "no_compte", nullable = false, unique = true, length = 20)
    private String no_compte;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer classe; // OHADA class (1 to 7)

    @Column(name = "type_compte", nullable = false, length = 50)
    private String type_compte; // ACTIF, PASSIF, CHARGE, PRODUIT

    @Builder.Default
    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "created_by", nullable = false, length = 50)
    private String created_by;

    @Column(name = "updated_by", nullable = false, length = 50)
    private String updated_by;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }

    public void updateSolde(BigDecimal new_solde) {
        this.solde = new_solde;
    }
}

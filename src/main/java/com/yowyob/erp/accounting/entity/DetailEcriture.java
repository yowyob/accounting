package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.yowyob.erp.common.enums.Sens;
import java.math.BigDecimal;

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
    private PlanComptable compte;

    @Column(nullable = false, length = 255)
    private String libelle;

    
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sens sens;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantDebit;

    @Column(precision = 18, scale = 2)
    private BigDecimal montantCredit;

    private LocalDateTime dateEcriture;
    private LocalDateTime createdAt;
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
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ecritures_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcritureComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalComptable journal;

    @ManyToOne
    @JoinColumn(name = "periode_id")
    private PeriodeComptable periode;

    @Column(nullable = false, unique = true, length = 100)
    private String numeroEcriture;

    @Column(nullable = false, length = 255)
    private String libelle;

    private String referenceExterne;
    
    private String notes;

    private LocalDate dateEcriture;

    private BigDecimal montantTotalDebit;
    private BigDecimal montantTotalCredit;

    
    @Builder.Default
    private Boolean validee = false;
    private LocalDateTime dateValidation;

    @OneToMany(mappedBy = "ecriture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetailEcriture> details;

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

    /**Utilisateur ayant validé l'ecriture */
    @Size(max = 255)
    @Column(name = "validated_by", length = 255)
    private String validatedBy;
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

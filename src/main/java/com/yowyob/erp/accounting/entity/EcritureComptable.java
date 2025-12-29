package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an accounting entry (Ecriture Comptable).
 * Compliant with OHADA standards and Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
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
    private String numero_ecriture;

    @Column(nullable = false, length = 255)
    private String libelle;

    private String reference_externe;

    private String notes;

    private LocalDate date_ecriture;

    private BigDecimal montant_total_debit;
    private BigDecimal montant_total_credit;

    @Builder.Default
    private Boolean validee = false;
    private LocalDateTime date_validation;

    @OneToMany(mappedBy = "ecriture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetailEcriture> details;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String created_by;

    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    @Size(max = 255)
    @Column(name = "validated_by", length = 255)
    private String validated_by;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

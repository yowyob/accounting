package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Entity representing a Fiscal Year (Exercice Comptable).
 * A fiscal year contains multiple accounting periods.
 *
 * @author ALD
 * @date 30.12.2025
 */
@Entity
@Table(name = "exercices_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciceComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 50)
    private String code; // e.g., "2025"

    @Column(nullable = false, length = 255)
    private String libelle; // e.g., "Exercice 2025"

    @Column(nullable = false)
    private LocalDate date_debut;

    @Column(nullable = false)
    private LocalDate date_fin;

    @Builder.Default
    private Boolean cloture = false;

    @OneToMany(mappedBy = "exercice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PeriodeComptable> periodes;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private String created_by;
    private String updated_by;

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

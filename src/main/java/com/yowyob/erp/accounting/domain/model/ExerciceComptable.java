package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Entity representing a Fiscal Year (Exercice Comptable) for R2DBC.
 */
@Table(name = "exercices_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciceComptable implements SettablePersistable<UUID> {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column("organization_id")
    private UUID organizationId;

    @Column("code")
    private String code;

    @Column("libelle")
    private String libelle;

    @Column("date_debut")
    private LocalDate date_debut;

    @Column("date_fin")
    private LocalDate date_fin;

    @Builder.Default
    @Column("cloture")
    private Boolean cloture = false;

    @Builder.Default
    @Column("statut")
    private ExerciceStatut statut = ExerciceStatut.OUVERT;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Column("created_by")
    private String created_by;

    @Column("updated_by")
    private String updated_by;

    @Transient
    private Organization organization;

    @Transient
    private List<PeriodeComptable> periodes;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}

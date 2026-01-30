package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting period (Periode Comptable) for R2DBC.
 */
@Table(name = "periodes_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodeComptable {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("exercice_id")
    private UUID exerciceId;

    @Column("code")
    private String code;

    @Column("date_debut")
    private LocalDate date_debut;

    @Column("date_fin")
    private LocalDate date_fin;

    @Builder.Default
    @Column("cloturee")
    private Boolean cloturee = false;

    @Column("notes")
    private String notes;

    @Column("date_cloture")
    private LocalDate date_cloture;

    @Column("created_at")
    private LocalDateTime created_at;

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column("created_by")
    private String created_by;

    @Size(max = 255)
    @Column("updated_by")
    private String updated_by;

    @Transient
    private Tenant tenant;

    @Transient
    private ExerciceComptable exercice;
}

package com.yowyob.erp.accounting.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting account template (Plan Comptable Template)
 * for R2DBC.
 */
@Table(name = "plans_comptables_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanComptableTemplate {

    @Id
    private UUID id;

    @Column("classe")
    private Integer classe;

    @Column("numero")
    private String numero;

    @Column("libelle")
    private String libelle;

    @Column("notes")
    private String notes;

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
}

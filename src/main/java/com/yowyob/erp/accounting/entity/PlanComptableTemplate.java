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

import org.springframework.data.domain.Persistable;
import org.springframework.data.annotation.Transient;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yowyob.erp.common.persistence.SettablePersistable;

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
public class PlanComptableTemplate implements SettablePersistable<UUID> {

    @Id
    @Column("id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

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

    @Transient
    @JsonIgnore
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return this.isNew || id == null;
    }

    @Override
    public void setNotNew() {
        this.isNew = false;
    }
}

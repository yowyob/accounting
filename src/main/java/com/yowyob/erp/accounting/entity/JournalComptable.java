package com.yowyob.erp.accounting.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.common.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting journal (Journal Comptable) for R2DBC.
 */
@Table(name = "journaux_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalComptable implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("code_journal")
    private String code_journal;

    @Column("libelle")
    private String libelle;

    @Column("type_journal")
    private String type_journal; // VENTES, ACHATS, TRESORERIE...

    @Column("notes")
    private String notes;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

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
    private Organization tenant;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
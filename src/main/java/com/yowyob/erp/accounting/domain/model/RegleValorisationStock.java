package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(name = "regles_valorisation_stock")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RegleValorisationStock implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("famille_id")
    private String familleId;

    @Column("famille_libelle")
    private String familleLibelle;

    @Column("methode")
    private String methode;

    @Column("date_application")
    private LocalDate dateApplication;

    @Builder.Default
    @Column("actif")
    private Boolean actif = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    private List<HistoriqueValorisationStock> historique;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

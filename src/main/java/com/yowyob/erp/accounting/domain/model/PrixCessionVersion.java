package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "prix_cession_versions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrixCessionVersion implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("prix_cession_id")
    private UUID prixCessionId;

    @Column("prix_unitaire")
    private BigDecimal prixUnitaire;

    @Column("methode")
    private String methode;

    @Column("date_debut")
    private LocalDate dateDebut;

    @Column("date_fin")
    private LocalDate dateFin;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

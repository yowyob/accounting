package com.yowyob.erp.accounting.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "lignes_concordance")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LigneConcordance implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("periode_id")
    private UUID periodeId;

    @Column("type")
    private String type;

    @Column("label")
    private String label;

    @Column("description")
    private String description;

    @Column("signe")
    private String signe;

    @Column("montant")
    private BigDecimal montant;

    @Column("charge_ventilee_id")
    private UUID chargeVentileeId;

    @Builder.Default
    @Column("auto_generee")
    private Boolean autoGeneree = false;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

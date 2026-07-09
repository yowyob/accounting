package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "couts_produits")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CoutProduit implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Column("produit_code")
    private String produitCode;

    @Column("produit_libelle")
    private String produitLibelle;

    @Column("cout_achat")
    private BigDecimal coutAchat;

    @Column("cout_production")
    private BigDecimal coutProduction;

    @Column("cout_revient")
    private BigDecimal coutRevient;

    @Column("methode_stock")
    private String methodeStock;

    @Column("periode_id")
    private UUID periodeId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("created_by")
    private String createdBy;

    @Column("updated_by")
    private String updatedBy;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override @Transient
    public boolean isNew() { return isNew || id == null; }

    @Override
    public void setNotNew() { this.isNew = false; }
}

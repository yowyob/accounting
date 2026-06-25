package com.yowyob.erp.accounting.domain.model;

import com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abonnement d'une organisation aux activités comptables.
 *
 * <p>Chaque organisation peut souscrire indépendamment à la comptabilité générale,
 * à la comptabilité analytique, ou aux deux. La sidebar du frontend n'affiche que
 * les activités actives. Une seule ligne par organisation (contrainte unique).</p>
 */
@Table("accounting_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingSubscription implements SettablePersistable<UUID> {

    @Id
    private UUID id;

    @Column("organization_id")
    private UUID organizationId;

    @Builder.Default
    @Column("generale_active")
    private Boolean generaleActive = true;

    @Builder.Default
    @Column("analytique_active")
    private Boolean analytiqueActive = false;

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

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}

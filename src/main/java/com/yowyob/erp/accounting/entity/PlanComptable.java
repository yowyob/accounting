package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Plan Comptable Général OHADA.
 * Liste des comptes utilisés par une entreprise (tenant).
 */
@Entity
@Table(name = "plan_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanComptable implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_comptable_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotNull
    @Column(name = "classe")
    private Integer classe;

    @NotBlank
    @Size(max = 20)
    @Column(name = "no_compte", nullable = false, unique = true)
    private String noCompte;

    @NotBlank
    @Size(max = 255)
    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Size(max = 255)
    private String notes;

    @NotNull
    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Override
    public UUID getTenantId() { return tenantId; }

    @Override
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
}

package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représente une période comptable (mois, trimestre ou année).
 * Chaque période est liée à un tenant (entreprise).
 */
@Entity
@Table(name = "periode_comptable")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodeComptable implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "periode_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotNull
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @NotNull
    @Column(name = "cloturee", nullable = false)
    private Boolean cloturee = false;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Column(length = 255)
    private String notes;

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

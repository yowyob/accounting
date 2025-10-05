package com.yowyob.erp.accounting.entity;

import com.yowyob.erp.common.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un client ou une entreprise utilisant le système ERP.
 * Multi-tenant isolation.
 */
@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_internal_id")
    private Long id;

    @NotNull
    @Column(name = "tenant_id", unique = true, nullable = false)
    private UUID tenantId;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "legal_name")
    private String legalName;

    @Size(max = 100)
    @Column(name = "registration_number")
    private String registrationNumber;

    @Size(max = 100)
    @Column(name = "tax_id")
    private String taxId;

    @Size(max = 255)
    private String address;

    @Pattern(regexp = "\\+?[0-9\\-\\s]{10,20}")
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotBlank
    @Size(max = 100)
    @Column(name = "accounting_code")
    private String accountingCode;

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

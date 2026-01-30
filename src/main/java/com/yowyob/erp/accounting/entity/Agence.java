package com.yowyob.erp.accounting.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an Agency (Agence).
 * An Agency belongs to a Tenant.
 *
 * @author ALD
 * @date 30.12.2025
 */
@Table("agences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agence {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    private String name;

    private String code;

    private String address;
    private String city;
    private String country;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}

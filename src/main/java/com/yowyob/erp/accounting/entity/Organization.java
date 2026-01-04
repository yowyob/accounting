package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an Organization.
 * An organization can have multiple Tenants.
 *
 * @author ALD
 * @date 30.12.2025
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    @Column(name = "tax_id")
    private String tax_id;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

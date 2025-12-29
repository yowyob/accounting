package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a device (POS, mobile, tablet) used by a tenant.
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "device_id", length = 100, unique = true, nullable = false)
    private String device_id; // e.g., "POS-001", "MOBILE-AND-123"

    @Column(length = 200)
    private String nom;

    @Column(length = 100)
    private String type; // "POS", "MOBILE", "TABLET"

    @Column(name = "marque_modele", length = 100)
    private String marque_modele; // "Ingenico Move/5000", "Samsung Galaxy Tab"

    @Column(name = "serial_number", length = 100)
    private String serial_number;

    @Column(name = "ip_address", length = 45)
    private String ip_address;

    @Column(name = "last_seen")
    private LocalDateTime last_seen;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updated_at = LocalDateTime.now();

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
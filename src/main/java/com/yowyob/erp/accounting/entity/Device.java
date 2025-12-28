package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "device_id", length = 100, unique = true, nullable = false)
    private String deviceId; // ex: "POS-001", "MOBILE-AND-123"

    @Column(length = 200)
    private String nom;

    @Column(length = 100)
    private String type; // "POS", "MOBILE", "TABLETTE"

    @Column(length = 100)
    private String marqueModele; // "Ingenico Move/5000", "Samsung Galaxy Tab"

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
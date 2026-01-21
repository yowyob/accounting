package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a Currency (Devise).
 * Currencies are global to the system.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "devises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Devise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 10)
    private String code; // ISO code: EUR, USD, XAF

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 10)
    private String symbole;

    @Builder.Default
    @Column(nullable = false)
    private boolean est_nationale = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();
}

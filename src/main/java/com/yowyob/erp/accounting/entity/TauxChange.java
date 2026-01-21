package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an Exchange Rate (Taux de Change).
 * Rates are tenant-specific and define the conversion from source to target
 * currency.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "taux_change")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TauxChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "devise_source_id", nullable = false)
    private Devise devise_source;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "devise_cible_id", nullable = false)
    private Devise devise_cible;

    @NotNull
    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal taux;

    @NotNull
    @Column(name = "date_effet", nullable = false)
    private LocalDateTime date_effet;

    private String notes;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime created_at = LocalDateTime.now();
}

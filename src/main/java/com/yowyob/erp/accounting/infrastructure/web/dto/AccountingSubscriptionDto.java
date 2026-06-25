package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abonnement d'une organisation aux activités comptables (Générale / Analytique).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingSubscriptionDto {
    private UUID id;
    private boolean generale;
    private boolean analytique;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

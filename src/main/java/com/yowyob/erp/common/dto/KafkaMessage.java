package com.yowyob.erp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 📦 KafkaMessage
 * 
 * Objet générique représentant un événement transmis via Kafka.
 * Utilisé pour encapsuler les échanges inter-modulaires dans l'ERP.
 *
 * Structure standardisée selon la Charte Yowyob :
 * - Identité du tenant
 * - Type d’événement métier
 * - Payload métier sérialisé (objet)
 * - Métadonnées de traçabilité (timestamp, source, correlationId)
 *
 * Exemple :
 * new KafkaMessage(
 *      ecritureComptable,
 *      "tenant-550e8400",
 *      "ACCOUNTING_ENTRY_VALIDATED",
 *      LocalDateTime.now(),
 *      UUID.randomUUID().toString(),
 *      "accounting-service"
 * )
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Donnée métier (EcritureComptable, Facture, Transaction, etc.) */
    private Object payload;

    /** Identifiant unique du tenant (multi-tenant ERP) */
    private String tenantId;

    /** Type d’événement (INVOICE_CREATED, ACCOUNTING_ENTRY_VALIDATED, etc.) */
    private String eventType;

    /** Horodatage de l’événement */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Identifiant de corrélation pour tracer un flux complet (ex: de facture à écriture) */
    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    /** Nom du microservice ou module émetteur */
    @Builder.Default
    private String source = "erp-backend";
}

package com.yowyob.erp.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generic object representing an event transmitted via Kafka.
 * Used to encapsulate inter-module exchanges in the ERP.
 * 
 * Standardized structure according to Yowyob Charter:
 * - Organization identity
 * - Business event type
 * - Serialized business payload (object)
 * - Traceability metadata (timestamp, source, correlationId)
 * 
 * Example:
 * new KafkaMessage(
 * accountingEntry,
 * "organization-550e8400",
 * "ACCOUNTING_ENTRY_VALIDATED",
 * LocalDateTime.now(),
 * UUID.randomUUID().toString(),
 * "accounting-service"
 * )
 * 
 * @author ALD
 * @date 30.09.25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Business data (AccountingEntry, Invoice, Transaction, etc.) */
    private Object payload;

    /** Unique organization identifier (multi-organization ERP) */
    @JsonProperty("organization_id")
    private UUID organizationId;

    /** Event type (INVOICE_CREATED, ACCOUNTING_ENTRY_VALIDATED, etc.) */
    @JsonProperty("event_type")
    private String eventType;

    /** Event timestamp */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Correlation identifier to trace a complete flow (e.g., from invoice to entry)
     */
    @JsonProperty("correlation_id")
    @Builder.Default
    private String correlationId = UUID.randomUUID().toString();

    /** Name of the emitting microservice or module */
    @Builder.Default
    private String source = "erp-backend";
}

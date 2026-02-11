package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterMovementDto {
    private String id;
    private String session_id;
    private String sense; // "entree", "sortie", "transfert"
    private BigDecimal amount;
    private String reason;
    private String recipient_id;
    private String emitter_id;
    private Boolean is_accounted;
    private Boolean event_ticketing_details;
    private String external_reference;
    private LocalDateTime create_on;
    private String create_by;
    private String emitter_accounting_account;
    private String recipient_accounting_account;
    private java.util.List<java.util.UUID> attachmentIds;
    private Boolean is_deleted;
}

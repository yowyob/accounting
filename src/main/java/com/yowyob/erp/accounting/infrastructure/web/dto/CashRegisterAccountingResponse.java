package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashRegisterAccountingResponse {
    private UUID movement_id;
    private String status;
    private UUID ecriture_id;
}

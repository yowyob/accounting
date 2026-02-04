package com.yowyob.erp.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountGenerationRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Type is required (CLIENT, SUPPLIER, EMPLOYEE, CASH, BANK, VAT_COLLECTED, VAT_DEDUCTIBLE, STOCK)")
    private String type;

    private String notes;

    private UUID externalId;
}

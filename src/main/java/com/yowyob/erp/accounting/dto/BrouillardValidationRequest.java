package com.yowyob.erp.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrouillardValidationRequest {
    private String notes;
    private Boolean forceValidation; // Pour forcer même si déséquilibré (pour usage futur)
}

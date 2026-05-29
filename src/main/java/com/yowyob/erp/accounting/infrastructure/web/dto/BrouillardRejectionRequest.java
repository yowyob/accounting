package com.yowyob.erp.accounting.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrouillardRejectionRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}

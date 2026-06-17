package com.yowyob.erp.accounting.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of the accounting onboarding wizard: one {@link StepResult} per component, so the
 * frontend can show, step by step, what was created, what was already present, and what failed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingSetupResponse {

    private UUID organizationId;
    private int year;
    private List<StepResult> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        /** Stable identifier matching the request flag (planComptable, journaux, …). */
        private String key;
        /** Human-readable label (FR) for display. */
        private String label;
        /** CREATED | ALREADY_PRESENT | MISSING | SKIPPED | ERROR */
        private String status;
        /** Free-text detail / count / error message. */
        private String detail;
    }
}

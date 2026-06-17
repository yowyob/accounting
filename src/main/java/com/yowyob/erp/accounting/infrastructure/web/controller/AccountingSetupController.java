package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.AccountingSetupService;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSetupRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSetupResponse;
import com.yowyob.erp.config.auth.AccountingAuthorities;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Onboarding wizard for a new organization's accounting: lets an ADMIN / RESPONSABLE_COMPTABLE
 * initialize the chart of accounts, journals, fiscal year, periods and operation templates from
 * the frontend, instead of relying on an automatic boot-time seed of a single default organization.
 * The organization is resolved from the request context (X-Organization-Id).
 */
@RestController
@RequestMapping("/api/accounting/setup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting Setup", description = "Initialisation guidée du module comptable d'une organisation")
public class AccountingSetupController {

    private final AccountingSetupService setupService;

    @PostMapping
    @Operation(summary = "Initialiser les composants comptables sélectionnés pour l'organisation courante")
    @PreAuthorize(AccountingAuthorities.SUPERVISE)
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSetupResponse>>> runSetup(
            @RequestBody AccountingSetupRequest request) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(orgId -> setupService.runSetup(orgId, request))
                .map(result -> ResponseEntity.ok(
                        ApiResponseWrapper.success(result, "Initialisation comptable terminée.")))
                .onErrorResume(e -> {
                    log.error("Accounting setup failed: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponseWrapper.error(e.getMessage(), (AccountingSetupResponse) null)));
                });
    }

    @GetMapping("/status")
    @Operation(summary = "État d'initialisation des composants comptables de l'organisation courante")
    @PreAuthorize(AccountingAuthorities.READ)
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSetupResponse>>> status(
            @RequestParam(value = "year", required = false) Integer year) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(orgId -> setupService.status(orgId, year))
                .map(result -> ResponseEntity.ok(ApiResponseWrapper.success(result)));
    }
}

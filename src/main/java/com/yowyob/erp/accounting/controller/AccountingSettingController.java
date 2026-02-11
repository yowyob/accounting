package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.AccountingSettingDto;
import com.yowyob.erp.accounting.entity.AccountingSetting;
import com.yowyob.erp.accounting.entity.BrouillardType;
import com.yowyob.erp.accounting.service.AccountingSettingService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting Settings", description = "Endpoints for configuring accounting entry modes")
public class AccountingSettingController {

    private final AccountingSettingService settingService;

    @GetMapping
    @Operation(summary = "Get all accounting settings")
    public Mono<ResponseEntity<Flux<AccountingSettingDto>>> getAllSettings() {
        return ReactiveTenantContext.getTenantId()
                .map(tenantId -> ResponseEntity.ok(
                        settingService.getAllSettings(tenantId)
                                .map(this::mapToDto)
                ));
        // Note: getAllSettings in service currently returns empty. 
        // We should fix that later if listing is required.
    }

    @GetMapping("/{type}")
    @Operation(summary = "Get setting for a specific type and optional journal")
    public Mono<ResponseEntity<AccountingSettingDto>> getSetting(
            @PathVariable BrouillardType type,
            @RequestParam(required = false) UUID journalId) {
        
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenantId -> settingService.getSetting(tenantId, type, journalId))
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Update or create an accounting setting")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSettingDto>>> updateSetting(
            @Valid @RequestBody AccountingSettingDto dto) {
        
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenantId -> settingService.updateSetting(tenantId, dto))
                .map(this::mapToDto)
                .map(result -> ResponseEntity.ok(ApiResponseWrapper.success(result, "Setting updated successfully")));
    }

    private AccountingSettingDto mapToDto(AccountingSetting entity) {
        return AccountingSettingDto.builder()
                .id(entity.getId())
                .objetType(entity.getObjetType())
                .modeSaisie(entity.getModeSaisie())
                .montantSeuil(entity.getMontantSeuil())
                .journalId(entity.getJournalId())
                .actif(entity.getActif())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

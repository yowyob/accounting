package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSettingDto;
import com.yowyob.erp.accounting.domain.model.AccountingSetting;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import com.yowyob.erp.accounting.domain.port.in.AccountingSettingUseCase;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    private final AccountingSettingUseCase settingService;

    @GetMapping
    @Operation(summary = "Get all accounting settings")
    public Mono<ResponseEntity<Flux<AccountingSettingDto>>> getAllSettings() {
        return ReactiveOrganizationContext.getOrganizationId()
                .map(organizationId -> ResponseEntity.ok(
                        settingService.getAllSettings(organizationId)
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
        
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organizationId -> settingService.getSetting(organizationId, type, journalId))
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Update or create an accounting setting")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSettingDto>>> updateSetting(
            @Valid @RequestBody AccountingSettingDto dto) {
        
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organizationId -> settingService.updateSetting(organizationId, dto))
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

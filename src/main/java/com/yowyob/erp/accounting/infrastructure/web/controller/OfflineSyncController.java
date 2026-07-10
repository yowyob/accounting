package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.application.service.OfflineSyncService;
import com.yowyob.erp.accounting.infrastructure.web.dto.SyncPullResponseDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.SyncPushRequestDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.SyncPushResponseDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Sync client offline : pull incrémental (?since=) et push batch CREATE.
 */
@RestController
@RequestMapping("/api/accounting/sync")
@RequiredArgsConstructor
@Tag(name = "Offline Sync", description = "Synchronisation offline client (pull/push)")
@SecurityRequirement(name = "Bearer Authentication")
public class OfflineSyncController {

    private final OfflineSyncService offlineSyncService;

    @GetMapping("/pull")
    @Operation(summary = "Pull incrémental des changements depuis une date")
    public Mono<ResponseEntity<ApiResponseWrapper<SyncPullResponseDto>>> pull(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return offlineSyncService.pull(since)
                .map(data -> ResponseEntity.ok(ApiResponseWrapper.success(data, "Sync pull OK")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }

    @PostMapping("/push")
    @Operation(summary = "Push batch des créations offline (Idempotency-Key par opération)")
    public Mono<ResponseEntity<ApiResponseWrapper<SyncPushResponseDto>>> push(
            @Valid @RequestBody SyncPushRequestDto request) {
        return offlineSyncService.push(request)
                .map(data -> ResponseEntity.ok(ApiResponseWrapper.success(data, "Sync push OK")))
                .contextWrite(ReactiveOrganizationContext.captureFromThreadLocal());
    }
}

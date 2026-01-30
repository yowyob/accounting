package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.service.JournalComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Reactive REST Controller for managing journals.
 */
@RestController
@RequestMapping("/api/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Accounting Journal Management", description = "Endpoints for managing journals in the accounting module")
public class JournalComptableController {

        private final JournalComptableService journal_service;

        @PostMapping
        @Operation(summary = "Create a new journal")
        public Mono<ResponseEntity<ApiResponseWrapper<JournalComptableDto>>> createJournal(
                        @Valid @RequestBody JournalComptableDto dto) {
                return journal_service.createJournalComptable(dto)
                                .map(created -> ResponseEntity.status(HttpStatus.CREATED)
                                                .body(ApiResponseWrapper.success(created,
                                                                "Journal created successfully")))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update an existing journal")
        public Mono<ResponseEntity<ApiResponseWrapper<JournalComptableDto>>> updateJournal(@PathVariable UUID id,
                        @Valid @RequestBody JournalComptableDto dto) {
                return journal_service.updateJournalComptable(id, dto)
                                .map(updated -> ResponseEntity.ok(
                                                ApiResponseWrapper.success(updated, "Journal updated successfully")))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get journal by ID")
        public Mono<ResponseEntity<ApiResponseWrapper<JournalComptableDto>>> getJournal(@PathVariable UUID id) {
                return journal_service.getJournalComptable(id)
                                .map(journal -> ResponseEntity.ok(ApiResponseWrapper.success(journal, "Journal found")))
                                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(ApiResponseWrapper.error("Journal not found", 404)))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        @GetMapping
        @Operation(summary = "List all journals for the current tenant")
        public Mono<ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>>> getAllJournals() {
                return journal_service.getAllJournaux()
                                .map(list -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(list, "Journals list retrieved")))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        @GetMapping("/active")
        @Operation(summary = "List all active journals")
        public Mono<ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>>> getActiveJournals() {
                return journal_service.getActiveJournaux()
                                .map(list -> ResponseEntity
                                                .ok(ApiResponseWrapper.success(list, "Active journals list retrieved")))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a journal")
        public Mono<ResponseEntity<ApiResponseWrapper<Object>>> deleteJournal(@PathVariable UUID id) {
                return journal_service.deleteJournalComptable(id)
                                .then(Mono.fromCallable(
                                                () -> ResponseEntity.ok(ApiResponseWrapper.success(null,
                                                                "Journal deleted successfully"))))
                                .contextWrite(ReactiveTenantContext.captureFromThreadLocal());
        }
}
package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardComptableDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardRejectionRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardValidationRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.invoice.CustomerInvoiceDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.invoice.SupplierInvoiceDto;
import com.yowyob.erp.accounting.domain.model.BrouillardComptable;
import com.yowyob.erp.accounting.domain.model.BrouillardStatut;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import com.yowyob.erp.accounting.domain.port.in.BrouillardComptableUseCase;
import com.yowyob.erp.accounting.application.service.InvoiceAccountingService;
import com.yowyob.erp.accounting.application.service.FactureProcessingService;
import com.yowyob.erp.accounting.application.service.AttachmentService;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/brouillards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Draft Accounting", description = "Endpoints for managing draft accounting entries (brouillards)")
public class BrouillardComptableController {

    private final BrouillardComptableUseCase brouillardService;
    private final InvoiceAccountingService invoiceAccountingService;
    private final FactureProcessingService factureProcessingService;
    private final AttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Get all draft entries")
    public Mono<ResponseEntity<Flux<BrouillardComptableDto>>> getAllBrouillards(
            @RequestParam(required = false) BrouillardStatut statut,
            @RequestParam(required = false) BrouillardType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ReactiveOrganizationContext.getOrganizationId()
                .flatMapMany(organizationId -> {
                    PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
                    if (statut != null) {
                        return brouillardService.getBrouillardsByStatut(organizationId, statut, pageable);
                    } else if (type != null) {
                        return brouillardService.getBrouillardsByType(organizationId, type, pageable);
                    } else {
                        return brouillardService.getAllBrouillards(organizationId, pageable);
                    }
                })
                .map(this::mapToDto)
                .collectList()
                .map(list -> ResponseEntity.ok(Flux.fromIterable(list)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a draft entry by ID")
    public Mono<ResponseEntity<BrouillardComptableDto>> getBrouillardById(@PathVariable UUID id) {
        return brouillardService.getBrouillardById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/upload", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @Operation(summary = "Upload an invoice/receipt for OCR and create a draft entry")
    public Mono<ResponseEntity<ApiResponseWrapper<BrouillardComptableDto>>> uploadDraftFromInvoice(
            @RequestPart("file") Mono<FilePart> fileMono,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "system";

        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organizationId -> fileMono.flatMap(file ->
                // 1. Convert to FilePart and save attachment
                attachmentService.storeFile(file).flatMap(attachmentDto ->
                // 2. Perform OCR Extraction using the saved file reference (as a hack) or
                // directly via FilePart
                factureProcessingService.extractFactureData(file).flatMap(facture -> {

                    // 3. Create JSON array for attachmentIds
                    var attachmentIdsNode = objectMapper.createArrayNode();
                    attachmentIdsNode.add(attachmentDto.getId().toString());

                    return brouillardService.createDraft(
                            organizationId,
                            facture.is_achat() ? BrouillardType.FACTURE_FOURNISSEUR : BrouillardType.FACTURE_CLIENT,
                            facture,
                            UUID.randomUUID().toString(), // SourceID
                            "OCR_UPLOAD", // SourceType
                            facture.getId().toString(), // Numéro Pièce par défaut
                            facture.getDate(), // Date document
                            facture.getLibelle(), // Libellé
                            facture.getMontant_ht().add(facture.getMontant_ht().multiply(facture.getTaux_tva())), // Montant
                                                                                                                  // total
                                                                                                                  // (TTC)
                            "XAF", // Devise par défaut
                            facture.getJournal_comptable_id(), // Journal si détecté
                            facture.getPeriode_comptable_id(), // Période si détectée
                            username,
                            attachmentIdsNode // Attachment ref
                    ).map(savedDraft -> {
                        savedDraft.setStatut(BrouillardStatut.EN_ATTENTE_VALIDATION); // Force status
                        return savedDraft;
                    }).flatMap(brouillardService::saveBrouillard);
                }))))
                .map(this::mapToDto)
                .map(dto -> ResponseEntity
                        .ok(ApiResponseWrapper.success(dto, "Facture importée et brouillard créé avec succès.")))
                .onErrorResume(e -> {
                    log.error("Failed to upload and create draft from OCR", e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(ApiResponseWrapper.error("Erreur OCR: " + e.getMessage(), null)));
                });
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate a draft entry and create accounting entry")
    // @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public Mono<ResponseEntity<ApiResponseWrapper<BrouillardComptableDto>>> validateBrouillard(
            @PathVariable UUID id,
            @RequestBody(required = false) BrouillardValidationRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "system"; // Should retrieve user from
                                                                                        // context

        // This logic is tricky because we need to know HOW to process the JSON based on
        // TYPE.
        // And `InvoiceAccountingService` methods currently take DTOs, not JSON.
        // We need to deserialize inside the lambda passed to `validateBrouillard`.

        return brouillardService.getBrouillardById(id)
                .flatMap(b -> {
                    return brouillardService.validateBrouillard(id, username,
                            request != null ? request : new BrouillardValidationRequest(),
                            jsonNode -> {
                                try {
                                    ObjectMapper lenientMapper = new ObjectMapper()
                                            .findAndRegisterModules()
                                            .configure(
                                                    com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                                    false);

                                    if (b.getType() == BrouillardType.FACTURE_FOURNISSEUR) {
                                        SupplierInvoiceDto dto = lenientMapper.treeToValue(jsonNode,
                                                SupplierInvoiceDto.class);
                                        return invoiceAccountingService.createDirectSupplierInvoiceEntry(dto);
                                    } else if (b.getType() == BrouillardType.FACTURE_CLIENT) {
                                        CustomerInvoiceDto dto = lenientMapper.treeToValue(jsonNode,
                                                CustomerInvoiceDto.class);
                                        return invoiceAccountingService.createDirectCustomerInvoiceEntry(dto);
                                    }
                                    return Mono.error(new IllegalArgumentException(
                                            "Unsupported type for validation: " + b.getType()));
                                } catch (JsonProcessingException e) {
                                    return Mono.error(new IllegalArgumentException("Invalid JSON data in draft", e));
                                }
                            });
                })
                .map(this::mapToDto)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Draft validated successfully")))
                .onErrorResume(e -> {
                    log.error("Failed to validate brouillard: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.badRequest().body(ApiResponseWrapper.error(e.getMessage(), null)));
                });
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a draft entry")
    // @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public Mono<ResponseEntity<ApiResponseWrapper<BrouillardComptableDto>>> rejectBrouillard(
            @PathVariable UUID id,
            @Valid @RequestBody BrouillardRejectionRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "system";
        return brouillardService.rejectBrouillard(id, username, request)
                .map(this::mapToDto)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Draft rejected")))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft entry")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteBrouillard(@PathVariable UUID id) {
        return brouillardService.deleteBrouillard(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    private BrouillardComptableDto mapToDto(BrouillardComptable entity) {
        return BrouillardComptableDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .statut(entity.getStatut())
                .sourceId(entity.getSourceId())
                .sourceType(entity.getSourceType())
                .numeroPiece(entity.getNumeroPiece())
                .datePiece(entity.getDatePiece())
                .libelle(entity.getLibelle())
                .montantTotal(entity.getMontantTotal())
                .devise(entity.getDevise())
                .journalId(entity.getJournalId())
                .periodeId(entity.getPeriodeId())
                .dataJson(entity.getDataJson())
                .ecritureId(entity.getEcritureId())
                .attachmentIds(entity.getAttachmentIds())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .validatedBy(entity.getValidatedBy())
                .validatedAt(entity.getValidatedAt())
                .rejectedBy(entity.getRejectedBy())
                .rejectedAt(entity.getRejectedAt())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }
}

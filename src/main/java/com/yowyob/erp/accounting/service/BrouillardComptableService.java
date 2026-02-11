package com.yowyob.erp.accounting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.dto.BrouillardRejectionRequest;
import com.yowyob.erp.accounting.dto.BrouillardValidationRequest;
import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.entity.BrouillardComptable;
import com.yowyob.erp.accounting.entity.BrouillardStatut;
import com.yowyob.erp.accounting.entity.BrouillardType;
import com.yowyob.erp.accounting.repository.BrouillardComptableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrouillardComptableService {

    private final BrouillardComptableRepository repository;
    private final AccountingSettingService settingService;
    private final BrouillardNotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final EcritureComptableService ecritureService;

    // Services methods for processing specific objects will be injected or handled via callbacks/functions
    // due to circular dependencies potential, we might need a better strategy or use lazy injection
    // For now, validation logic will need to know how to process the JSON back to an accounting entry.
    
    // We can use a registry or strategy pattern here. 
    // Or simply, pass the processing function during validation? No, validation is async API call.
    // We likely need references to specific services (InvoiceAccountingService etc) but avoiding cycles.
    // Let's defer strict typing of validation for a moment and focus on the structure.


    /**
     * Process an accounting object (Invoice, Stock Movement, etc.).
     * Decides whether to create a draft or a direct accounting entry based on settings.
     *
     * @param info DTO containing Info for decision (type, amount, journal, etc.)
     * @param dto The full object DTO
     * @return Mono<BrouillardComptable> if draft is created, or Mono.empty() if direct entry (handled by caller)
     * 
     * Refactored: simpler approach.
     * We return a Mono<Object> which is either the created EcritureComptableDto (if auto) or BrouillardComptableDto (if draft).
     * But mixing return types is messy.
     * 
     * Better: 
     * Methods in specific services call this service.
     * "shouldCreateDraft(type, amount...)" -> if true, they call "createDraft(dto...)"
     * if false, they proceed with their normal logic.
     * 
     * OR: "processAndGetResult" which returns a wrapper.
     */

    public Mono<Boolean> shouldCreateDraft(UUID organizationId, BrouillardType type, BigDecimal amount, UUID journalId) {
        return settingService.shouldUseBrouillard(organizationId, type, amount, journalId);
    }
    
    public Mono<BrouillardComptable> createDraft(UUID organizationId, BrouillardType type, Object sourceDto, 
                                                 String sourceId, String sourceType, 
                                                 String numeroPiece, LocalDate datePiece, String libelle,
                                                 BigDecimal montantTotal, String devise,
                                                 UUID journalId, UUID periodeId, String user,
                                                 JsonNode attachmentIds) {
        
        JsonNode jsonNode = objectMapper.valueToTree(sourceDto);

        BrouillardComptable draft = BrouillardComptable.builder()
                .organizationId(organizationId)
                .type(type)
                .statut(BrouillardStatut.BROUILLON)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .numeroPiece(numeroPiece)
                .datePiece(datePiece)
                .libelle(libelle)
                .montantTotal(montantTotal)
                .devise(devise)
                .journalId(journalId)
                .periodeId(periodeId)
                .dataJson(jsonNode)
                .attachmentIds(attachmentIds)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return repository.save(draft)
                .flatMap(savedDraft -> notificationService.notifyNewBrouillard(savedDraft).thenReturn(savedDraft));
    }

    public Flux<BrouillardComptable> getAllBrouillards(UUID organizationId, Pageable pageable) {
        return repository.findAllByOrganizationId(organizationId, pageable);
    }
    
    public Flux<BrouillardComptable> getBrouillardsByStatut(UUID organizationId, BrouillardStatut statut, Pageable pageable) {
        return repository.findAllByOrganizationIdAndStatut(organizationId, statut, pageable);
    }
    
    public Flux<BrouillardComptable> getBrouillardsByType(UUID organizationId, BrouillardType type, Pageable pageable) {
        return repository.findAllByOrganizationIdAndType(organizationId, type, pageable);
    }

    public Mono<BrouillardComptable> getBrouillardById(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public Mono<BrouillardComptable> rejectBrouillard(UUID id, String user, BrouillardRejectionRequest request) {
        return repository.findById(id)
                .filter(b -> b.getStatut() == BrouillardStatut.BROUILLON || b.getStatut() == BrouillardStatut.EN_ATTENTE_VALIDATION)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Brouillard not found or not in correct status")))
                .flatMap(b -> {
                    b.setStatut(BrouillardStatut.REJETE);
                    b.setRejectedBy(user);
                    b.setRejectedAt(LocalDateTime.now());
                    b.setRejectionReason(request.getReason());
                    b.setUpdatedAt(LocalDateTime.now());
                    b.setNotNew();
                    return repository.save(b);
                });
    }

    public Mono<Void> deleteBrouillard(UUID id) {
        return repository.findById(id)
                .filter(b -> b.getStatut() == BrouillardStatut.BROUILLON || b.getStatut() == BrouillardStatut.REJETE)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cannot delete validated or waiting draft")))
                .flatMap(repository::delete);
    }
    
    // Validation needs to inject logic to create actual accounting entries
    // We will handle this by returning the stored DTO and Type, letting the controller delegate to the right service?
    // Or we provide a callback mechanism. 
    // Let's implement a generic validate method that takes a function to generate the entry.
    
    @Transactional
    public Mono<BrouillardComptable> validateBrouillard(UUID id, String user, BrouillardValidationRequest request, 
                                                        Function<JsonNode, Mono<EcritureComptableDto>> entryGenerator) {
        return repository.findById(id)
                .filter(b -> b.getStatut() != BrouillardStatut.VALIDE)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Draft not found or already validated")))
                .flatMap(b -> {
                    return entryGenerator.apply(b.getDataJson())
                            .flatMap(ecritureDto -> {
                                // Transfer attachments from draft to new entry if not already set by generator
                                if (ecritureDto.getAttachment_ids() == null) {
                                    ecritureDto.setAttachment_ids(b.getAttachmentIds());
                                }
                                
                                // Also allow adding/updating notes from validation request
                                if (request.getNotes() != null) {
                                    ecritureDto.setNotes(request.getNotes());
                                    b.setNotes(request.getNotes());
                                }

                                b.setEcritureId(ecritureDto.getId());
                                b.setStatut(BrouillardStatut.VALIDE);
                                b.setValidatedBy(user);
                                b.setValidatedAt(LocalDateTime.now());
                                b.setUpdatedAt(LocalDateTime.now());
                                b.setNotNew();
                                return repository.save(b);
                            });
                });
    }
}

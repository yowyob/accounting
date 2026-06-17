package com.yowyob.erp.accounting.domain.port.in;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;

import com.yowyob.erp.accounting.domain.model.BrouillardComptable;
import com.yowyob.erp.accounting.domain.model.BrouillardStatut;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardRejectionRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardValidationRequest;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the BrouillardComptable operations.
 */
public interface BrouillardComptableUseCase {
    Mono<Boolean> shouldCreateDraft(UUID organizationId, BrouillardType type, BigDecimal amount, UUID journalId);
    Mono<BrouillardComptable> createDraft(UUID organizationId, BrouillardType type, Object sourceDto, String sourceId, String sourceType, String numeroPiece, LocalDate datePiece, String libelle, BigDecimal montantTotal, String devise, UUID journalId, UUID periodeId, String user, JsonNode attachmentIds);
    Mono<BrouillardComptable> saveBrouillard(BrouillardComptable brouillard);
    Mono<BrouillardComptable> updateDraft(UUID id, com.yowyob.erp.accounting.infrastructure.web.dto.BrouillardComptableDto update);
    Flux<BrouillardComptable> getAllBrouillards(UUID organizationId, Pageable pageable);
    Flux<BrouillardComptable> getBrouillardsByStatut(UUID organizationId, BrouillardStatut statut, Pageable pageable);
    Flux<BrouillardComptable> getBrouillardsByType(UUID organizationId, BrouillardType type, Pageable pageable);
    Mono<BrouillardComptable> getBrouillardById(UUID id);
    Mono<BrouillardComptable> rejectBrouillard(UUID id, String user, BrouillardRejectionRequest request);
    Mono<Void> deleteBrouillard(UUID id);
    Mono<BrouillardComptable> validateBrouillard(UUID id, String user, BrouillardValidationRequest request, Function<JsonNode, Mono<EcritureComptableDto>> entryGenerator);
}

package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.EcritureComptable;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.JournalComptable;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalComptableDto;
import com.yowyob.erp.shared.domain.constants.AppConstants;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the JournalComptable operations.
 */
public interface JournalComptableUseCase {
    Mono<JournalComptableDto> createJournalComptable(JournalComptableDto dto);
    Mono<JournalComptableDto> getJournalComptable(UUID journal_id);
    Mono<java.util.List<JournalComptableDto>> getAllJournaux();
    Mono<java.util.List<JournalComptableDto>> getActiveJournaux();
    Mono<JournalComptableDto> updateJournalComptable(UUID id, JournalComptableDto dto);
    Mono<Void> deleteJournalComptable(UUID id);
    Mono<List<com.yowyob.erp.accounting.infrastructure.web.dto.CompteDto>> getComptesByJournal(UUID journalId);
}

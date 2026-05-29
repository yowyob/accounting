package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.Compte;
import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.domain.model.EcritureComptable;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.OperationComptable;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.domain.model.Transaction;
import com.yowyob.erp.accounting.infrastructure.web.dto.DetailEcritureDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.shared.domain.enums.Sens;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import com.yowyob.erp.shared.domain.model.ComptableObject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the DetailEcriture operations.
 */
public interface DetailEcritureUseCase {
    Mono<DetailEcriture> createDetailEcriture(DetailEcriture detail, Organization organization, EcritureComptable ecriture);
    Mono<Void> generateDetailsFromOperation(EcritureComptable ecriture, OperationComptable operation, Transaction transaction);
    Mono<Void> generateDetailsFromComptableObject(EcritureComptable ecriture, ComptableObject object);
    Mono<DetailEcriture> getDetailEcriture(UUID id, Organization organization);
    Flux<DetailEcriture> getAllDetailsEcriture(Organization organization);
    Flux<DetailEcriture> getDetailsByEcriture(Organization organization, EcritureComptable ecriture);
    Mono<DetailEcriture> updateDetailEcriture(UUID id, DetailEcriture updated_detail, Organization organization, EcritureComptable ecriture);
    Mono<Void> deleteDetailEcriture(UUID id, Organization organization, EcritureComptable ecriture);
}

package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.AccountingSettingRepositoryPort;

import com.yowyob.erp.accounting.domain.model.AccountingSetting;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AccountingSettingRepository extends R2dbcRepository<AccountingSetting, UUID>, AccountingSettingRepositoryPort {
    
    Mono<AccountingSetting> findByOrganizationIdAndObjetTypeAndJournalId(UUID organizationId, BrouillardType objetType, UUID journalId);
    
    Mono<AccountingSetting> findByOrganizationIdAndObjetTypeAndJournalIdIsNull(UUID organizationId, BrouillardType objetType);

    Flux<AccountingSetting> findAllByOrganizationId(UUID organizationId);
}

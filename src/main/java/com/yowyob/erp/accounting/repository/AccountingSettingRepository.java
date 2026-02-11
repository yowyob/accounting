package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.AccountingSetting;
import com.yowyob.erp.accounting.entity.BrouillardType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface AccountingSettingRepository extends R2dbcRepository<AccountingSetting, UUID> {
    
    Mono<AccountingSetting> findByTenantIdAndObjetTypeAndJournalId(UUID organizationId, BrouillardType objetType, UUID journalId);
    
    Mono<AccountingSetting> findByTenantIdAndObjetTypeAndJournalIdIsNull(UUID organizationId, BrouillardType objetType);

    Flux<AccountingSetting> findAllByTenantId(UUID organizationId);
}

package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.AccountingSetting;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for AccountingSetting persistence operations.
 */
public interface AccountingSettingRepositoryPort {
    Mono<AccountingSetting> findByOrganizationIdAndObjetTypeAndJournalId(UUID organizationId, BrouillardType objetType, UUID journalId);
    Mono<AccountingSetting> findByOrganizationIdAndObjetTypeAndJournalIdIsNull(UUID organizationId, BrouillardType objetType);
    Flux<AccountingSetting> findAllByOrganizationId(UUID organizationId);
}

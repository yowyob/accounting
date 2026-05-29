package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.AccountingSetting;
import com.yowyob.erp.accounting.domain.model.BrouillardType;
import com.yowyob.erp.accounting.domain.model.ModeSaisie;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSettingDto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the AccountingSetting operations.
 */
public interface AccountingSettingUseCase {
    Mono<AccountingSetting> getSetting(UUID organizationId, BrouillardType type, UUID journalId);
    Mono<Boolean> shouldUseBrouillard(UUID organizationId, BrouillardType type, BigDecimal amount, UUID journalId);
    Flux<AccountingSetting> getAllSettings(UUID organizationId);
    Mono<AccountingSetting> updateSetting(UUID organizationId, AccountingSettingDto dto);
}

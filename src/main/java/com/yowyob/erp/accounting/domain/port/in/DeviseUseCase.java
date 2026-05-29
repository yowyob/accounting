package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.Devise;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.web.dto.DeviseDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Devise operations.
 */
public interface DeviseUseCase {
    Mono<DeviseDto> createDevise(DeviseDto dto);
    Mono<DeviseDto> updateDevise(UUID id, DeviseDto dto);
    Mono<DeviseDto> getDevise(UUID id);
    Mono<List<DeviseDto>> getAllDevises();
    Mono<List<DeviseDto>> getActiveDevises();
    Mono<Void> deleteDevise(UUID id);
}

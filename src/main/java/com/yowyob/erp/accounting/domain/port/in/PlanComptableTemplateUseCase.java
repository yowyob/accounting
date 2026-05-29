package com.yowyob.erp.accounting.domain.port.in;

import com.yowyob.erp.accounting.domain.model.PlanComptableTemplate;
import com.yowyob.erp.accounting.infrastructure.web.dto.PlanComptableTemplateDto;
import com.yowyob.erp.shared.application.service.ValidationService;
import java.time.LocalDateTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the PlanComptableTemplate operations.
 */
public interface PlanComptableTemplateUseCase {
    Mono<PlanComptableTemplateDto> createAccount(PlanComptableTemplateDto dto);
    Flux<PlanComptableTemplateDto> getAllAccounts();
}

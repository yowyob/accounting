package com.yowyob.erp.accounting.domain.port.in;
import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the ClotureMensuelle operations.
 */
public interface ClotureMensuelleUseCase {
    Mono<Void> cloturerMoisEtGenererANouveaux(UUID organizationId, int mois, int annee);
    Mono<Map<String, Object>> cloturerPeriode(UUID periode_id, String user);
    Mono<Map<String, Object>> verifierEligibiliteCloture(UUID periode_id);
    Mono<Void> annulerCloture(UUID periode_id, String user);
}

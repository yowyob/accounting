package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.domain.model.ReleveBancaire;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the RapprochementBancaire operations.
 */
public interface RapprochementBancaireUseCase {
    Mono<Void> importerReleve(List<ReleveBancaire> lignes);
    Flux<DetailEcriture> proposerRapprochement(UUID releveId);
    Mono<Void> validerRapprochement(UUID releveId, UUID detailId);
}

package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.LigneImputation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface LigneImputationRepository extends R2dbcRepository<LigneImputation, UUID> {

    @Query("SELECT * FROM lignes_imputation WHERE ecriture_id = :ecritureId")
    Flux<LigneImputation> findByEcritureId(@Param("ecritureId") UUID ecritureId);

    @Query("DELETE FROM lignes_imputation WHERE ecriture_id = :ecritureId")
    Mono<Void> deleteByEcritureId(@Param("ecritureId") UUID ecritureId);

    @Query("SELECT COALESCE(SUM(montant), 0) FROM lignes_imputation li " +
           "JOIN ecritures_analytiques ea ON li.ecriture_id = ea.id " +
           "WHERE li.centre_id = :centreId AND ea.statut = 'VALIDEE' " +
           "AND ea.periode_id = :periodeId AND li.sens = 'DEBIT'")
    Mono<BigDecimal> sumMontantByCentreAndPeriode(@Param("centreId") UUID centreId, @Param("periodeId") UUID periodeId);
}

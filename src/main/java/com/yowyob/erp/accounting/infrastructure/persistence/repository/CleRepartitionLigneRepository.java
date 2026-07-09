package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.CleRepartitionLigne;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface CleRepartitionLigneRepository extends R2dbcRepository<CleRepartitionLigne, UUID> {

    @Query("SELECT * FROM cles_repartition_lignes WHERE cle_id = :cleId")
    Flux<CleRepartitionLigne> findByCleId(@Param("cleId") UUID cleId);

    @Query("DELETE FROM cles_repartition_lignes WHERE cle_id = :cleId")
    Mono<Void> deleteByCleId(@Param("cleId") UUID cleId);
}

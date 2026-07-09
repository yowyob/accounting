package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.VentilationCharge;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface VentilationChargeRepository extends R2dbcRepository<VentilationCharge, UUID> {

    @Query("SELECT * FROM ventilations_charge WHERE charge_ventilee_id = :chargeId")
    Flux<VentilationCharge> findByChargeVentileeId(@Param("chargeId") UUID chargeId);

    @Query("DELETE FROM ventilations_charge WHERE charge_ventilee_id = :chargeId")
    Mono<Void> deleteByChargeVentileeId(@Param("chargeId") UUID chargeId);
}

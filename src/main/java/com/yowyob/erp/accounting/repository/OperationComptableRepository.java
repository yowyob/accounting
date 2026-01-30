package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.OperationComptable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive R2DBC Repository for OperationComptable entity.
 */
@Repository
public interface OperationComptableRepository extends R2dbcRepository<OperationComptable, UUID> {

        @Query("SELECT * FROM operation_comptable WHERE tenant_id = :tenant_id")
        Flux<OperationComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

        @Query("SELECT * FROM operation_comptable WHERE tenant_id = :tenant_id AND operation_id = :id")
        Mono<OperationComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

        @Query("SELECT * FROM operation_comptable WHERE tenant_id = :tenant_id AND compte_principal_id = :compte_principal_id")
        Flux<OperationComptable> findByTenant_IdAndCompte_principal_id(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("compte_principal_id") UUID compte_principal_id);

        @Query("SELECT * FROM operation_comptable WHERE tenant_id = :tenant_id AND type_operation = :type_operation AND mode_reglement = :mode_reglement")
        Mono<OperationComptable> findByTenant_IdAndType_operationAndMode_reglement(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("type_operation") String type_operation,
                        @Param("mode_reglement") String mode_reglement);
}

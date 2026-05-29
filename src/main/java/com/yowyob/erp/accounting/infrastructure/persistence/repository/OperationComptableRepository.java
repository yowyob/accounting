package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.OperationComptableRepositoryPort;

import com.yowyob.erp.accounting.domain.model.OperationComptable;
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
public interface OperationComptableRepository extends R2dbcRepository<OperationComptable, UUID>, OperationComptableRepositoryPort {

        @Query("SELECT * FROM operation_comptable WHERE organization_id = :organization_id")
        Flux<OperationComptable> findByOrganization_Id(@Param("organization_id") UUID organization_id);

        @Query("SELECT * FROM operation_comptable WHERE organization_id = :organization_id AND operation_id = :id")
        Mono<OperationComptable> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

        @Query("SELECT * FROM operation_comptable WHERE organization_id = :organization_id AND compte_principal_id = :compte_principal_id")
        Flux<OperationComptable> findByOrganization_IdAndCompte_principal_id(
                        @Param("organization_id") UUID organization_id,
                        @Param("compte_principal_id") UUID compte_principal_id);

        @Query("SELECT * FROM operation_comptable WHERE organization_id = :organization_id AND type_operation = :type_operation AND mode_reglement = :mode_reglement")
        Mono<OperationComptable> findByOrganization_IdAndType_operationAndMode_reglement(
                        @Param("organization_id") UUID organization_id,
                        @Param("type_operation") String type_operation,
                        @Param("mode_reglement") String mode_reglement);

        @Query("SELECT * FROM operation_comptable WHERE organization_id = :organization_id AND type_operation = :type_operation")
        Flux<OperationComptable> findByOrganizationIdAndTypeOperation(
                        @Param("organization_id") UUID organization_id,
                        @Param("type_operation") String type_operation);
}

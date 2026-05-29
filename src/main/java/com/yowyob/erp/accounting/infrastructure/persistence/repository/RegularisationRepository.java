package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.RegularisationRepositoryPort;

import com.yowyob.erp.accounting.domain.model.RegularisationComptable;
import com.yowyob.erp.accounting.domain.model.StatutRegularisation;
import com.yowyob.erp.accounting.domain.model.TypeRegularisation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface RegularisationRepository extends R2dbcRepository<RegularisationComptable, UUID>, RegularisationRepositoryPort {

    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId ORDER BY date_regularisation DESC")
    Flux<RegularisationComptable> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId AND id = :id")
    Mono<RegularisationComptable> findByOrganizationIdAndId(@Param("orgId") UUID orgId, @Param("id") UUID id);

    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId AND periode_id = :periodeId ORDER BY type_regularisation")
    Flux<RegularisationComptable> findByOrganizationIdAndPeriodeId(@Param("orgId") UUID orgId, @Param("periodeId") UUID periodeId);

    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId AND statut = :statut ORDER BY date_regularisation DESC")
    Flux<RegularisationComptable> findByOrganizationIdAndStatut(@Param("orgId") UUID orgId, @Param("statut") String statut);

    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId AND type_regularisation = :type ORDER BY date_regularisation DESC")
    Flux<RegularisationComptable> findByOrganizationIdAndType(@Param("orgId") UUID orgId, @Param("type") String type);

    /**
     * Trouve toutes les régularisations ACTIVES dont la date d'extourne est <= aujourd'hui.
     * Utilisé par le job d'extourne automatique au début de chaque période.
     */
    @Query("SELECT * FROM regularisations_comptables WHERE organization_id = :orgId AND statut = 'ACTIVE' AND date_extourne <= :today")
    Flux<RegularisationComptable> findDuesForExtourne(@Param("orgId") UUID orgId, @Param("today") LocalDate today);

    @Query("SELECT * FROM regularisations_comptables WHERE statut = 'ACTIVE' AND date_extourne <= :today")
    Flux<RegularisationComptable> findAllDuesForExtourne(@Param("today") LocalDate today);
}

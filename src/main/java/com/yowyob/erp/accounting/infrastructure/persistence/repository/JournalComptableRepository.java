package com.yowyob.erp.accounting.infrastructure.persistence.repository;
import com.yowyob.erp.accounting.domain.port.out.JournalComptableRepositoryPort;

import com.yowyob.erp.accounting.domain.model.JournalComptable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * R2DBC Repository interface for managing JournalComptable entities.
 */
@Repository
public interface JournalComptableRepository extends R2dbcRepository<JournalComptable, UUID>, JournalComptableRepositoryPort {

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id")
    Flux<JournalComptable> findByOrganization_Id(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND actif = true")
    Flux<JournalComptable> findByOrganization_IdAndActifTrue(@Param("organization_id") UUID organization_id);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND id = :id")
    Mono<JournalComptable> findByOrganization_IdAndId(@Param("organization_id") UUID organization_id, @Param("id") UUID id);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND code_journal = :code_journal")
    Mono<JournalComptable> findByOrganization_IdAndCode_journal(@Param("organization_id") UUID organization_id,
            @Param("code_journal") String code_journal);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND type_journal = :type_journal")
    Flux<JournalComptable> findByOrganization_IdAndType_journal(@Param("organization_id") UUID organization_id,
            @Param("type_journal") String type_journal);

    @Query("SELECT COUNT(*) > 0 FROM journaux_comptables WHERE organization_id = :organization_id AND code_journal = :code_journal")
    Mono<Boolean> existsByOrganization_IdAndCode_journal(@Param("organization_id") UUID organization_id,
            @Param("code_journal") String code_journal);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByOrganization_Id(@Param("organization_id") UUID organization_id, Pageable pageable);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND actif = true LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByOrganization_IdAndActifTrue(@Param("organization_id") UUID organization_id, Pageable pageable);

    @Query("SELECT * FROM journaux_comptables WHERE organization_id = :organization_id AND type_journal = :type_journal LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByOrganization_IdAndType_journal(@Param("organization_id") UUID organization_id,
            @Param("type_journal") String type_journal, Pageable pageable);

    @Query("SELECT COUNT(*) > 0 FROM journaux_comptables WHERE organization_id = :organization_id AND code_journal = :code_journal AND id <> :id")
    Mono<Boolean> existsByOrganization_IdAndCode_journalAndIdNot(@Param("organization_id") UUID organization_id,
            @Param("code_journal") String code_journal, @Param("id") UUID id);
}

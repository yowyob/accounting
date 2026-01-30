package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalComptable;
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
public interface JournalComptableRepository extends R2dbcRepository<JournalComptable, UUID> {

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id")
    Flux<JournalComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND actif = true")
    Flux<JournalComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND id = :id")
    Mono<JournalComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND code_journal = :code_journal")
    Mono<JournalComptable> findByTenant_IdAndCode_journal(@Param("tenant_id") UUID tenant_id,
            @Param("code_journal") String code_journal);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND type_journal = :type_journal")
    Flux<JournalComptable> findByTenant_IdAndType_journal(@Param("tenant_id") UUID tenant_id,
            @Param("type_journal") String type_journal);

    @Query("SELECT COUNT(*) > 0 FROM journaux_comptables WHERE tenant_id = :tenant_id AND code_journal = :code_journal")
    Mono<Boolean> existsByTenant_IdAndCode_journal(@Param("tenant_id") UUID tenant_id,
            @Param("code_journal") String code_journal);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id, Pageable pageable);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND actif = true LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id, Pageable pageable);

    @Query("SELECT * FROM journaux_comptables WHERE tenant_id = :tenant_id AND type_journal = :type_journal LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<JournalComptable> findByTenant_IdAndType_journal(@Param("tenant_id") UUID tenant_id,
            @Param("type_journal") String type_journal, Pageable pageable);

    @Query("SELECT COUNT(*) > 0 FROM journaux_comptables WHERE tenant_id = :tenant_id AND code_journal = :code_journal AND id <> :id")
    Mono<Boolean> existsByTenant_IdAndCode_journalAndIdNot(@Param("tenant_id") UUID tenant_id,
            @Param("code_journal") String code_journal, @Param("id") UUID id);
}

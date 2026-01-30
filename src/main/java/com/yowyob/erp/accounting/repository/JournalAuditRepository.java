package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalAudit;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC Repository interface for managing JournalAudit entities.
 */
@Repository
public interface JournalAuditRepository extends R2dbcRepository<JournalAudit, UUID> {

        /** Lists all audits for a tenant, ordered by action date descending */
        @Query("SELECT * FROM journal_audit WHERE tenant_id = :tenant_id ORDER BY date_action DESC")
        Flux<JournalAudit> findByTenant_IdOrderByDate_actionDesc(@Param("tenant_id") UUID tenant_id);

        /** Lists audits for a tenant filtered by user */
        @Query("SELECT * FROM journal_audit WHERE tenant_id = :tenant_id AND utilisateur = :utilisateur")
        Flux<JournalAudit> findByTenant_IdAndUtilisateur(@Param("tenant_id") UUID tenant_id,
                        @Param("utilisateur") String utilisateur);

        /** Lists audits for a tenant filtered by action type */
        @Query("SELECT * FROM journal_audit WHERE tenant_id = :tenant_id AND action = :action")
        Flux<JournalAudit> findByTenant_IdAndAction(@Param("tenant_id") UUID tenant_id, @Param("action") String action);

        /** Lists audits for a tenant within a specific time range */
        @Query("SELECT * FROM journal_audit WHERE tenant_id = :tenant_id AND date_action BETWEEN :start_date AND :end_date")
        Flux<JournalAudit> findByTenant_IdAndDate_actionBetween(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("start_date") LocalDateTime start_date,
                        @Param("end_date") LocalDateTime end_date);

        /** Lists audits for a tenant related to a specific accounting entry */
        @Query("SELECT * FROM journal_audit WHERE tenant_id = :tenant_id AND ecriture_id = :ecriture_comptable_id")
        Flux<JournalAudit> findByTenant_IdAndEcriture_comptable_id(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("ecriture_comptable_id") UUID ecriture_comptable_id);
}

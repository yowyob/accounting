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

        /** Lists all audits for a organization, ordered by action date descending */
        @Query("SELECT * FROM journal_audit WHERE organization_id = :organization_id ORDER BY date_action DESC")
        Flux<JournalAudit> findByOrganization_IdOrderByDate_actionDesc(@Param("organization_id") UUID organization_id);

        /** Lists audits for a organization filtered by user */
        @Query("SELECT * FROM journal_audit WHERE organization_id = :organization_id AND utilisateur = :utilisateur")
        Flux<JournalAudit> findByOrganization_IdAndUtilisateur(@Param("organization_id") UUID organization_id,
                        @Param("utilisateur") String utilisateur);

        /** Lists audits for a organization filtered by action type */
        @Query("SELECT * FROM journal_audit WHERE organization_id = :organization_id AND action = :action")
        Flux<JournalAudit> findByOrganization_IdAndAction(@Param("organization_id") UUID organization_id, @Param("action") String action);

        /** Lists audits for a organization within a specific time range */
        @Query("SELECT * FROM journal_audit WHERE organization_id = :organization_id AND date_action BETWEEN :start_date AND :end_date")
        Flux<JournalAudit> findByOrganization_IdAndDate_actionBetween(
                        @Param("organization_id") UUID organization_id,
                        @Param("start_date") LocalDateTime start_date,
                        @Param("end_date") LocalDateTime end_date);

        /** Lists audits for a organization related to a specific accounting entry */
        @Query("SELECT * FROM journal_audit WHERE organization_id = :organization_id AND ecriture_id = :ecriture_comptable_id")
        Flux<JournalAudit> findByOrganization_IdAndEcriture_comptable_id(
                        @Param("organization_id") UUID organization_id,
                        @Param("ecriture_comptable_id") UUID ecriture_comptable_id);
}

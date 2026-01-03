package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing JournalAudit entities.
 * Triggers tenant-aware lookups for audit trails.
 * * @author ALD
 * 
 * @date 30.09.25
 */
@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, UUID> {

        /** Lists all audits for a tenant, ordered by action date descending */
        @Query("SELECT j FROM JournalAudit j WHERE j.tenant.id = :tenant_id ORDER BY j.date_action DESC")
        List<JournalAudit> findByTenant_IdOrderByDate_actionDesc(@Param("tenant_id") UUID tenant_id);

        /** Lists audits for a tenant filtered by user */
        @Query("SELECT j FROM JournalAudit j WHERE j.tenant.id = :tenant_id AND j.utilisateur = :utilisateur")
        List<JournalAudit> findByTenant_IdAndUtilisateur(@Param("tenant_id") UUID tenant_id,
                        @Param("utilisateur") String utilisateur);

        /** Lists audits for a tenant filtered by action type */
        @Query("SELECT j FROM JournalAudit j WHERE j.tenant.id = :tenant_id AND j.action = :action")
        List<JournalAudit> findByTenant_IdAndAction(@Param("tenant_id") UUID tenant_id, @Param("action") String action);

        /** Lists audits for a tenant within a specific time range */
        @Query("SELECT j FROM JournalAudit j WHERE j.tenant.id = :tenant_id AND j.date_action BETWEEN :start_date AND :end_date")
        List<JournalAudit> findByTenant_IdAndDate_actionBetween(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("start_date") LocalDateTime start_date,
                        @Param("end_date") LocalDateTime end_date);

        /** Lists audits for a tenant related to a specific accounting entry */
        @Query("SELECT j FROM JournalAudit j WHERE j.tenant.id = :tenant_id AND j.ecriture_comptable_id = :ecriture_comptable_id")
        List<JournalAudit> findByTenant_IdAndEcriture_comptable_id(
                        @Param("tenant_id") UUID tenant_id,
                        @Param("ecriture_comptable_id") UUID ecriture_comptable_id);
}

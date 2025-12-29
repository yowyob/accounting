package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing JournalAudit entities.
 * Triggers tenant-aware lookups for audit trails.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, UUID> {

    /** Lists all audits for a tenant, ordered by action date descending */
    List<JournalAudit> findByTenant_IdOrderByDate_actionDesc(UUID tenant_id);

    /** Lists audits for a tenant filtered by user */
    List<JournalAudit> findByTenant_IdAndUtilisateur(UUID tenant_id, String utilisateur);

    /** Lists audits for a tenant filtered by action type */
    List<JournalAudit> findByTenant_IdAndAction(UUID tenant_id, String action);

    /** Lists audits for a tenant within a specific time range */
    List<JournalAudit> findByTenant_IdAndDate_actionBetween(UUID tenant_id, LocalDateTime start_date,
            LocalDateTime end_date);

    /** Lists audits for a tenant related to a specific accounting entry */
    List<JournalAudit> findByTenant_IdAndEcriture_comptable_id(UUID tenant_id, UUID ecriture_comptable_id);
}

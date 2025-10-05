package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long> {

    List<JournalAudit> findByTenantIdOrderByDateActionDesc(UUID tenantId);

    List<JournalAudit> findByTenantIdAndUtilisateur(UUID tenantId, String utilisateur);

    List<JournalAudit> findByTenantIdAndAction(UUID tenantId, String action);

    List<JournalAudit> findByTenantIdAndDateActionBetween(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    List<JournalAudit> findByTenantIdAndEcritureComptableId(UUID tenantId, Long ecritureComptableId);
}

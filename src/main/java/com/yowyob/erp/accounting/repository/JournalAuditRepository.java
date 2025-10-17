package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalAuditRepository extends JpaRepository<JournalAudit, UUID> {

    List<JournalAudit> findByTenant_IdOrderByDateActionDesc(UUID tenantId);

    List<JournalAudit> findByTenant_IdAndUtilisateur(UUID tenantId, String utilisateur);

    List<JournalAudit> findByTenant_IdAndAction(UUID tenantId, String action);

    List<JournalAudit> findByTenant_IdAndDateActionBetween(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    List<JournalAudit> findByTenant_IdAndEcritureComptableId(UUID tenantId, UUID ecritureComptableId);
}

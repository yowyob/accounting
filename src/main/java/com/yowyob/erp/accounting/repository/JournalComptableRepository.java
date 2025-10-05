package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalComptableRepository extends JpaRepository<JournalComptable, Long> {

    List<JournalComptable> findByTenantId(UUID tenantId);

    List<JournalComptable> findByTenantIdAndActifTrue(UUID tenantId);

    Optional<JournalComptable> findByTenantIdAndId(UUID tenantId, Long id);

    Optional<JournalComptable> findByTenantIdAndCodeJournal(UUID tenantId, String codeJournal);

    List<JournalComptable> findByTenantIdAndTypeJournal(UUID tenantId, String typeJournal);

    boolean existsByTenantIdAndCodeJournal(UUID tenantId, String codeJournal);
}

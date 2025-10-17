package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalComptable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing JournalComptable entities.
 * Provides custom queries for tenant-specific operations.
 *
 * @author ALD
 * @date 12/10/2025 06:49 AM WAT
 */
@Repository
public interface JournalComptableRepository extends JpaRepository<JournalComptable, UUID> {

    /**
     * Retrieves all journals for a given tenant.
     *
     * @param tenantId the tenant ID
     * @return a list of JournalComptable entities
     */
    List<JournalComptable> findByTenant_Id(UUID tenantId);

    /**
     * Retrieves all active journals for a given tenant.
     *
     * @param tenantId the tenant ID
     * @return a list of active JournalComptable entities
     */
    List<JournalComptable> findByTenant_IdAndActifTrue(UUID tenantId);

    /**
     * Retrieves a journal by tenant ID and journal ID.
     *
     * @param tenantId the tenant ID
     * @param id the journal ID
     * @return an Optional containing the JournalComptable if found
     */
    Optional<JournalComptable> findByTenant_IdAndId(UUID tenantId, UUID id);

    /**
     * Retrieves a journal by tenant ID and journal code.
     *
     * @param tenantId the tenant ID
     * @param codeJournal the journal code
     * @return an Optional containing the JournalComptable if found
     */
    Optional<JournalComptable> findByTenant_IdAndCodeJournal(UUID tenantId, String codeJournal);

    /**
     * Retrieves all journals of a specific type for a given tenant.
     *
     * @param tenantId the tenant ID
     * @param typeJournal the journal type
     * @return a list of JournalComptable entities
     */
    List<JournalComptable> findByTenant_IdAndTypeJournal(UUID tenantId, String typeJournal);

    /**
     * Checks if a journal with the given tenant ID and code exists.
     *
     * @param tenantId the tenant ID
     * @param codeJournal the journal code
     * @return true if the journal exists, false otherwise
     */
    boolean existsByTenant_IdAndCodeJournal(UUID tenantId, String codeJournal);

    /**
     * Retrieves all journals for a given tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination information
     * @return a page of JournalComptable entities
     */
    Page<JournalComptable> findByTenant_Id(UUID tenantId, Pageable pageable);

    /**
     * Retrieves all active journals for a given tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable the pagination information
     * @return a page of active JournalComptable entities
     */
    Page<JournalComptable> findByTenant_IdAndActifTrue(UUID tenantId, Pageable pageable);

    /**
     * Retrieves all journals of a specific type for a given tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param typeJournal the journal type
     * @param pageable the pagination information
     * @return a page of JournalComptable entities
     */
    Page<JournalComptable> findByTenant_IdAndTypeJournal(UUID tenantId, String typeJournal, Pageable pageable);

    /**
     * Checks if a journal with the given tenant ID and code exists, excluding a specific ID.
     *
     * @param tenantId the tenant ID
     * @param codeJournal the journal code
     * @param id the ID to exclude
     * @return true if the journal exists and is not the excluded ID, false otherwise
     */
    boolean existsByTenant_IdAndCodeJournalAndIdNot(UUID tenantId, String codeJournal, UUID id);
}
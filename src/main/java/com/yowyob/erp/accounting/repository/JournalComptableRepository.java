package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.JournalComptable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing JournalComptable entities.
 * Provides custom queries for tenant-specific operations.
 *
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface JournalComptableRepository extends JpaRepository<JournalComptable, UUID> {

    /**
     * Retrieves all journals for a given tenant.
     *
     * @param tenant_id the tenant ID
     * @return a list of JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id")
    List<JournalComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

    /**
     * Retrieves all active journals for a given tenant.
     *
     * @param tenant_id the tenant ID
     * @return a list of active JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.actif = true")
    List<JournalComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id);

    /**
     * Retrieves a journal by tenant ID and journal ID.
     *
     * @param tenant_id the tenant ID
     * @param id        the journal ID
     * @return an Optional containing the JournalComptable if found
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.id = :id")
    Optional<JournalComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

    /**
     * Retrieves a journal by tenant ID and journal code.
     *
     * @param tenant_id    the tenant ID
     * @param code_journal the journal code
     * @return an Optional containing the JournalComptable if found
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.code_journal = :code_journal")
    Optional<JournalComptable> findByTenant_IdAndCode_journal(@Param("tenant_id") UUID tenant_id, @Param("code_journal") String code_journal);

    /**
     * Retrieves all journals of a specific type for a given tenant.
     *
     * @param tenant_id    the tenant ID
     * @param type_journal the journal type
     * @return a list of JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.type_journal = :type_journal")
    List<JournalComptable> findByTenant_IdAndType_journal(@Param("tenant_id") UUID tenant_id, @Param("type_journal") String type_journal);

    /**
     * Checks if a journal with the given tenant ID and code exists.
     *
     * @param tenant_id    the tenant ID
     * @param code_journal the journal code
     * @return true if the journal exists, false otherwise
     */
    @Query("SELECT COUNT(j) > 0 FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.code_journal = :code_journal")
    boolean existsByTenant_IdAndCode_journal(@Param("tenant_id") UUID tenant_id, @Param("code_journal") String code_journal);

    /**
     * Retrieves all journals for a given tenant with pagination.
     *
     * @param tenant_id the tenant ID
     * @param pageable  the pagination information
     * @return a page of JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id")
    Page<JournalComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id, Pageable pageable);

    /**
     * Retrieves all active journals for a given tenant with pagination.
     *
     * @param tenant_id the tenant ID
     * @param pageable  the pagination information
     * @return a page of active JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.actif = true")
    Page<JournalComptable> findByTenant_IdAndActifTrue(@Param("tenant_id") UUID tenant_id, Pageable pageable);

    /**
     * Retrieves all journals of a specific type for a given tenant with pagination.
     *
     * @param tenant_id    the tenant ID
     * @param type_journal the journal type
     * @param pageable     the pagination information
     * @return a page of JournalComptable entities
     */
    @Query("SELECT j FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.type_journal = :type_journal")
    Page<JournalComptable> findByTenant_IdAndType_journal(@Param("tenant_id") UUID tenant_id, @Param("type_journal") String type_journal, Pageable pageable);

    /**
     * Checks if a journal with the given tenant ID and code exists, excluding a
     * specific ID.
     *
     * @param tenant_id    the tenant ID
     * @param code_journal the journal code
     * @param id           the ID to exclude
     * @return true if the journal exists and is not the excluded ID, false
     * otherwise
     */
    @Query("SELECT COUNT(j) > 0 FROM JournalComptable j WHERE j.tenant.id = :tenant_id AND j.code_journal = :code_journal AND j.id <> :id")
    boolean existsByTenant_IdAndCode_journalAndIdNot(@Param("tenant_id") UUID tenant_id, @Param("code_journal") String code_journal, @Param("id") UUID id);
}

package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.EcritureComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing EcritureComptable entities.
 * Provides custom queries for tenant-specific and date-range operations.
 * * @author ALD
 * @date 30.09.25
 */
@Repository
public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, UUID> {

   /**
    * Retrieves all entries for a given tenant.
    * * @param tenant_id the tenant ID
    * @return a list of entries
    */
   @Query("SELECT e FROM EcritureComptable e WHERE e.tenant.id = :tenant_id")
   List<EcritureComptable> findByTenant_Id(@Param("tenant_id") UUID tenant_id);

   /**
    * Retrieves an entry by tenant ID and ID.
    * * @param tenant_id the tenant ID
    * @param id        the entry ID
    * @return an Optional containing the entry if found
    */
   @Query("SELECT e FROM EcritureComptable e WHERE e.tenant.id = :tenant_id AND e.id = :id")
   Optional<EcritureComptable> findByTenant_IdAndId(@Param("tenant_id") UUID tenant_id, @Param("id") UUID id);

   /**
    * Retrieves all non-validated entries for a given tenant.
    * * @param tenant_id the tenant ID
    * @return a list of non-validated entries
    */
   @Query("SELECT e FROM EcritureComptable e WHERE e.tenant.id = :tenant_id AND e.validee = false")
   List<EcritureComptable> findByTenant_IdAndValideeFalse(@Param("tenant_id") UUID tenant_id);

   /**
    * Retrieves entries within a specific date range for a tenant.
    * * @param tenant_id  the tenant ID
    * @param start_date range start date
    * @param end_date   range end date
    * @return a list of entries
    */
   @Query("SELECT e FROM EcritureComptable e WHERE e.tenant.id = :tenant_id AND e.date_ecriture BETWEEN :start_date AND :end_date")
   List<EcritureComptable> findByTenant_IdAndDate_ecritureBetween(
         @Param("tenant_id") UUID tenant_id, 
         @Param("start_date") LocalDate start_date,
         @Param("end_date") LocalDate end_date);

   /**
    * Retrieves entries for a specific journal for a tenant.
    * * @param tenant_id  the tenant ID
    * @param journal_id the journal ID
    * @return a list of entries
    */
   @Query("SELECT e FROM EcritureComptable e WHERE e.tenant.id = :tenant_id AND e.journal.id = :journal_id")
   List<EcritureComptable> findByTenant_IdAndJournal_Id(
         @Param("tenant_id") UUID tenant_id, 
         @Param("journal_id") UUID journal_id);

   /**
    * Retrieves entries for a specific journal and date range for a tenant.
    * * @param tenant_id  the tenant ID
    * @param journal_id the journal ID
    * @param start_date range start date
    * @param end_date   range end date
    * @return a list of entries
    */
   @Query("""
          SELECT e FROM EcritureComptable e 
          WHERE e.tenant.id = :tenant_id 
          AND e.journal.id = :journal_id 
          AND e.date_ecriture BETWEEN :start_date AND :end_date
          """)
   List<EcritureComptable> findByTenant_IdAndJournal_IdAndDate_ecritureBetween(
         @Param("tenant_id") UUID tenant_id, 
         @Param("journal_id") UUID journal_id, 
         @Param("start_date") LocalDate start_date, 
         @Param("end_date") LocalDate end_date);

   /**
    * Finds non-validated entries within a date range using a custom JPQL query.
    * * @param tenant_id  the tenant ID
    * @param start_date range start date
    * @param end_date   range end date
    * @return a list of non-validated entries
    */
   @Query("""
          SELECT e FROM EcritureComptable e
          WHERE e.tenant.id = :tenant_id
          AND e.date_ecriture BETWEEN :start_date AND :end_date
          AND e.validee = false
          """)
   List<EcritureComptable> findNonValidatedByDateRange(
         @Param("tenant_id") UUID tenant_id,
         @Param("start_date") LocalDate start_date,
         @Param("end_date") LocalDate end_date);
}

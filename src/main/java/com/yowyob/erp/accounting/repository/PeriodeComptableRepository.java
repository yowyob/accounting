package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PeriodeComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing PeriodeComptable entities.
 * Includes tenant-aware queries and range-based period lookups.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface PeriodeComptableRepository extends JpaRepository<PeriodeComptable, UUID> {

       /** Finds a period by tenant and ID */
       Optional<PeriodeComptable> findByTenant_IdAndId(UUID tenant_id, UUID id);

       /** Lists all periods for a tenant, ordered by start date descending */
       @Query("SELECT p FROM PeriodeComptable p WHERE p.tenant.id = :tenant_id ORDER BY p.date_debut DESC")
       List<PeriodeComptable> findByTenant_IdOrderByDate_debutDesc(@Param("tenant_id") UUID tenant_id);

       /** Finds a period by tenant and period code */
       Optional<PeriodeComptable> findByTenant_IdAndCode(UUID tenant_id, String code);

       /**
        * Finds a period for a tenant where the given date falls within the period
        * range
        */
       @Query("""
                     SELECT p FROM PeriodeComptable p
                     WHERE p.tenant.id = :tenant_id
                     AND :date BETWEEN p.date_debut AND p.date_fin
                     """)
       Optional<PeriodeComptable> findByTenant_IdAndDateInRange(UUID tenant_id, LocalDate date);

       /** Lists all open (non-closed) periods for a tenant */
       List<PeriodeComptable> findByTenant_IdAndClotureeFalse(UUID tenant_id);

       /**
        * Lists periods for a tenant that fall within a given start and end date range
        */
       @Query("""
                     SELECT p FROM PeriodeComptable p
                     WHERE p.tenant.id = :tenant_id
                     AND p.date_debut >= :start_date
                     AND p.date_fin <= :end_date
                     """)
       List<PeriodeComptable> findByTenant_IdAndPeriodRange(UUID tenant_id, LocalDate start_date, LocalDate end_date);
}

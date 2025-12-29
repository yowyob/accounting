package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing DetailEcriture entities.
 * Handles granular accounting data and balance calculations.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface DetailEcritureRepository extends JpaRepository<DetailEcriture, UUID> {

  List<DetailEcriture> findByTenant_Id(UUID tenant_id);

  List<DetailEcriture> findByTenant_IdAndEcriture_Id(UUID tenant_id, UUID ecriture_id);

  List<DetailEcriture> findByTenant_IdAndCompte_Id(UUID tenant_id, UUID compte_id);

  @Query("""
      SELECT d FROM DetailEcriture d
      WHERE d.tenant.id = :tenant_id
      AND d.date_ecriture BETWEEN :start_date AND :end_date
      """)
  List<DetailEcriture> findByTenant_IdAndDateRange(@Param("tenant_id") UUID tenant_id,
      @Param("start_date") LocalDateTime start_date,
      @Param("end_date") LocalDateTime end_date);

  @Query("""
      SELECT COALESCE(SUM(d.montant_debit - d.montant_credit), 0)
      FROM DetailEcriture d
      WHERE d.tenant.id = :tenant_id AND d.compte.id = :compte_id
      """)
  Double calculateAccountBalance(@Param("tenant_id") UUID tenant_id, @Param("compte_id") UUID compte_id);

  @Query(value = """
      SELECT de.* FROM details_ecritures de
      WHERE de.tenant_id = :tenant_id
        AND COALESCE(de.pointee, false) = false
        AND (
          (de.sens = 'DEBIT'  AND de.montant_debit  = :montant) OR
          (de.sens = 'CREDIT' AND de.montant_credit = :montant)
        )
        AND de.date_ecriture BETWEEN :date_debut AND :date_fin
        AND (LOWER(de.libelle) LIKE LOWER('%' || :libelle || '%') OR :libelle IS NULL)
      ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :date_operation)))
      LIMIT 5
      """, nativeQuery = true)
  List<DetailEcriture> findCandidatesForPointage(
      @Param("tenant_id") UUID tenant_id,
      @Param("montant") BigDecimal montant,
      @Param("date_debut") LocalDateTime date_debut,
      @Param("date_fin") LocalDateTime date_fin,
      @Param("libelle") String libelle,
      @Param("date_operation") LocalDateTime date_operation);

  @Query(value = """
      SELECT de.* FROM details_ecritures de
      WHERE de.tenant_id = :tenant_id
        AND COALESCE(de.pointee, false) = false
        AND (
          (de.sens = 'DEBIT'  AND de.montant_debit  = :montant) OR
          (de.sens = 'CREDIT' AND de.montant_credit = :montant)
        )
        AND de.date_ecriture BETWEEN :date_debut AND :date_fin
      ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :date_reference)))
      LIMIT 3
      """, nativeQuery = true)
  List<DetailEcriture> findByTenantIdAndMontantAndDateProche(
      @Param("tenant_id") UUID tenant_id,
      @Param("montant") BigDecimal montant,
      @Param("date_debut") LocalDate date_debut,
      @Param("date_fin") LocalDate date_fin,
      @Param("date_reference") LocalDate date_reference);
}
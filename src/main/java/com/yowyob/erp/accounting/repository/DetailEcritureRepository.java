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

@Repository
public interface DetailEcritureRepository extends JpaRepository<DetailEcriture, UUID> {

    List<DetailEcriture> findByTenant_Id(UUID tenantId);
    List<DetailEcriture> findByTenant_IdAndEcriture_Id(UUID tenantId, UUID ecritureId);
    List<DetailEcriture> findByTenant_IdAndCompte_Id(UUID tenantId, UUID compteId);

    @Query("""
           SELECT d FROM DetailEcriture d 
           WHERE d.tenant.id = :tenantId 
           AND d.dateEcriture BETWEEN :startDate AND :endDate
           """)
    List<DetailEcriture> findByTenant_IdAndDateRange(UUID tenantId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
           SELECT COALESCE(SUM(d.montantDebit - d.montantCredit), 0) 
           FROM DetailEcriture d 
           WHERE d.tenant.id = :tenantId AND d.compte.id = :compteId
           """)
    Double calculateAccountBalance(UUID tenantId, UUID compteId);

    @Query(value = """
        SELECT de.* FROM details_ecritures de
        WHERE de.tenant_id = :tenantId
          AND COALESCE(de.pointee, false) = false
          AND (
            (de.sens = 'DEBIT'  AND de.montant_debit  = :montant) OR
            (de.sens = 'CREDIT' AND de.montant_credit = :montant)
          )
          AND de.date_ecriture BETWEEN :dateDebut AND :dateFin
          AND (LOWER(de.libelle) LIKE LOWER('%' || :libelle || '%') OR :libelle IS NULL)
        ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :dateOperation)))
        LIMIT 5
        """, nativeQuery = true)
    List<DetailEcriture> findCandidatesForPointage(
        @Param("tenantId") UUID tenantId,
        @Param("montant") BigDecimal montant,
        @Param("dateDebut") LocalDateTime dateDebut,
        @Param("dateFin") LocalDateTime dateFin,
        @Param("libelle") String libelle
    );

      @Query(value = """
      SELECT de.* FROM details_ecritures de
      WHERE de.tenant_id = :tenantId
        AND COALESCE(de.pointee, false) = false
        AND (
          (de.sens = 'DEBIT'  AND de.montant_debit  = :montant) OR
          (de.sens = 'CREDIT' AND de.montant_credit = :montant)
        )
        AND de.date_ecriture BETWEEN :dateDebut AND :dateFin
      ORDER BY ABS(EXTRACT(DAY FROM (de.date_ecriture - :dateReference)))
      LIMIT 3
      """, nativeQuery = true)
  List<DetailEcriture> findByTenantIdAndMontantAndDateProche(
      @Param("tenantId") UUID tenantId,
      @Param("montant") BigDecimal montant,
      @Param("dateDebut") LocalDate dateDebut,
      @Param("dateFin") LocalDate dateFin,
      @Param("dateReference") LocalDate dateReference
  );
}
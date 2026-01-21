package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.TauxChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TauxChange entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface TauxChangeRepository extends JpaRepository<TauxChange, UUID> {

    List<TauxChange> findByTenant_Id(UUID tenantId);

    /**
     * Finds the most recent exchange rate for a pair of currencies at or before a
     * specific date.
     */
    @Query("SELECT t FROM TauxChange t WHERE t.tenant.id = :tenantId " +
            "AND t.devise_source.id = :sourceId AND t.devise_cible.id = :targetId " +
            "AND t.date_effet <= :date ORDER BY t.date_effet DESC")
    List<TauxChange> findLatestRate(@Param("tenantId") UUID tenantId,
            @Param("sourceId") UUID sourceId,
            @Param("targetId") UUID targetId,
            @Param("date") LocalDateTime date);

    default Optional<TauxChange> findMostRecentRate(UUID tenantId, UUID sourceId, UUID targetId, LocalDateTime date) {
        List<TauxChange> rates = findLatestRate(tenantId, sourceId, targetId, date);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
    }
}

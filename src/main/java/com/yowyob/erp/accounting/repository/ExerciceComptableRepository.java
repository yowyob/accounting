package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.ExerciceComptable;
import com.yowyob.erp.accounting.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciceComptableRepository extends JpaRepository<ExerciceComptable, UUID> {

    List<ExerciceComptable> findByTenant(Tenant tenant);

    Optional<ExerciceComptable> findByTenantAndCode(Tenant tenant, String code);

    // Find active exercise for a given date
    @Query("SELECT e FROM ExerciceComptable e WHERE e.tenant = :tenant AND :date BETWEEN e.date_debut AND e.date_fin")
    Optional<ExerciceComptable> findActiveForDate(@Param("tenant") Tenant tenant, @Param("date") LocalDate date);
}

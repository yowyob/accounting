package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PlanComptableTemplate entity.
 * Provides access to official OHADA account templates.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface PlanComptableTemplateRepository extends JpaRepository<PlanComptableTemplate, UUID> {

    List<PlanComptableTemplate> findAll();

    Optional<PlanComptableTemplate> findById(UUID id);

    boolean existsByNumero(String numero);
}

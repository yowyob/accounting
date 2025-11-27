package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.PlanComptableTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanComptableTemplateRepository extends JpaRepository<PlanComptableTemplate, UUID> {

    List<PlanComptableTemplate> findAll();
    Optional<PlanComptableTemplate> findById(UUID id);
    boolean existsByNumero(String numero);
    
}

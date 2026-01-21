package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Taxe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Taxe entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface TaxeRepository extends JpaRepository<Taxe, UUID> {

    List<Taxe> findByTenant_Id(UUID tenant_id);

    List<Taxe> findByTenant_IdAndActifTrue(UUID tenant_id);

    Optional<Taxe> findByTenant_IdAndId(UUID tenant_id, UUID id);

    Optional<Taxe> findByTenant_IdAndCode(UUID tenant_id, String code);

    boolean existsByTenant_IdAndCode(UUID tenant_id, String code);
}

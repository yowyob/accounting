package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Devise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Devise entity.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Repository
public interface DeviseRepository extends JpaRepository<Devise, UUID> {

    Optional<Devise> findByCode(String code);

    List<Devise> findByActifTrue();

    boolean existsByCode(String code);
}

package com.yowyob.erp.accounting.infrastructure.persistence.repository;

import com.yowyob.erp.accounting.domain.model.PrixCessionVersion;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface PrixCessionVersionRepository extends R2dbcRepository<PrixCessionVersion, UUID> {
    Flux<PrixCessionVersion> findByPrixCessionIdOrderByDateDebutDesc(UUID prixCessionId);
}

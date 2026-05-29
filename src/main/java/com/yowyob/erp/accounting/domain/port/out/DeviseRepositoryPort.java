package com.yowyob.erp.accounting.domain.port.out;

import com.yowyob.erp.accounting.domain.model.Devise;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for Devise persistence operations.
 */
public interface DeviseRepositoryPort {
    Mono<Devise> findByCode(String code);
    Flux<Devise> findByActifTrue();
    Mono<Boolean> existsByCode(String code);
}

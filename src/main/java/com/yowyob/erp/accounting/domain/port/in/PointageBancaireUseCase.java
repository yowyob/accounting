package com.yowyob.erp.accounting.domain.port.in;
import org.springframework.http.codec.multipart.FilePart;

import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import java.time.LocalDate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the PointageBancaire operations.
 */
public interface PointageBancaireUseCase {
    Mono<Integer> importerEtPointer(FilePart file);
}

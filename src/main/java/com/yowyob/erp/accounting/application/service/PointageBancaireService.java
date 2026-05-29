package com.yowyob.erp.accounting.application.service;
import com.yowyob.erp.accounting.domain.port.in.PointageBancaireUseCase;

import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.DetailEcritureRepository;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Reactive Service for bank statement reconciliation (Pointage Bancaire).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointageBancaireService implements PointageBancaireUseCase {

    private final DetailEcritureRepository detailRepo;
    private final CsvReleveBancaireService csvService;

    /**
     * Imports a bank statement and automatically reconciles entries.
     */
    @Transactional
    public Mono<Integer> importerEtPointer(FilePart file) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organization_id -> csvService.parseReleveBancaire(file)
                        .flatMapMany(Flux::fromIterable)
                        .concatMap(op -> {
                            LocalDate dDebut = op.getDateOperation().toLocalDate();
                            LocalDate dFin = dDebut.plusDays(1);

                            return detailRepo
                                    .findByOrganizationIdAndMontantAndDateProche(organization_id, op.getMontant(), dDebut, dFin,
                                            dDebut)
                                    .collectList()
                                    .flatMap(candidats -> {
                                        if (!candidats.isEmpty()) {
                                            DetailEcriture de = candidats.get(0);
                                            de.setPointee(true);
                                            String lib = op.getLibelle() != null ? op.getLibelle() : "";
                                            de.setReference_bancaire(
                                                    "AUTO -> " + lib.substring(0, Math.min(50, lib.length())));
                                            return detailRepo.save(de).thenReturn(1);
                                        }
                                        return Mono.just(0);
                                    });
                        })
                        .reduce(0, (a, b) -> a + b)
                        .doOnSuccess(count -> log.info("✅ {} operations automatically pointed for organization {}", count,
                                organization_id)));
    }
}
package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.DetailEcriture;
import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Reactive Service for bank statement reconciliation (Pointage Bancaire).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointageBancaireService {

    private final DetailEcritureRepository detailRepo;
    private final CsvReleveBancaireService csvService;

    /**
     * Imports a bank statement and automatically reconciles entries.
     */
    @SuppressWarnings("null")
    @Transactional
    public Mono<Integer> importerEtPointer(MultipartFile file) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> csvService.parseReleveBancaire(file)
                        .flatMapMany(Flux::fromIterable)
                        .concatMap(op -> {
                            LocalDate debut = op.getDateOperation();
                            LocalDate fin = op.getDateOperation().plusDays(1);
                            LocalDate ref = op.getDateOperation();

                            return detailRepo
                                    .findByTenantIdAndMontantAndDateProche(tenant_id, op.getMontant(), debut, fin, ref)
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
                        .reduce(0, Integer::sum)
                        .doOnSuccess(count -> log.info("✅ {} operations automatically pointed for tenant {}", count,
                                tenant_id)));
    }
}
package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import com.yowyob.erp.accounting.service.CsvReleveBancaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reactive Controller for bank reconciliation (pointage).
 */
@RestController
@RequestMapping("/api/accounting/pointage")
@RequiredArgsConstructor
@Tag(name = "Accounting Pointage Bancaire", description = "Pointage automatique des opérations bancaires")
@SecurityRequirement(name = "BasicAuth")
@Slf4j
public class PointageController {

    private final CsvReleveBancaireService csvService;
    private final DetailEcritureRepository detailEcritureRepository;

    /**
     * Imports a bank statement and automatically points matching accounting
     * entries.
     */
    @SuppressWarnings("null")
    @PostMapping("/import")
    @Operation(summary = "Import and point bank statement")
    public Mono<ResponseEntity<String>> importerReleve(MultipartFile file) {
        // Assuming TenantContext.getCurrentTenant() works in reactive context if
        // configured
        // Otherwise, it should be retrieved from ReactiveSecurityContext or through the
        // service layer.
        // For now, assume it's available or handled by the service.

        return csvService.parseReleveBancaire(file)
                .flatMapMany(Flux::fromIterable)
                .flatMap(op -> {
                    // This assumes findByTenantIdAndMontantAndDateProche returns a Flux
                    LocalDate dOp = op.getDateOperation().toLocalDate();
                    return detailEcritureRepository.findByTenantIdAndMontantAndDateProche(
                            null,
                            op.getMontant(),
                            dOp,
                            dOp.plusDays(1),
                            dOp)
                            .next() // Take the first candidate
                            .flatMap(d -> {
                                d.setPointee(true);
                                d.setReference_bancaire(op.getLibelle());
                                return detailEcritureRepository.save(d).thenReturn(1);
                            })
                            .defaultIfEmpty(0);
                })
                .reduce(0, (a, b) -> a + b)
                .map(count -> ResponseEntity.ok(count + " opérations pointées automatiquement"))
                .onErrorResume(e -> {
                    log.error("Error during pointage: {}", e.getMessage());
                    return Mono.just(ResponseEntity.ok("ERREUR LORS DU POINTAGE " + e.getMessage()));
                });
    }
}
package com.yowyob.erp.accounting.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.domain.port.in.*;
import com.yowyob.erp.accounting.infrastructure.web.dto.*;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.application.service.IdempotentCreateSupport;
import com.yowyob.erp.shared.domain.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineSyncService {

    private final JournalComptableUseCase journalService;
    private final TaxeUseCase taxeService;
    private final DeviseUseCase deviseService;
    private final OperationComptableUseCase operationService;
    private final PlanComptableUseCase planService;
    private final PeriodeComptableUseCase periodeService;
    private final EcritureComptableUseCase ecritureService;
    private final AxeAnalytiqueUseCase axeAnalytiqueService;
    private final CompteAnalytiqueUseCase compteAnalytiqueService;
    private final ChargeAnalytiqueService chargeAnalytiqueService;
    private final JournalAnalytiqueService journalAnalytiqueService;
    private final EcritureAnalytiqueService ecritureAnalytiqueService;
    private final IdempotentCreateSupport idempotentCreate;
    private final ObjectMapper objectMapper;

    public Mono<SyncPullResponseDto> pull(LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.MIN;

        Mono<List<?>> cgJournaux = journalService.getAllJournaux().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgTaxes = taxeService.getAllTaxes().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgDevises = deviseService.getAllDevises().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgOperations = operationService.getAllOperations().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgPlan = planService.getAllAccounts().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgPeriodes = periodeService.getAllPeriodes().defaultIfEmpty(List.of()).map(l -> l);
        Mono<List<?>> cgEcritures = ecritureService.getAll().defaultIfEmpty(List.of()).map(l -> l);

        Mono<List<?>> caCentres = toList(axeAnalytiqueService.getAll());
        Mono<List<?>> caComptes = toList(compteAnalytiqueService.getAll());
        Mono<List<?>> caCharges = toList(chargeAnalytiqueService.getAll(null, null));
        Mono<List<?>> caJournaux = toList(journalAnalytiqueService.getAll());
        Mono<List<?>> caEcritures = toList(ecritureAnalytiqueService.getAll(null, null));

        return Mono.zip(
                Mono.zip(cgJournaux, cgTaxes, cgDevises, cgOperations, cgPlan, cgPeriodes, cgEcritures),
                Mono.zip(caCentres, caComptes, caCharges, caJournaux, caEcritures)
        ).map(t -> {
            var cg = t.getT1();
            var ca = t.getT2();
            Map<String, List<?>> changes = new HashMap<>();
            changes.put("cg.journaux", filterSince(cg.getT1(), effectiveSince));
            changes.put("cg.taxes", filterSince(cg.getT2(), effectiveSince));
            changes.put("cg.devises", filterSince(cg.getT3(), effectiveSince));
            changes.put("cg.operations", filterSince(cg.getT4(), effectiveSince));
            changes.put("cg.plan_comptable", filterSince(cg.getT5(), effectiveSince));
            changes.put("cg.periodes", filterSince(cg.getT6(), effectiveSince));
            changes.put("ecriture_comptable", filterSince(cg.getT7(), effectiveSince));
            changes.put("ca.centres", filterSince(ca.getT1(), effectiveSince));
            changes.put("ca.comptes", filterSince(ca.getT2(), effectiveSince));
            changes.put("ca.charges", filterSince(ca.getT3(), effectiveSince));
            changes.put("ca.journaux", filterSince(ca.getT4(), effectiveSince));
            changes.put("ecriture_analytique", filterSince(ca.getT5(), effectiveSince));
            return SyncPullResponseDto.builder()
                    .serverTime(LocalDateTime.now())
                    .since(effectiveSince)
                    .changes(changes)
                    .build();
        });
    }

    public Mono<SyncPushResponseDto> push(SyncPushRequestDto request) {
        List<SyncPushRequestDto.SyncPushOperationDto> ops =
                request.getOperations() == null ? List.of() : request.getOperations();

        return Flux.fromIterable(ops)
                .concatMap(this::processOne)
                .collectList()
                .map(results -> {
                    int synced = 0;
                    int failed = 0;
                    int already = 0;
                    for (SyncPushResponseDto.SyncPushItemResultDto r : results) {
                        if ("ALREADY_PROCESSED".equals(r.getStatus())) already++;
                        else if ("OK".equals(r.getStatus())
                                || "CREATED".equals(r.getStatus())
                                || "DELETED".equals(r.getStatus())) synced++;
                        else if ("SKIPPED".equals(r.getStatus())) { /* ignore */ }
                        else failed++;
                    }
                    return SyncPushResponseDto.builder()
                            .results(results)
                            .synced(synced)
                            .failed(failed)
                            .alreadyProcessed(already)
                            .build();
                });
    }

    private <T> Mono<List<?>> toList(Flux<T> flux) {
        return flux.collectList().defaultIfEmpty(List.of()).map(l -> l);
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> processOne(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(orgId -> dispatch(orgId, op))
                .onErrorResume(err -> {
                    log.warn("Sync push failed for {} {}: {}", op.getEntity(), op.getAction(), err.getMessage());
                    boolean conflict = err instanceof ConflictException
                            || (err.getMessage() != null && err.getMessage().toLowerCase().contains("conflit"));
                    return Mono.just(SyncPushResponseDto.SyncPushItemResultDto.builder()
                            .clientMutationId(op.getClientMutationId())
                            .entityId(op.getEntityId())
                            .status(conflict ? "CONFLICT" : "FAILED")
                            .message(err.getMessage())
                            .build());
                });
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> dispatch(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        String entity = op.getEntity() != null ? op.getEntity() : "";
        String action = op.getAction() != null ? op.getAction() : "";

        return switch (entity) {
            case "cg.journaux" -> switch (action) {
                case "CREATE" -> createJournal(orgId, op);
                case "UPDATE" -> updateJournal(op);
                case "DELETE" -> deleteJournal(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "cg.taxes" -> switch (action) {
                case "CREATE" -> createTaxe(orgId, op);
                case "UPDATE" -> updateTaxe(op);
                case "DELETE" -> deleteTaxe(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "cg.devises" -> switch (action) {
                case "CREATE" -> createDevise(orgId, op);
                case "UPDATE" -> updateDevise(op);
                case "DELETE" -> deleteDevise(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "cg.operations" -> switch (action) {
                case "CREATE" -> createOperation(orgId, op);
                case "UPDATE" -> updateOperation(op);
                case "DELETE" -> deleteOperation(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "cg.plan_comptable" -> switch (action) {
                case "CREATE" -> createPlan(orgId, op);
                case "UPDATE" -> updatePlan(op);
                case "DELETE" -> deletePlan(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "cg.periodes" -> switch (action) {
                case "CREATE" -> createPeriode(orgId, op);
                case "UPDATE" -> updatePeriode(op);
                case "DELETE" -> deletePeriode(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "ca.centres" -> switch (action) {
                case "CREATE" -> createAxe(orgId, op);
                case "UPDATE" -> updateAxe(op);
                case "DELETE" -> deleteAxe(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "ca.comptes", "ca.plan_comptes" -> switch (action) {
                case "CREATE" -> createCompteAnalytique(orgId, op);
                case "UPDATE" -> updateCompteAnalytique(op);
                case "DELETE" -> deleteCompteAnalytique(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "ca.charges" -> switch (action) {
                case "CREATE" -> createCharge(orgId, op);
                case "UPDATE" -> updateCharge(op);
                case "DELETE" -> deleteCharge(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "ca.journaux" -> switch (action) {
                case "CREATE" -> createJournalAnalytique(orgId, op);
                case "UPDATE" -> updateJournalAnalytique(op);
                case "DELETE" -> deleteJournalAnalytique(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            case "ecriture_analytique" -> switch (action) {
                case "CREATE" -> createEcritureAnalytique(op);
                case "UPDATE" -> updateEcritureAnalytique(op);
                case "DELETE" -> deleteEcritureAnalytique(op);
                default -> skipped(op, "Action non supportée: " + action);
            };
            default -> skipped(op, "Entity not supported in batch: " + entity);
        };
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> skipped(
            SyncPushRequestDto.SyncPushOperationDto op, String message) {
        return Mono.just(SyncPushResponseDto.SyncPushItemResultDto.builder()
                .clientMutationId(op.getClientMutationId())
                .entityId(op.getEntityId())
                .status("SKIPPED")
                .message(message)
                .build());
    }

    private UUID parseEntityId(SyncPushRequestDto.SyncPushOperationDto op) {
        try {
            return UUID.fromString(op.getEntityId());
        } catch (Exception e) {
            Object payloadId = op.getPayload() != null ? op.getPayload().get("id") : null;
            if (payloadId != null) {
                return UUID.fromString(String.valueOf(payloadId));
            }
            throw new IllegalArgumentException("entityId UUID invalide: " + op.getEntityId());
        }
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> ok(
            SyncPushRequestDto.SyncPushOperationDto op, Object data, String status) {
        String id = op.getEntityId();
        try {
            Object rawId = data.getClass().getMethod("getId").invoke(data);
            if (rawId != null) id = rawId.toString();
        } catch (Exception ignored) {
            // keep entityId
        }
        return Mono.just(SyncPushResponseDto.SyncPushItemResultDto.builder()
                .clientMutationId(op.getClientMutationId())
                .entityId(id)
                .status(status)
                .data(data)
                .build());
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleted(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return Mono.just(SyncPushResponseDto.SyncPushItemResultDto.builder()
                .clientMutationId(op.getClientMutationId())
                .entityId(op.getEntityId())
                .status("DELETED")
                .build());
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateJournal(
            SyncPushRequestDto.SyncPushOperationDto op) {
        JournalComptableDto dto = objectMapper.convertValue(op.getPayload(), JournalComptableDto.class);
        return journalService.updateJournalComptable(parseEntityId(op), dto)
                .flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteJournal(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return journalService.deleteJournalComptable(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateTaxe(
            SyncPushRequestDto.SyncPushOperationDto op) {
        TaxeDto dto = objectMapper.convertValue(op.getPayload(), TaxeDto.class);
        return taxeService.updateTaxe(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteTaxe(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return taxeService.deleteTaxe(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateDevise(
            SyncPushRequestDto.SyncPushOperationDto op) {
        DeviseDto dto = objectMapper.convertValue(op.getPayload(), DeviseDto.class);
        return deviseService.updateDevise(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteDevise(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return deviseService.deleteDevise(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateOperation(
            SyncPushRequestDto.SyncPushOperationDto op) {
        OperationComptableDto dto = objectMapper.convertValue(op.getPayload(), OperationComptableDto.class);
        return operationService.updateOperation(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteOperation(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return operationService.deleteOperation(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updatePlan(
            SyncPushRequestDto.SyncPushOperationDto op) {
        PlanComptableDto dto = objectMapper.convertValue(op.getPayload(), PlanComptableDto.class);
        return planService.updateAccount(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deletePlan(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return planService.deactivateAccount(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updatePeriode(
            SyncPushRequestDto.SyncPushOperationDto op) {
        PeriodeComptableDto dto = objectMapper.convertValue(op.getPayload(), PeriodeComptableDto.class);
        return periodeService.updatePeriode(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deletePeriode(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return periodeService.deletePeriode(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createJournal(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        JournalComptableDto dto = objectMapper.convertValue(op.getPayload(), JournalComptableDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "journal_comptable",
                        journalService::getJournalComptable,
                        () -> journalService.createJournalComptable(dto),
                        JournalComptableDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createTaxe(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        TaxeDto dto = objectMapper.convertValue(op.getPayload(), TaxeDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "taxe",
                        taxeService::getTaxe,
                        () -> taxeService.createTaxe(dto),
                        TaxeDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createDevise(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        DeviseDto dto = objectMapper.convertValue(op.getPayload(), DeviseDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "devise",
                        deviseService::getDevise,
                        () -> deviseService.createDevise(dto),
                        DeviseDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createOperation(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        OperationComptableDto dto = objectMapper.convertValue(op.getPayload(), OperationComptableDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "operation_comptable",
                        operationService::getOperation,
                        () -> operationService.createOperation(dto),
                        OperationComptableDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createPlan(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        PlanComptableDto dto = objectMapper.convertValue(op.getPayload(), PlanComptableDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "plan_comptable",
                        planService::getAccountById,
                        () -> planService.createAccount(dto),
                        PlanComptableDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createPeriode(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        PeriodeComptableDto dto = objectMapper.convertValue(op.getPayload(), PeriodeComptableDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "periode_comptable",
                        periodeService::getPeriode,
                        () -> periodeService.createPeriode(dto),
                        PeriodeComptableDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createAxe(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        AxeAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), AxeAnalytiqueDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "axe_analytique",
                        axeAnalytiqueService::findById,
                        () -> axeAnalytiqueService.create(dto),
                        AxeAnalytiqueDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateAxe(
            SyncPushRequestDto.SyncPushOperationDto op) {
        AxeAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), AxeAnalytiqueDto.class);
        return axeAnalytiqueService.update(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteAxe(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return axeAnalytiqueService.delete(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createCompteAnalytique(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        CompteAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), CompteAnalytiqueDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "compte_analytique",
                        compteAnalytiqueService::findById,
                        () -> compteAnalytiqueService.create(dto),
                        CompteAnalytiqueDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateCompteAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        CompteAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), CompteAnalytiqueDto.class);
        return compteAnalytiqueService.update(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteCompteAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return compteAnalytiqueService.delete(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createCharge(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        ChargeAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), ChargeAnalytiqueDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "charge_analytique",
                        chargeAnalytiqueService::findById,
                        () -> chargeAnalytiqueService.create(dto),
                        ChargeAnalytiqueDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateCharge(
            SyncPushRequestDto.SyncPushOperationDto op) {
        ChargeAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), ChargeAnalytiqueDto.class);
        return chargeAnalytiqueService.update(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteCharge(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return chargeAnalytiqueService.delete(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createJournalAnalytique(
            UUID orgId, SyncPushRequestDto.SyncPushOperationDto op) {
        JournalAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), JournalAnalytiqueDto.class);
        return idempotentCreate.create(orgId, op.getClientMutationId(), "journal_analytique",
                        journalAnalytiqueService::findById,
                        () -> journalAnalytiqueService.create(dto),
                        JournalAnalytiqueDto::getId)
                .map(r -> toResult(op, r));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateJournalAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        JournalAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), JournalAnalytiqueDto.class);
        return journalAnalytiqueService.update(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteJournalAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return journalAnalytiqueService.delete(parseEntityId(op)).then(deleted(op));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> createEcritureAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        EcritureAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), EcritureAnalytiqueDto.class);
        return ecritureAnalytiqueService.create(dto, op.getClientMutationId())
                .map(r -> SyncPushResponseDto.SyncPushItemResultDto.builder()
                        .clientMutationId(op.getClientMutationId())
                        .entityId(r.getDto().getId() != null ? r.getDto().getId().toString() : op.getEntityId())
                        .status(r.isAlreadyProcessed() ? "ALREADY_PROCESSED" : "CREATED")
                        .data(r.getDto())
                        .build());
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> updateEcritureAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        EcritureAnalytiqueDto dto = objectMapper.convertValue(op.getPayload(), EcritureAnalytiqueDto.class);
        return ecritureAnalytiqueService.update(parseEntityId(op), dto).flatMap(data -> ok(op, data, "OK"));
    }

    private Mono<SyncPushResponseDto.SyncPushItemResultDto> deleteEcritureAnalytique(
            SyncPushRequestDto.SyncPushOperationDto op) {
        return ecritureAnalytiqueService.delete(parseEntityId(op)).then(deleted(op));
    }

    private <T> SyncPushResponseDto.SyncPushItemResultDto toResult(
            SyncPushRequestDto.SyncPushOperationDto op, IdempotentCreateSupport.Result<T> r) {
        UUID id = null;
        try {
            Object rawId = r.data().getClass().getMethod("getId").invoke(r.data());
            if (rawId instanceof UUID uuid) id = uuid;
        } catch (Exception ignored) {
            // ignore
        }
        return SyncPushResponseDto.SyncPushItemResultDto.builder()
                .clientMutationId(op.getClientMutationId())
                .entityId(id != null ? id.toString() : op.getEntityId())
                .status(r.alreadyProcessed() ? "ALREADY_PROCESSED" : "CREATED")
                .data(r.data())
                .build();
    }

    private List<?> filterSince(List<?> items, LocalDateTime since) {
        if (items == null || items.isEmpty()) return List.of();
        if (since == null || since.equals(LocalDateTime.MIN)) return items;
        return items.stream().filter(item -> {
            LocalDateTime updated = extractUpdatedAt(item);
            return updated == null || !updated.isBefore(since);
        }).collect(Collectors.toList());
    }

    private LocalDateTime extractUpdatedAt(Object item) {
        try {
            Object v = item.getClass().getMethod("getUpdated_at").invoke(item);
            if (v instanceof LocalDateTime ldt) return ldt;
        } catch (Exception ignored) {
            // try camelCase
        }
        try {
            Object v = item.getClass().getMethod("getUpdatedAt").invoke(item);
            if (v instanceof LocalDateTime ldt) return ldt;
        } catch (Exception ignored) {
            // no updated field
        }
        return null;
    }
}

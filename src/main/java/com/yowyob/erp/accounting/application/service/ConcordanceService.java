package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.LigneConcordance;
import com.yowyob.erp.accounting.domain.model.PeriodeComptable;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.*;
import com.yowyob.erp.accounting.infrastructure.web.dto.*;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcordanceService {

    private final LigneConcordanceRepository ligneRepo;
    private final EcritureAnalytiqueRepository ecritureRepo;
    private final PeriodeComptableRepository periodeComptableRepo;
    private final ChargeVentileeService chargeVentileeService;

    public Mono<ConcordancePeriodeDto> getPeriode(UUID periodeId) {
        return getLignesManuelles(periodeId)
            .flatMap(manuelles -> compute(periodeId, manuelles)
                .map(calcul -> ConcordancePeriodeDto.builder()
                    .periodeId(periodeId)
                    .lignesManuelles(manuelles)
                    .calcul(calcul)
                    .build()));
    }

    public Mono<List<LigneConcordanceDto>> getLignesManuelles(UUID periodeId) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> ligneRepo.findByOrganizationIdAndPeriodeId(orgId, periodeId)
                .map(this::toDto)
                .collectList());
    }

    @Transactional
    public Mono<List<LigneConcordanceDto>> replaceLignesManuelles(UUID periodeId, List<LigneConcordanceDto> lignes) {
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return ligneRepo.deleteByOrganizationIdAndPeriodeId(orgId, periodeId)
                    .thenMany(Flux.fromIterable(lignes != null ? lignes : List.<LigneConcordanceDto>of()))
                    .map(dto -> LigneConcordance.builder()
                        .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
                        .organizationId(orgId)
                        .periodeId(periodeId)
                        .type(dto.getType())
                        .label(dto.getLabel())
                        .description(dto.getDescription())
                        .signe(dto.getSigne())
                        .montant(dto.getMontant())
                        .chargeVentileeId(dto.getChargeVentileeId())
                        .autoGeneree(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .createdBy(user)
                        .build())
                    .flatMap(ligneRepo::save)
                    .map(this::toDto)
                    .collectList();
            });
    }

    public Mono<ConcordanceCalculDto> compute(UUID periodeId) {
        return getLignesManuelles(periodeId).flatMap(manuelles -> compute(periodeId, manuelles));
    }

    private Mono<ConcordanceCalculDto> compute(UUID periodeId, List<LigneConcordanceDto> manuelles) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> chargeVentileeService.getAll(periodeId, null).collectList()
                .zipWith(ecritureRepo.findByOrganizationIdAndStatutAndPeriodeId(orgId, "VALIDEE", periodeId).collectList())
                .flatMap(tuple -> {
                    List<ChargeVentileeDto> charges = tuple.getT1();
                    var ecritures = tuple.getT2();

                    List<LigneConcordanceDto> auto = ConcordanceCalculHelper.buildLignesAutoFromCharges(charges, periodeId);
                    List<LigneConcordanceDto> lignes = ConcordanceCalculHelper.mergeLignes(manuelles, auto);
                    BigDecimal sommeDiff = ConcordanceCalculHelper.sommeLignes(lignes);
                    BigDecimal resultCG = ConcordanceCalculHelper.zero();
                    BigDecimal totalAnalytique = ConcordanceCalculHelper.sumEcrituresValidees(ecritures, periodeId);
                    BigDecimal resultCA = resultCG.add(sommeDiff);
                    BigDecimal ecartVerif = resultCA.subtract(totalAnalytique);

                    return resolvePeriodeCgLibelle(orgId, periodeId)
                        .map(libelle -> ConcordanceCalculDto.builder()
                            .periodeId(periodeId)
                            .periodeCgLibelle(libelle)
                            .resultCG(resultCG)
                            .totalChargesCG(ConcordanceCalculHelper.zero())
                            .totalProduitsCG(ConcordanceCalculHelper.zero())
                            .totalNonInc(ConcordanceCalculHelper.sumChargesNonInc(charges, periodeId))
                            .totalIncorporable(ConcordanceCalculHelper.sumChargesIncorporable(charges, periodeId))
                            .totalAnalytiqueEcritures(totalAnalytique)
                            .sommeDiff(sommeDiff)
                            .resultCA(resultCA)
                            .ecartVerif(ecartVerif)
                            .concordanceOk(ConcordanceCalculHelper.isConcordanceOk(ecartVerif))
                            .lignesManuelles(manuelles)
                            .lignesAuto(auto)
                            .lignes(lignes)
                            .build());
                }));
    }

    private Mono<String> resolvePeriodeCgLibelle(UUID orgId, UUID periodeId) {
        return periodeComptableRepo.findByOrganization_IdAndId(orgId, periodeId)
            .map(PeriodeComptable::getCode)
            .defaultIfEmpty("");
    }

    private LigneConcordanceDto toDto(LigneConcordance entity) {
        return LigneConcordanceDto.builder()
            .id(entity.getId())
            .type(entity.getType())
            .label(entity.getLabel())
            .description(entity.getDescription())
            .signe(entity.getSigne())
            .montant(entity.getMontant())
            .chargeVentileeId(entity.getChargeVentileeId())
            .autoGeneree(entity.getAutoGeneree())
            .build();
    }
}

package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ChargeVentilee;
import com.yowyob.erp.accounting.domain.model.VentilationCharge;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.AxeAnalytiqueRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.ChargeVentileeRepository;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.VentilationChargeRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeVentileeDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeVentileeStatsDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.VentilationAxeDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import com.yowyob.erp.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeVentileeService {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final ChargeVentileeRepository repository;
    private final VentilationChargeRepository ventilationRepo;
    private final AxeAnalytiqueRepository axeRepo;

    @Transactional
    public Mono<ChargeVentileeDto> create(ChargeVentileeDto dto) {
        validateVentilationRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                ChargeVentilee entity = toEntity(dto, orgId, user, true);
                return repository.save(entity)
                    .flatMap(saved -> saveVentilations(saved.getId(), dto.getVentilations())
                        .then(enrichDto(saved)));
            });
    }

    @Transactional
    public Mono<ChargeVentileeDto> update(UUID id, ChargeVentileeDto dto) {
        validateVentilationRules(dto);
        return ReactiveOrganizationContext.getOrganizationId()
            .zipWith(ReactiveOrganizationContext.getCurrentUser().defaultIfEmpty("system"))
            .flatMap(t -> {
                UUID orgId = t.getT1();
                String user = t.getT2();
                return repository.findById(id)
                    .filter(c -> orgId.equals(c.getOrganizationId()))
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeVentilee", id.toString())))
                    .flatMap(existing -> {
                        existing.setChargeSourceId(dto.getChargeSourceId());
                        existing.setCompteCG(dto.getCompteCG());
                        existing.setLibelle(dto.getLibelle());
                        existing.setMontantTotal(dto.getMontantTotal());
                        existing.setIncorporable(dto.getIncorporable() != null ? dto.getIncorporable() : true);
                        existing.setPeriodeId(dto.getPeriodeId());
                        existing.setPeriodeCgId(dto.getPeriodeCgId() != null ? dto.getPeriodeCgId() : dto.getPeriodeId());
                        existing.setUpdatedAt(LocalDateTime.now());
                        existing.setUpdatedBy(user);
                        existing.setNotNew();

                        return repository.save(existing)
                            .flatMap(saved -> ventilationRepo.deleteByChargeVentileeId(saved.getId())
                                .then(saveVentilations(saved.getId(), dto.getVentilations()))
                                .then(enrichDto(saved)));
                    });
            });
    }

    @Transactional
    public Mono<Void> delete(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeVentilee", id.toString())))
                .flatMap(existing -> ventilationRepo.deleteByChargeVentileeId(existing.getId())
                    .then(repository.delete(existing))));
    }

    public Mono<ChargeVentileeDto> findById(UUID id) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMap(orgId -> repository.findById(id)
                .filter(c -> orgId.equals(c.getOrganizationId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("ChargeVentilee", id.toString())))
                .flatMap(this::enrichDto));
    }

    public Flux<ChargeVentileeDto> getAll(UUID periodeId, Boolean incorporable) {
        return ReactiveOrganizationContext.getOrganizationId()
            .flatMapMany(orgId -> {
                if (periodeId != null && incorporable != null) {
                    return repository.findByOrganizationIdAndPeriodeIdAndIncorporable(orgId, periodeId, incorporable);
                }
                if (periodeId != null) {
                    return repository.findByOrganizationIdAndPeriodeId(orgId, periodeId);
                }
                if (incorporable != null) {
                    return repository.findByOrganizationIdAndIncorporable(orgId, incorporable);
                }
                return repository.findByOrganizationId(orgId);
            })
            .flatMap(this::enrichDto);
    }

    public Mono<ChargeVentileeStatsDto> getStats(UUID periodeId) {
        return getAll(periodeId, null).collectList()
            .map(charges -> {
                BigDecimal totalIncorporable = BigDecimal.ZERO;
                BigDecimal totalNonIncorporable = BigDecimal.ZERO;
                BigDecimal totalVentile = BigDecimal.ZERO;
                BigDecimal totalNonVentile = BigDecimal.ZERO;

                for (ChargeVentileeDto c : charges) {
                    BigDecimal montant = c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO;
                    if (Boolean.TRUE.equals(c.getIncorporable())) {
                        totalIncorporable = totalIncorporable.add(montant);
                        if (c.getVentilations() != null && !c.getVentilations().isEmpty()) {
                            totalVentile = totalVentile.add(montant);
                        } else {
                            totalNonVentile = totalNonVentile.add(montant);
                        }
                    } else {
                        totalNonIncorporable = totalNonIncorporable.add(montant);
                    }
                }

                return ChargeVentileeStatsDto.builder()
                    .totalIncorporable(totalIncorporable)
                    .totalNonIncorporable(totalNonIncorporable)
                    .totalVentile(totalVentile)
                    .totalNonVentile(totalNonVentile)
                    .build();
            });
    }

    private void validateVentilationRules(ChargeVentileeDto dto) {
        boolean incorporable = dto.getIncorporable() == null || dto.getIncorporable();
        List<VentilationAxeDto> vents = dto.getVentilations();

        if (!incorporable) {
            if (vents != null && !vents.isEmpty()) {
                throw new BusinessException("Une charge non incorporable ne peut pas avoir de ventilation analytique.");
            }
            return;
        }

        if (vents == null || vents.isEmpty()) {
            return;
        }

        BigDecimal total = vents.stream()
            .map(v -> v.getPourcentage() != null ? v.getPourcentage() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.subtract(new BigDecimal("100")).abs().compareTo(TOLERANCE) > 0) {
            throw new BusinessException(
                "Les pourcentages de ventilation doivent totaliser 100 % (actuel : "
                    + total.setScale(1, RoundingMode.HALF_UP) + " %).");
        }
    }

    private ChargeVentilee toEntity(ChargeVentileeDto dto, UUID orgId, String user, boolean isNew) {
        LocalDateTime now = LocalDateTime.now();
        ChargeVentilee.ChargeVentileeBuilder builder = ChargeVentilee.builder()
            .organizationId(orgId)
            .chargeSourceId(dto.getChargeSourceId())
            .compteCG(dto.getCompteCG())
            .libelle(dto.getLibelle())
            .montantTotal(dto.getMontantTotal())
            .incorporable(dto.getIncorporable() != null ? dto.getIncorporable() : true)
            .periodeId(dto.getPeriodeId())
            .periodeCgId(dto.getPeriodeCgId() != null ? dto.getPeriodeCgId() : dto.getPeriodeId())
            .createdBy(user)
            .updatedBy(user);

        if (isNew) {
            builder.id(UUID.randomUUID()).createdAt(now).updatedAt(now);
        } else {
            builder.id(dto.getId()).updatedAt(now);
        }

        return builder.build();
    }

    private Mono<Void> saveVentilations(UUID chargeId, List<VentilationAxeDto> ventilations) {
        if (ventilations == null || ventilations.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(ventilations)
            .map(v -> VentilationCharge.builder()
                .id(UUID.randomUUID())
                .chargeVentileeId(chargeId)
                .axeId(v.getAxeId())
                .centreId(v.getCentreId())
                .pourcentage(v.getPourcentage())
                .build())
            .flatMap(ventilationRepo::save)
            .then();
    }

    private Mono<ChargeVentileeDto> enrichDto(ChargeVentilee charge) {
        return ventilationRepo.findByChargeVentileeId(charge.getId()).collectList()
            .flatMap(vents -> Flux.fromIterable(vents)
                .flatMap(v -> {
                    Mono<String> axeLibelle = v.getAxeId() != null
                        ? axeRepo.findById(v.getAxeId()).map(a -> a.getLibelle()).defaultIfEmpty("")
                        : Mono.just("");
                    Mono<String> centreLibelle = v.getCentreId() != null
                        ? axeRepo.findById(v.getCentreId()).map(a -> a.getLibelle()).defaultIfEmpty("")
                        : Mono.just("");
                    return Mono.zip(axeLibelle, centreLibelle)
                        .map(t -> VentilationAxeDto.builder()
                            .id(v.getId())
                            .axeId(v.getAxeId())
                            .axeLibelle(t.getT1())
                            .centreId(v.getCentreId())
                            .centreLibelle(t.getT2())
                            .pourcentage(v.getPourcentage())
                            .build());
                })
                .collectList()
                .map(vDtos -> ChargeVentileeDto.builder()
                    .id(charge.getId())
                    .chargeSourceId(charge.getChargeSourceId())
                    .compteCG(charge.getCompteCG())
                    .libelle(charge.getLibelle())
                    .montantTotal(charge.getMontantTotal())
                    .incorporable(charge.getIncorporable())
                    .periodeId(charge.getPeriodeId())
                    .periodeCgId(charge.getPeriodeCgId())
                    .ventilations(vDtos)
                    .build()));
    }
}

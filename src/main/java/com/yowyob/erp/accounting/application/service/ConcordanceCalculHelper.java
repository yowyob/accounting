package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.ChargeVentilee;
import com.yowyob.erp.accounting.domain.model.EcritureAnalytique;
import com.yowyob.erp.accounting.infrastructure.web.dto.ChargeVentileeDto;
import com.yowyob.erp.accounting.infrastructure.web.dto.LigneConcordanceDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ConcordanceCalculHelper {

    private static final BigDecimal TOLERANCE_ECART = new BigDecimal("1000");

    private ConcordanceCalculHelper() {}

    static List<LigneConcordanceDto> buildLignesAutoFromCharges(List<ChargeVentileeDto> charges, UUID periodeId) {
        List<ChargeVentileeDto> nonInc = charges.stream()
            .filter(c -> periodeId.equals(c.getPeriodeId()) && !Boolean.TRUE.equals(c.getIncorporable()))
            .toList();
        if (nonInc.isEmpty()) {
            return List.of();
        }

        BigDecimal total = nonInc.stream()
            .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String description = nonInc.stream()
            .map(c -> c.getCompteCG() + " — " + c.getLibelle())
            .reduce((a, b) -> a + " · " + b)
            .orElse("");

        UUID chargeRef = nonInc.get(0).getId();

        return List.of(LigneConcordanceDto.builder()
            .id(UUID.nameUUIDFromBytes(("auto-non-inc-" + periodeId).getBytes()))
            .type("CHARGE_NON_INC")
            .label("Charges non incorporables (auto)")
            .description(description)
            .signe("+")
            .montant(total)
            .chargeVentileeId(chargeRef)
            .autoGeneree(true)
            .build());
    }

    static List<LigneConcordanceDto> mergeLignes(
        List<LigneConcordanceDto> manuelles,
        List<LigneConcordanceDto> auto) {
        Set<UUID> autoChargeIds = new HashSet<>();
        for (LigneConcordanceDto l : auto) {
            if (l.getChargeVentileeId() != null) {
                autoChargeIds.add(l.getChargeVentileeId());
            }
        }

        List<LigneConcordanceDto> filtered = new ArrayList<>();
        for (LigneConcordanceDto l : manuelles) {
            if (l.getChargeVentileeId() == null || !autoChargeIds.contains(l.getChargeVentileeId())) {
                filtered.add(l);
            }
        }

        BigDecimal manualNonIncTotal = filtered.stream()
            .filter(l -> "CHARGE_NON_INC".equals(l.getType()))
            .map(l -> l.getMontant() != null ? l.getMontant() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LigneConcordanceDto> mergedAuto = manualNonIncTotal.compareTo(BigDecimal.ZERO) > 0
            ? auto.stream().filter(l -> !"CHARGE_NON_INC".equals(l.getType())).toList()
            : auto;

        List<LigneConcordanceDto> result = new ArrayList<>(filtered);
        result.addAll(mergedAuto);
        return result;
    }

    static BigDecimal sommeLignes(List<LigneConcordanceDto> lignes) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LigneConcordanceDto l : lignes) {
            BigDecimal m = l.getMontant() != null ? l.getMontant() : BigDecimal.ZERO;
            if ("+".equals(l.getSigne())) {
                sum = sum.add(m);
            } else {
                sum = sum.subtract(m);
            }
        }
        return sum;
    }

    static BigDecimal sumChargesNonInc(List<ChargeVentileeDto> charges, UUID periodeId) {
        return charges.stream()
            .filter(c -> periodeId.equals(c.getPeriodeId()) && !Boolean.TRUE.equals(c.getIncorporable()))
            .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumChargesIncorporable(List<ChargeVentileeDto> charges, UUID periodeId) {
        return charges.stream()
            .filter(c -> periodeId.equals(c.getPeriodeId()) && Boolean.TRUE.equals(c.getIncorporable()))
            .map(c -> c.getMontantTotal() != null ? c.getMontantTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal sumEcrituresValidees(List<EcritureAnalytique> ecritures, UUID periodeId) {
        return ecritures.stream()
            .filter(e -> "VALIDEE".equals(e.getStatut()) && periodeId.equals(e.getPeriodeId()))
            .map(e -> e.getMontantTotal() != null ? e.getMontantTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static boolean isConcordanceOk(BigDecimal ecart) {
        return ecart.abs().compareTo(TOLERANCE_ECART) < 0;
    }

    static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}

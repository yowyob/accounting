package com.yowyob.erp.accounting.domain.port.in;
import java.util.List;

import com.yowyob.erp.accounting.domain.model.Compte;
import com.yowyob.erp.accounting.domain.model.DetailEcriture;
import com.yowyob.erp.accounting.domain.model.JournalAudit;
import com.yowyob.erp.accounting.domain.model.Organization;
import com.yowyob.erp.accounting.infrastructure.web.dto.JournalAuditDto;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Use case port defining the Rapport operations.
 */
public interface RapportUseCase {
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.report.BilanDto> generateBilan(UUID organization_id, String date_debut, String date_fin);
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.report.CompteResultatDto> generateCompteResultat(UUID organization_id, String date_debut, String date_fin);
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.report.CashFlowDto> generateCashFlow(UUID organization_id, String date_debut, String date_fin);
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.report.BalanceAgeeDto> generateBalanceAgee(UUID organization_id, String date_reference_str);
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.report.ExecutiveSummaryDto> generateExecutiveSummary(UUID organization_id, String date_debut, String date_fin);
    Mono<List<com.yowyob.erp.accounting.infrastructure.web.dto.GrandLivreDto>> generateGrandLivre(UUID organization_id, String date_debut, String date_fin);
    Mono<com.yowyob.erp.accounting.infrastructure.web.dto.BalanceDesComptesDto> generateBalanceDesComptes(UUID organization_id, String date_debut, String date_fin);
}

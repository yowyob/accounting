package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.dto.CashRegisterAccountingResponse;
import com.yowyob.erp.accounting.dto.CashRegisterMovementDto;
import com.yowyob.erp.accounting.dto.DetailEcritureDto;
import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import com.yowyob.erp.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashRegisterAccountingService {

        private final EcritureComptableService ecritureService;
        private final CompteRepository compteRepository;
        private final PeriodeComptableService periodeService;
        private final JournalComptableService journalService;

        @Transactional
        public Mono<CashRegisterAccountingResponse> accountMovement(CashRegisterMovementDto movement) {
                log.info("Processing cash movement: id={}, sense={}, amount={}", movement.getId(), movement.getSense(),
                                movement.getAmount());

                if (Boolean.TRUE.equals(movement.getIs_accounted())) {
                        return Mono.error(new IllegalArgumentException("Movement is already accounted"));
                }

                return ReactiveTenantContext.getTenantId()
                                .flatMap(tenantId -> determineAccounts(movement, tenantId)
                                                .flatMap(tuple -> generateEcriture(movement, tuple.getT1(),
                                                                tuple.getT2(), tenantId)));
        }

        private Mono<Tuple2<Compte, Compte>> determineAccounts(CashRegisterMovementDto movement, UUID tenantId) {
                // Resolve Emitter Account
                Mono<Compte> emitterAccountMono;
                if (movement.getEmitter_accounting_account() != null
                                && !movement.getEmitter_accounting_account().isEmpty()) {
                        emitterAccountMono = compteRepository.findByTenant_IdAndNo_compte(tenantId,
                                        movement.getEmitter_accounting_account())
                                        .switchIfEmpty(Mono.error(new BusinessException("Emitter account not found: "
                                                        + movement.getEmitter_accounting_account())));
                } else {
                        emitterAccountMono = Mono
                                        .error(new BusinessException("Emitter accounting account is required"));
                }

                // Resolve Recipient Account
                Mono<Compte> recipientAccountMono;
                if (movement.getRecipient_accounting_account() != null
                                && !movement.getRecipient_accounting_account().isEmpty()) {
                        recipientAccountMono = compteRepository.findByTenant_IdAndNo_compte(tenantId,
                                        movement.getRecipient_accounting_account())
                                        .switchIfEmpty(Mono.error(new BusinessException("Recipient account not found: "
                                                        + movement.getRecipient_accounting_account())));
                } else {
                        recipientAccountMono = Mono
                                        .error(new BusinessException("Recipient accounting account is required"));
                }

                return Mono.zip(emitterAccountMono, recipientAccountMono);
        }

        private Mono<CashRegisterAccountingResponse> generateEcriture(CashRegisterMovementDto movement,
                        Compte emitterCompte, Compte recipientCompte, UUID tenantId) {
                return periodeService.getCurrentPeriode(tenantId)
                                .flatMap(periode -> {
                                        // Find a default journal for cash movements (looking for "Caisse" or "OD"
                                        // journal)
                                        return journalService.getAllJournaux()
                                                        .flatMap(journals -> {
                                                                UUID journalId = journals.isEmpty() ? null
                                                                                : journals.get(0).getId();
                                                                if (journalId == null) {
                                                                        return Mono.error(new BusinessException(
                                                                                        "No journal found for cash movements"));
                                                                }

                                                                EcritureComptableDto ecritureDto = EcritureComptableDto
                                                                                .builder()
                                                                                .libelle("Mvt Caisse: " + (movement
                                                                                                .getReason() != null
                                                                                                                ? movement.getReason()
                                                                                                                : "N/A"))
                                                                                .date_ecriture(movement.getCreate_on()
                                                                                                .toLocalDate())
                                                                                .journal_comptable_id(journalId)
                                                                                .periode_comptable_id(periode.getId())
                                                                                .montant_total_debit(
                                                                                                movement.getAmount())
                                                                                .montant_total_credit(
                                                                                                movement.getAmount())
                                                                                .validee(false)
                                                                                .reference_externe(movement.getId())
                                                                                .build();

                                                                // Recipient Account is always DEBITED (receives money)
                                                                // Emitter Account is always CREDITED (gives money)
                                                                DetailEcritureDto lineDebit = DetailEcritureDto
                                                                                .builder()
                                                                                .compte_comptable_id(
                                                                                                recipientCompte.getId())
                                                                                .libelle(movement.getReason() != null
                                                                                                ? movement.getReason()
                                                                                                : "Mouvement caisse")
                                                                                .sens("DEBIT")
                                                                                .montant_debit(movement.getAmount())
                                                                                .montant_credit(BigDecimal.ZERO)
                                                                                .build();

                                                                DetailEcritureDto lineCredit = DetailEcritureDto
                                                                                .builder()
                                                                                .compte_comptable_id(
                                                                                                emitterCompte.getId())
                                                                                .libelle(movement.getReason() != null
                                                                                                ? movement.getReason()
                                                                                                : "Mouvement caisse")
                                                                                .sens("CREDIT")
                                                                                .montant_debit(BigDecimal.ZERO)
                                                                                .montant_credit(movement.getAmount())
                                                                                .build();

                                                                ecritureDto.setDetails_ecriture(
                                                                                Arrays.asList(lineDebit, lineCredit));

                                                                return ecritureService.createEcriture(ecritureDto)
                                                                                .map(saved -> CashRegisterAccountingResponse
                                                                                                .builder()
                                                                                                .movement_id(UUID
                                                                                                                .fromString(movement
                                                                                                                                .getId()))
                                                                                                .status("ACCOUNTED")
                                                                                                .ecriture_id(saved
                                                                                                                .getId())
                                                                                                .build());
                                                        });
                                });
        }
}

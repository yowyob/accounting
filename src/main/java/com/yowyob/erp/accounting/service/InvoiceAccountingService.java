package com.yowyob.erp.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.erp.accounting.dto.BrouillardComptableDto;
import com.yowyob.erp.accounting.dto.DetailEcritureDto;
import com.yowyob.erp.accounting.dto.EcritureComptableDto;
import com.yowyob.erp.accounting.dto.invoice.CustomerInvoiceDto;
import com.yowyob.erp.accounting.dto.invoice.SupplierInvoiceDto;
import com.yowyob.erp.accounting.entity.BrouillardType;
import com.yowyob.erp.accounting.entity.Compte;
import com.yowyob.erp.accounting.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.repository.PeriodeComptableRepository;
import com.yowyob.erp.accounting.repository.CompteRepository;
import com.yowyob.erp.common.constants.AppConstants;
import com.yowyob.erp.common.exception.BusinessException;
import com.yowyob.erp.config.tenant.ReactiveTenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to handle accounting entries generation for invoices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceAccountingService {

    private final EcritureComptableService ecriture_service;
    private final CompteService compte_service;
    private final CompteRepository compte_repository;
    private final JournalComptableRepository journal_repository;
    private final PeriodeComptableRepository periode_repository;
    private final BrouillardComptableService brouillard_service;
    private final ObjectMapper objectMapper;

    /**
     * Accounts a supplier invoice.
     * Returns either EcritureComptableDto (if auto) or BrouillardComptableDto (if draft).
     */
    @Transactional
    public Mono<Object> accountSupplierInvoice(SupplierInvoiceDto invoice) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> {
                    log.info("Processing supplier invoice: {} for tenant: {}", invoice.getNumeroFacture(), tenant_id);
                    
                    // 1. Resolve Journal to check settings
                    return findJournal(tenant_id, AppConstants.JournalTypes.PURCHASES)
                            .flatMap(journal -> {
                                // 2. Check if we should create a draft
                                return brouillard_service.shouldCreateDraft(tenant_id, BrouillardType.FACTURE_FOURNISSEUR, 
                                        invoice.getMontantTTC(), journal.getId())
                                        .flatMap(shouldDraft -> {
                                            if (shouldDraft) {
                                                LocalDate date = invoice.getDateFacturation() != null ? invoice.getDateFacturation() : LocalDate.now();
                                                return findPeriode(tenant_id, date)
                                                    .flatMap(periode -> brouillard_service.createDraft(tenant_id, 
                                                            BrouillardType.FACTURE_FOURNISSEUR, 
                                                            invoice, 
                                                            invoice.getIdFacture().toString(), 
                                                            "SUPPLIER_INVOICE", 
                                                            invoice.getNumeroFacture(), 
                                                            date, 
                                                            "Facture Fournisseur " + invoice.getNumeroFacture(), 
                                                            invoice.getMontantTTC(), 
                                                            invoice.getDevise(), 
                                                            journal.getId(), 
                                                            periode.getId(), 
                                                            "system",
                                                            objectMapper.valueToTree(invoice.getAttachmentIds()))
                                                    .map(this::mapToDto)); // We need a mapper from Entity to DTO for Brouillard
                                            } else {
                                                // Create direct entry (existing logic)
                                                return processDirectSupplierInvoice(tenant_id, invoice, journal);
                                            }
                                        });
                            });
                });
    }

    private BrouillardComptableDto mapToDto(com.yowyob.erp.accounting.entity.BrouillardComptable entity) {
        return BrouillardComptableDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .statut(entity.getStatut())
                .montantTotal(entity.getMontantTotal())
                .libelle(entity.getLibelle())
                .createdAt(entity.getCreatedAt())
                .build(); // Add other fields as needed
    }

    /**
     * Creates a direct accounting entry for a supplier invoice, bypassing draft checks.
     * Used for draft validation.
     */
    @Transactional
    public Mono<EcritureComptableDto> createDirectSupplierInvoiceEntry(SupplierInvoiceDto invoice) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> findJournal(tenant_id, AppConstants.JournalTypes.PURCHASES)
                        .flatMap(journal -> processDirectSupplierInvoice(tenant_id, invoice, journal)));
    }

    /**
     * Creates a direct accounting entry for a customer invoice, bypassing draft checks.
     * Used for draft validation.
     */
    /**
     * Creates a direct accounting entry for a customer invoice, bypassing draft checks.
     * Used for draft validation.
     */
    @Transactional
    public Mono<EcritureComptableDto> createDirectCustomerInvoiceEntry(CustomerInvoiceDto invoice) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> findJournal(tenant_id, AppConstants.JournalTypes.SALES)
                        .flatMap(journal -> processDirectCustomerInvoice(tenant_id, invoice, journal)));
    }

    private Mono<EcritureComptableDto> processDirectSupplierInvoice(UUID tenant_id, SupplierInvoiceDto invoice, com.yowyob.erp.accounting.entity.JournalComptable journal) {
        return findOrCreateAccount(tenant_id, invoice.getIdFournisseur(), invoice.getNomFournisseru(), "SUPPLIER")
                .flatMap(supplier_account -> {
                    LocalDate date = invoice.getDateFacturation() != null ? invoice.getDateFacturation() : LocalDate.now();
                    return findPeriode(tenant_id, date)
                        .flatMap(periode -> {

                            List<DetailEcritureDto> details = new ArrayList<>();
                            String libelle = "Facture Fournisseur "
                                    + (invoice.getNumeroFacture() != null
                                            ? invoice.getNumeroFacture()
                                            : invoice.getIdFacture());

                            // 1. Debit Expense/Purchase account (HT)
                            details.add(DetailEcritureDto.builder()
                                    .libelle(libelle)
                                    .sens(AppConstants.AccountingSense.DEBIT)
                                    .montant_debit(invoice.getMontantHT())
                                    .montant_credit(BigDecimal.ZERO)
                                    .build());

                            // 2. Debit VAT account (if applicable)
                            if (invoice.getMontantTVA() != null
                                    && invoice.getMontantTVA().compareTo(BigDecimal.ZERO) > 0) {
                                details.add(DetailEcritureDto.builder()
                                        .libelle("TVA sur Achats - "
                                                + (invoice.getNumeroFacture() != null
                                                        ? invoice.getNumeroFacture()
                                                        : invoice.getIdFacture()))
                                        .sens(AppConstants.AccountingSense.DEBIT)
                                        .montant_debit(invoice.getMontantTVA())
                                        .montant_credit(BigDecimal.ZERO)
                                        .build());
                            }

                            // 3. Credit Supplier account (TTC)
                            details.add(DetailEcritureDto.builder()
                                    .compte_comptable_id(supplier_account.getId())
                                    .libelle(libelle)
                                    .sens(AppConstants.AccountingSense.CREDIT)
                                    .montant_debit(BigDecimal.ZERO)
                                    .montant_credit(invoice.getMontantTTC())
                                    .build());

                            return resolveAccountIds(tenant_id, details, "PURCHASE")
                                    .flatMap(finalDetails -> {
                                        EcritureComptableDto ecritureDto = EcritureComptableDto
                                                .builder()
                                                .libelle(libelle)
                                                .date_ecriture(date)
                                                .journal_comptable_id(journal.getId())
                                                .periode_comptable_id(periode.getId())
                                                .montant_total_debit(invoice.getMontantTTC())
                                                .montant_total_credit(invoice.getMontantTTC())
                                                .reference_externe(invoice.getNumeroFacture())
                                                .attachment_ids(objectMapper.valueToTree(invoice.getAttachmentIds()))
                                                .details_ecriture(finalDetails)
                                                .build();

                                        return ecriture_service.createEcriture(ecritureDto);
                                    });
                        });
                });
    }

    /**
     * Accounts a customer invoice.
     */
    @Transactional
    public Mono<Object> accountCustomerInvoice(CustomerInvoiceDto invoice) {
        return ReactiveTenantContext.getTenantId()
                .flatMap(tenant_id -> {
                    log.info("Processing customer invoice: {} for tenant: {}", invoice.getNumeroFacture(), tenant_id);
                    
                    return findJournal(tenant_id, AppConstants.JournalTypes.SALES)
                            .flatMap(journal -> {
                                return brouillard_service.shouldCreateDraft(tenant_id, BrouillardType.FACTURE_CLIENT, 
                                        invoice.getMontantTTC(), journal.getId())
                                        .flatMap(shouldDraft -> {
                                            if (shouldDraft) {
                                                LocalDate date = invoice.getDateFacturation() != null ? invoice.getDateFacturation().toLocalDate() : LocalDate.now();
                                                return findPeriode(tenant_id, date)
                                                    .flatMap(periode -> brouillard_service.createDraft(tenant_id, 
                                                            BrouillardType.FACTURE_CLIENT, 
                                                            invoice, 
                                                            invoice.getIdFacture().toString(), 
                                                            "CUSTOMER_INVOICE", 
                                                            invoice.getNumeroFacture(), 
                                                            date, 
                                                            "Facture Client " + invoice.getNumeroFacture(), 
                                                            invoice.getMontantTTC(), 
                                                            invoice.getDevise(), 
                                                            journal.getId(), 
                                                            periode.getId(), 
                                                            "system",
                                                            objectMapper.valueToTree(invoice.getAttachmentIds()))
                                                    .map(this::mapToDto));
                                            } else {
                                                return processDirectCustomerInvoice(tenant_id, invoice, journal);
                                            }
                                        });
                            });
                });
    }

    private Mono<EcritureComptableDto> processDirectCustomerInvoice(UUID tenant_id, CustomerInvoiceDto invoice, com.yowyob.erp.accounting.entity.JournalComptable journal) {
        UUID clientId = null;
        try {
            if (invoice.getIdClient() != null) {
                clientId = UUID.fromString(invoice.getIdClient());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for client ID: {}", invoice.getIdClient());
        }

        return findOrCreateAccount(tenant_id, clientId, invoice.getNomClient(), "CLIENT")
                .flatMap(client_account -> {
                            LocalDate date_facture = invoice.getDateFacturation() != null
                                    ? invoice.getDateFacturation().toLocalDate()
                                    : LocalDate.now();

                            return findPeriode(tenant_id, date_facture)
                                    .flatMap(periode -> {

                                        List<DetailEcritureDto> details = new ArrayList<>();
                                        String libelle = "Facture Client "
                                                + (invoice.getNumeroFacture() != null
                                                        ? invoice.getNumeroFacture()
                                                        : invoice.getIdFacture());

                                        // 1. Debit Client account (TTC)
                                        details.add(DetailEcritureDto.builder()
                                                .compte_comptable_id(client_account.getId())
                                                .libelle(libelle)
                                                .sens(AppConstants.AccountingSense.DEBIT)
                                                .montant_debit(invoice.getMontantTTC())
                                                .montant_credit(BigDecimal.ZERO)
                                                .build());

                                        // 2. Credit Sales account (HT)
                                        details.add(DetailEcritureDto.builder()
                                                .libelle(libelle)
                                                .sens(AppConstants.AccountingSense.CREDIT)
                                                .montant_debit(BigDecimal.ZERO)
                                                .montant_credit(invoice.getMontantHT())
                                                .build());

                                        // 3. Credit VAT account (if applicable)
                                        if (invoice.getMontantTVA() != null
                                                && invoice.getMontantTVA().compareTo(BigDecimal.ZERO) > 0) {
                                            details.add(DetailEcritureDto.builder()
                                                    .libelle("TVA collectée - "
                                                            + (invoice.getNumeroFacture() != null
                                                                    ? invoice.getNumeroFacture()
                                                                    : invoice.getIdFacture()))
                                                    .sens(AppConstants.AccountingSense.CREDIT)
                                                    .montant_debit(BigDecimal.ZERO)
                                                    .montant_credit(invoice.getMontantTVA())
                                                    .build());
                                        }

                                        return resolveAccountIds(tenant_id, details, "SALE")
                                                .flatMap(finalDetails -> {
                                                    EcritureComptableDto ecritureDto = EcritureComptableDto
                                                            .builder()
                                                            .libelle(libelle)
                                                            .date_ecriture(date_facture)
                                                            .journal_comptable_id(journal.getId())
                                                            .periode_comptable_id(periode.getId())
                                                            .montant_total_debit(invoice.getMontantTTC())
                                                            .montant_total_credit(invoice.getMontantTTC())
                                                            .reference_externe(invoice.getNumeroFacture())
                                                            .attachment_ids(objectMapper.valueToTree(invoice.getAttachmentIds()))
                                                            .details_ecriture(finalDetails)
                                                            .build();

                                                    return ecriture_service.createEcriture(ecritureDto);
                                                });
                                    });
                        });
    }

    private Mono<Compte> findOrCreateAccount(UUID tenant_id, UUID external_id, String name, String type) {
        if (external_id == null) {
            return Mono.error(new BusinessException(
                    "External ID is required to find or create the accounting account for the " + type));
        }

        return compte_repository.findByTenant_IdAndExternal_id(tenant_id, external_id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Account not found for {} with external ID {}, creating auto-generated account...", type,
                            external_id);
                    return compte_service
                            .createAutoGeneratedAccount(name, type, "Auto-created from invoice integration",
                                    external_id)
                            .flatMap(dto -> compte_repository.findById(dto.getId()));
                }));
    }

    private Mono<com.yowyob.erp.accounting.entity.JournalComptable> findJournal(UUID tenant_id, String type) {
        return journal_repository.findByTenant_IdAndType_journal(tenant_id, type)
                .filter(j -> Boolean.TRUE.equals(j.getActif()))
                .next()
                .switchIfEmpty(Mono
                        .error(new BusinessException("No active journal of type " + type + " found for this tenant")));
    }

    private Mono<com.yowyob.erp.accounting.entity.PeriodeComptable> findPeriode(UUID tenant_id, LocalDate date) {
        return periode_repository.findByTenant_IdAndDateInRange(tenant_id, date)
                .switchIfEmpty(
                        Mono.error(new BusinessException("No open accounting period found for the date " + date)));
    }

    private Mono<List<DetailEcritureDto>> resolveAccountIds(UUID tenant_id, List<DetailEcritureDto> details,
            String context) {
        List<Mono<DetailEcritureDto>> monos = details.stream().map(d -> {
            if (d.getCompte_comptable_id() == null) {
                String prefix;
                String type;
                String name;

                if (d.getLibelle().toLowerCase().contains("tva")) {
                    prefix = context.equals("SALE") ? AppConstants.AccountCodes.VAT_ACCOUNT_PREFIX : "445";
                    type = context.equals("SALE") ? "VAT_COLLECTED" : "VAT_DEDUCTIBLE";
                    name = context.equals("SALE") ? "TVA Collectée" : "TVA Déductible";
                } else {
                    prefix = context.equals("SALE") ? AppConstants.AccountCodes.SALES_ACCOUNT_PREFIX : "601";
                    type = context.equals("SALE") ? "SALES" : "PURCHASE";
                    name = context.equals("SALE") ? "Ventes de Marchandises" : "Achats de Marchandises";
                }

                return findOrCreateStandardAccount(tenant_id, prefix, type, name)
                        .map(c -> {
                            d.setCompte_comptable_id(c.getId());
                            return d;
                        });
            } else {
                return Mono.just(d);
            }
        }).collect(Collectors.toList());

        return Mono.zip(monos, objects -> {
            List<DetailEcritureDto> result = new ArrayList<>();
            for (Object o : objects) {
                result.add((DetailEcritureDto) o);
            }
            return result;
        });
    }

    private Mono<Compte> findOrCreateStandardAccount(UUID tenant_id, String prefix, String type, String name) {
        return compte_repository.findByTenant_IdAndNo_compteStartingWith(tenant_id, prefix)
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    log.info(
                            "Standard account with prefix {} not found for tenant {}, creating auto-generated account of type {}...",
                            prefix, tenant_id, type);
                    return compte_service.createAutoGeneratedAccount(name, type, "Auto-created standard account", null)
                            .flatMap(dto -> compte_repository.findById(dto.getId()));
                }));
    }
}



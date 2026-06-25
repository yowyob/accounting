package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.domain.model.AccountingSubscription;
import com.yowyob.erp.accounting.domain.port.in.AccountingSubscriptionUseCase;
import com.yowyob.erp.accounting.infrastructure.web.dto.AccountingSubscriptionDto;
import com.yowyob.erp.config.auth.AccountingAuthorities;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Gère l'abonnement d'une organisation aux activités comptables (Générale / Analytique).
 *
 * <p>L'organisation est résolue depuis le contexte de requête ({@code X-Organization-Id}).
 * La lecture est ouverte à tous les rôles comptables ; la modification est réservée au
 * responsable comptable / administrateur (entitlement au niveau organisation).</p>
 */
@RestController
@RequestMapping("/api/accounting/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounting Subscriptions", description = "Abonnement de l'organisation aux activités comptables")
public class AccountingSubscriptionController {

    private final AccountingSubscriptionUseCase subscriptionService;

    @GetMapping
    @Operation(summary = "Abonnement aux activités comptables de l'organisation courante")
    @PreAuthorize(AccountingAuthorities.READ)
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSubscriptionDto>>> getSubscription() {
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(subscriptionService::getForOrganization)
                .map(this::mapToDto)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Abonnement récupéré.")));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour l'abonnement aux activités comptables de l'organisation courante")
    @PreAuthorize(AccountingAuthorities.SUPERVISE)
    public Mono<ResponseEntity<ApiResponseWrapper<AccountingSubscriptionDto>>> updateSubscription(
            @RequestBody AccountingSubscriptionDto request) {
        return Mono.zip(
                        ReactiveOrganizationContext.getOrganizationId(),
                        ReactiveOrganizationContext.getCurrentUser())
                .flatMap(tuple -> subscriptionService.updateForOrganization(
                        tuple.getT1(), request.isGenerale(), request.isAnalytique(), tuple.getT2()))
                .map(this::mapToDto)
                .map(dto -> ResponseEntity.ok(ApiResponseWrapper.success(dto, "Abonnement mis à jour.")));
    }

    private AccountingSubscriptionDto mapToDto(AccountingSubscription entity) {
        return AccountingSubscriptionDto.builder()
                .id(entity.getId())
                .generale(Boolean.TRUE.equals(entity.getGeneraleActive()))
                .analytique(Boolean.TRUE.equals(entity.getAnalytiqueActive()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

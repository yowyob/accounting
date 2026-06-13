package com.yowyob.erp.accounting.infrastructure.web.controller;

import com.yowyob.erp.accounting.domain.port.in.ClotureAnnuelleUseCase;
import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.yowyob.erp.config.auth.AccountingAuthorities;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/comptable/cloture")
@RequiredArgsConstructor
@Tag(name = "Clôture Annuelle", description = "Clôture formelle de fin d'exercice : transfert du résultat (131/139) et génération des À-nouveaux")
@SecurityRequirement(name = "BasicAuth")
public class ClotureAnnuelleController {

    private final ClotureAnnuelleUseCase cloture_annuelle_service;

    @PostMapping("/annuelle/{exerciceId}")
    @PreAuthorize(AccountingAuthorities.SUPERVISE)
    @Operation(summary = "Exécuter la clôture annuelle d'un exercice comptable",
        description = """
            Processus complet OHADA de clôture de fin d'exercice :
            1. Vérifie que toutes les périodes mensuelles sont clôturées
            2. Calcule le résultat net (produits - charges, classes 6 et 7)
            3. Transfère le résultat vers 131000 (bénéfice) ou 139000 (perte)
            4. Solde tous les comptes de gestion (6xx et 7xx)
            5. Génère les écritures d'À-nouveaux (AN) pour l'exercice suivant
            """)
    public Mono<ResponseEntity<ApiResponseWrapper<Void>>> executerClotureAnnuelle(
            @PathVariable UUID exerciceId) {
        return cloture_annuelle_service.executerCloture(exerciceId)
            .then(Mono.just(ResponseEntity.ok(
                ApiResponseWrapper.<Void>success(null,
                    "Clôture annuelle effectuée — résultat transféré et À-nouveaux générés"))));
    }
}

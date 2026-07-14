package com.yowyob.erp.legal;

import com.yowyob.erp.shared.infrastructure.dto.ApiResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Point d'accès des plateformes clientes aux documents légaux.
 *
 * <p>Le frontend accounting appelle {@code GET /api/legal-documents/{slug}} ; le backend relaie vers
 * le kernel (via {@link LegalDocumentService}) et met en cache. Les documents ne se modifient que
 * dans le kernel. Endpoint public (voir {@code SecurityConfig}) : ce sont des textes légaux
 * destinés à être lus sans authentification utilisateur.</p>
 */
@RestController
@RequestMapping("/api/legal-documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    public LegalDocumentController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponseWrapper<List<LegalDocumentDto>>>> list() {
        return legalDocumentService.list()
                .map(documents -> ResponseEntity.ok(ApiResponseWrapper.success(documents, "Documents légaux récupérés.")));
    }

    @GetMapping("/{slug}")
    public Mono<ResponseEntity<ApiResponseWrapper<LegalDocumentDto>>> get(@PathVariable("slug") String slug) {
        return legalDocumentService.get(slug)
                .map(document -> ResponseEntity.ok(ApiResponseWrapper.success(document, "Document légal récupéré.")));
    }

    @ExceptionHandler(LegalDocumentNotFoundException.class)
    public ResponseEntity<ApiResponseWrapper<Void>> handleNotFound(LegalDocumentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseWrapper.error(exception.getMessage()));
    }
}

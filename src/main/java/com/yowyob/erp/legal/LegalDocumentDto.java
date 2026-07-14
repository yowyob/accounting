package com.yowyob.erp.legal;

/**
 * Vue applatie d'un document légal renvoyée au frontend.
 *
 * <p>Le backend ne fait que relayer (et mettre en cache) ce que le kernel expose ; le kernel reste
 * l'unique source de vérité. Le {@code content} est le texte brut consommé par le parser du
 * frontend.</p>
 */
public record LegalDocumentDto(
        String slug,
        String locale,
        String title,
        String version,
        String content,
        String updatedAt) {
}

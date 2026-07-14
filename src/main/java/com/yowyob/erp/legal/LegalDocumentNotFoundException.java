package com.yowyob.erp.legal;

/** Levée lorsque le kernel ne connaît pas le slug de document légal demandé. */
public class LegalDocumentNotFoundException extends RuntimeException {

    public LegalDocumentNotFoundException(String slug) {
        super("Aucun document légal pour le slug: " + slug);
    }
}

package com.yowyob.erp.accounting.application.service;

import com.yowyob.erp.accounting.domain.model.FactureComptable;
import com.yowyob.erp.accounting.domain.model.JournalComptable;
import com.yowyob.erp.accounting.domain.port.in.EcritureComptableUseCase;
import com.yowyob.erp.accounting.infrastructure.persistence.repository.JournalComptableRepository;
import com.yowyob.erp.accounting.infrastructure.web.dto.EcritureComptableDto;
import com.yowyob.erp.config.organization.ReactiveOrganizationContext;
import com.yowyob.erp.shared.domain.constants.AppConstants;
import com.yowyob.erp.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing invoice documents (PDF/Images) using OCR.
 * Extracts structured data from raw documents.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactureProcessingService {

    @Value("${invoice.ocr.enabled:true}")
    private boolean ocr_enabled;

    @Value("${invoice.ocr.tessdataPath:/usr/share/tessdata}")
    private String tessdata_path;

    @Value("${invoice.ocr.lang:fra}")
    private String ocr_lang;

    private final InvoiceTextParser parser;
    private final EcritureComptableUseCase ecriture_service;
    private final JournalComptableRepository journal_repository;

    /**
     * Full pipeline: extracts invoice data via OCR, resolves the appropriate
     * accounting journal (ACHAT/VENTE), then generates and persists the OHADA
     * accounting entry.
     *
     * @param filePart the uploaded file (PDF or image)
     * @return a Mono with the generated accounting entry
     */
    public Mono<EcritureComptableDto> extractAndComptabiliser(FilePart filePart) {
        return extractFactureData(filePart)
                .flatMap(facture -> resolveJournalId(facture.is_achat())
                        .map(journal_id -> {
                            facture.setJournal_comptable_id(journal_id);
                            return facture;
                        }))
                .flatMap(ecriture_service::generateFromComptableObject)
                .doOnSuccess(ecriture -> log.info("✅ Invoice recorded — accounting entry {} generated",
                        ecriture.getNumero_ecriture()));
    }

    /**
     * Resolves the active accounting journal matching the invoice nature
     * (purchase → ACHAT journal, sale → VENTE journal) for the current
     * organization.
     *
     * @param is_achat true for a purchase invoice, false for a sale
     * @return a Mono with the resolved journal id
     */
    private Mono<UUID> resolveJournalId(boolean is_achat) {
        String type = is_achat ? AppConstants.JournalTypes.PURCHASES : AppConstants.JournalTypes.SALES;
        return ReactiveOrganizationContext.getOrganizationId()
                .flatMap(organization_id -> journal_repository
                        .findByOrganization_IdAndType_journal(organization_id, type)
                        .filter(j -> Boolean.TRUE.equals(j.getActif()))
                        .next()
                        .map(JournalComptable::getId)
                        .switchIfEmpty(Mono.error(new BusinessException(
                                "No active '" + type
                                        + "' journal found for this organization. Please initialize accounting journals first."))));
    }

    /**
     * /**
     * Main entry point: extracts and returns a reconstructed FactureComptable from
     * the provided file.
     *
     * @param filePart the uploaded file (PDF or image)
     * @return a Mono<FactureComptable> object
     * @throws RuntimeException if extraction fails
     */
    public Mono<FactureComptable> extractFactureData(FilePart filePart) {
        return extractText(filePart)
                .map(text -> {
                    if (text == null || text.trim().isEmpty()) {
                        throw new IllegalStateException("Unable to extract text from the invoice");
                    }

                    log.debug("Extracted text (preview):\n{}", preview(text, 1200));

                    try {
                        // 1) Parse key information
                        var infos = parser.parse(text);

                        // Fetch accounting period ID
                        UUID periode_id = Optional.ofNullable(infos.periode_comptable_id()).orElse(null);

                        // If HT amount is null, throw exception
                        if (infos.montant_ht() == null) {
                            throw new IllegalStateException(
                                    "Unable to parse a valid Montant HT from the invoice text.");
                        }

                        // 2) Build business object
                        FactureComptable facture = new FactureComptable(
                                Optional.ofNullable(infos.id()).orElse(UUID.randomUUID()),
                                infos.montant_ht(),
                                Optional.ofNullable(infos.date()).orElse(LocalDate.now()),
                                Optional.ofNullable(infos.libelle()).orElse("Imported Invoice"),
                                infos.journal_comptable_id(),
                                periode_id,
                                infos.client_id(),
                                Boolean.TRUE.equals(infos.is_achat()));

                        // Convert double TVA rate to BigDecimal
                        facture.setTaux_tva(
                                infos.taux_tva() != null
                                        ? BigDecimal.valueOf(infos.taux_tva())
                                        : BigDecimal.valueOf(0.18));

                        return facture;
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        throw e;
                    } catch (Exception e) {
                        log.error("Error during invoice extraction: {}", e.getMessage(), e);
                        throw new RuntimeException("Error extracting invoice: " + e.getMessage());
                    }
                });
    }

    /**
     * Extracts text from the file: PDF (text) → PDF (images via OCR) → image (OCR).
     *
     * @param filePart the uploaded filePart
     * @return a Mono containing extracted text
     */
    private Mono<String> extractText(FilePart filePart) {
        String content_type = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : guessContentType(filePart.filename());

        File tempFile;
        try {
            tempFile = File.createTempFile("ocr_temp_", "_" + filePart.filename());
        } catch (IOException e) {
            return Mono.error(new RuntimeException("Failed to create temporary file for OCR", e));
        }

        return filePart.transferTo(tempFile).then(Mono.fromCallable(() -> {
            try {
                if (tempFile.length() > 10 * 1024 * 1024) { // 10MB limit
                    throw new IllegalArgumentException("File size exceeds 10MB limit");
                }

                if (MediaType.APPLICATION_PDF_VALUE.equals(content_type)) {
                    String pdf_text = extractTextFromPDF(tempFile);
                    if (isBlank(pdf_text) && ocr_enabled) {
                        return ocrFromPdf(tempFile);
                    }
                    return pdf_text;
                }

                if (ocr_enabled && content_type.startsWith("image/")) {
                    return ocrFromImage(tempFile);
                }

                try {
                    String pdf_text = extractTextFromPDF(tempFile);
                    if (!isBlank(pdf_text))
                        return pdf_text;
                } catch (Exception ignore) {
                }

                if (ocr_enabled) {
                    return ocrFromImage(tempFile);
                }

                return null;
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }));
    }

    private String extractTextFromPDF(File file) throws IOException {
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String ocrFromPdf(File file) throws IOException, TesseractException {
        try (PDDocument doc = PDDocument.load(file)) {
            if (!new File(tessdata_path).exists()) {
                throw new IllegalStateException("Tesseract data path not found: " + tessdata_path);
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage page_image = renderer.renderImageWithDPI(i, 300);
                sb.append(runOcr(page_image)).append('\n');
            }
            return sb.toString();
        }
    }

    private String ocrFromImage(File file) throws IOException, TesseractException {
        BufferedImage img = ImageIO.read(file);
        return runOcr(img);
    }

    private String runOcr(BufferedImage img) throws TesseractException {
        Tesseract t = new Tesseract();
        t.setDatapath(tessdata_path);
        t.setLanguage(ocr_lang);
        return t.doOCR(img);
    }

    private String preview(String s, int max) {
        if (s == null)
            return "";
        return s.length() > max ? s.substring(0, max) + " ..." : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String guessContentType(String filename) {
        String name = Optional.ofNullable(filename).orElse("").toLowerCase();
        if (name.endsWith(".pdf"))
            return MediaType.APPLICATION_PDF_VALUE;
        if (name.matches(".*\\.(png|jpg|jpeg|tif|tiff|bmp|webp)$"))
            return "image/*";
        return "application/octet-stream";
    }
}
package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.entity.FactureComptable;
import com.yowyob.erp.common.entity.ComptableObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactureProcessingService {

    @Value("${invoice.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Value("${invoice.ocr.tessdataPath:/usr/share/tessdata}")
    private String tessdataPath;

    @Value("${invoice.ocr.lang:fra}")
    private String ocrLang;

    private final InvoiceTextParser parser;

        /**
     * Main entry point: extracts and returns a reconstructed FactureComptable from the provided file.
     *
     * @param file the uploaded file (PDF or image)
     * @return a FactureComptable object
     * @throws RuntimeException if extraction fails
     * @author ALD
     * @date 12/10/2025 07:51 AM WAT
     */
    public FactureComptable extractFactureData(MultipartFile file) {
        try {
            String text = extractText(file);
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalStateException("Unable to extract text from the invoice");
            }

            log.debug("Extracted text (preview):\n{}", preview(text, 1200));

            // 1) Parse key information
            var infos = parser.parse(text);

            // Gère l'ID de la période comptable (MANQUANT AUPARAVANT, maintenant requis par le constructeur)
            UUID periodeId = Optional.ofNullable(infos.periodeComptableId()).orElse(null);
            
            // Si le montant HT est nul (non trouvé), nous devons lever une exception
            if (infos.montantHT() == null) {
                 throw new IllegalStateException("Unable to parse a valid Montant HT from the invoice text.");
            }

            // 2) Build business object
            FactureComptable facture = new FactureComptable(
                    Optional.ofNullable(infos.id()).orElse(UUID.randomUUID()),
                    infos.montantHT(), // Est déjà BigDecimal grâce à la mise à jour du Parser
                    Optional.ofNullable(infos.date()).orElse(LocalDate.now()),
                    Optional.ofNullable(infos.libelle()).orElse("Imported Invoice"),
                    infos.journalComptableId(),
                    periodeId, // Argument 6 : periodeComptableId
                    infos.clientId(),
                    Boolean.TRUE.equals(infos.isAchat())
            );
            
            // CORRECTION: Convertit le Double (infos.tauxTVA()) en BigDecimal 
            facture.setTauxTVA(
                    infos.tauxTVA() != null 
                        ? BigDecimal.valueOf(infos.tauxTVA()) 
                        : BigDecimal.valueOf(0.18) // Conversion du littéral double en BigDecimal
            );

            return facture;

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (TesseractException e) {
            log.error("OCR processing error: {}", e.getMessage(), e);
            throw new RuntimeException("OCR processing failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during invoice extraction: {}", e.getMessage(), e);
            throw new RuntimeException("Error extracting invoice: " + e.getMessage());
        }
    }


    /**
     * Extracts text from the file: PDF (text) → PDF (images via OCR) → image (OCR).
     *
     * @param file the uploaded file
     * @return extracted text or null if extraction fails
     * @throws Exception if processing fails
     */
    private String extractText(MultipartFile file) throws Exception {
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null) contentType = guessContentType(file);

        if (MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
            String pdfText = extractTextFromPDF(file);
            if (isBlank(pdfText) && ocrEnabled) {
                return ocrFromPdf(file);
            }
            return pdfText;
        }

        if (ocrEnabled && contentType.startsWith("image/")) {
            return ocrFromImage(file);
        }

        try {
            String pdfText = extractTextFromPDF(file);
            if (!isBlank(pdfText)) return pdfText;
        } catch (Exception ignore) {}

        if (ocrEnabled) {
            return ocrFromImage(file);
        }

        return null;
    }

    private String extractTextFromPDF(MultipartFile file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String ocrFromPdf(MultipartFile file) throws IOException, TesseractException {
        try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
            if (!new File(tessdataPath).exists()) {
                throw new IllegalStateException("Tesseract data path not found: " + tessdataPath);
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);
                sb.append(runOcr(pageImage)).append('\n');
            }
            return sb.toString();
        }
    }

    private String ocrFromImage(MultipartFile file) throws IOException, TesseractException {
        BufferedImage img = ImageIO.read(file.getInputStream());
        return runOcr(img);
    }

    private String runOcr(BufferedImage img) throws TesseractException {
        Tesseract t = new Tesseract();
        t.setDatapath(tessdataPath);
        t.setLanguage(ocrLang);
        return t.doOCR(img);
    }

    private String preview(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + " ..." : s;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String guessContentType(MultipartFile file) throws IOException {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        if (name.endsWith(".pdf")) return MediaType.APPLICATION_PDF_VALUE;
        if (name.matches(".*\\.(png|jpg|jpeg|tif|tiff|bmp|webp)$")) return "image/*";
        return "application/octet-stream";
    }
}
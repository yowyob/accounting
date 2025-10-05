package com.yowyob.erp.accounting.controller;

import com.yowyob.erp.accounting.dto.JournalComptableDto;
import com.yowyob.erp.accounting.service.JournalComptableService;
import com.yowyob.erp.common.dto.ApiResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/journals")
public class JournalComptableController {

    private static final Logger logger = LoggerFactory.getLogger(JournalComptableController.class);
    private final JournalComptableService journalComptableService;

    @Autowired
    public JournalComptableController(JournalComptableService journalComptableService) {
        this.journalComptableService = journalComptableService;
    }

    // Create a new journal comptable
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> createJournalComptable(@RequestBody JournalComptableDto journalComptableDto) {
        logger.info("Requête pour créer un journal comptable");
        JournalComptableDto savedJournal = journalComptableService.createJournalComptable(journalComptableDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseWrapper.success(savedJournal, "Journal comptable créé avec succès"));
    }

    // Read a specific journal comptable by ID
    @GetMapping("/{journalComptableId}")
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> getJournalComptable(@PathVariable UUID journalComptableId) {
        logger.info("Requête pour récupérer le journal comptable avec ID: {}", journalComptableId);
        return journalComptableService.getJournalComptable(journalComptableId)
                .map(journal -> ResponseEntity.ok(ApiResponseWrapper.success(journal, "Journal récupéré avec succès")))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Read all journals comptables for the current tenant
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>> getAllJournalComptables() {
        logger.info("Requête pour récupérer tous les journals comptables");
        List<JournalComptableDto> journals = journalComptableService.getAllJournalComptables();
        return ResponseEntity.ok(ApiResponseWrapper.success(journals, "Liste des journals récupérée avec succès"));
    }

    // Read all active journals comptables for the current tenant
    @GetMapping("/active")
    public ResponseEntity<ApiResponseWrapper<List<JournalComptableDto>>> getActiveJournalComptables() {
        logger.info("Requête pour récupérer les journals comptables actifs");
        List<JournalComptableDto> journals = journalComptableService.getActiveJournalComptables();
        return ResponseEntity.ok(ApiResponseWrapper.success(journals, "Liste des journals actifs récupérée avec succès"));
    }

    // Update a journal comptable
    @PutMapping("/{journalComptableId}")
    public ResponseEntity<ApiResponseWrapper<JournalComptableDto>> updateJournalComptable(@PathVariable UUID journalComptableId, @RequestBody JournalComptableDto journalComptableDto) {
        logger.info("Requête pour mettre à jour le journal comptable avec ID: {}", journalComptableId);
        try {
            JournalComptableDto updatedJournal = journalComptableService.updateJournalComptable(journalComptableId, journalComptableDto);
            return ResponseEntity.ok(ApiResponseWrapper.success(updatedJournal, "Journal mis à jour avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete a journal comptable
    @DeleteMapping("/{journalComptableId}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteJournalComptable(@PathVariable UUID journalComptableId) {
        logger.info("Requête pour supprimer le journal comptable avec ID: {}", journalComptableId);
        try {
            journalComptableService.deleteJournalComptable(journalComptableId);
            return ResponseEntity.ok(ApiResponseWrapper.success(null, "Journal supprimé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

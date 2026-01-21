package com.yowyob.erp.accounting.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JournalAudit entity traces actions like creation, validation, deletion, and
 * modification.
 * Audit logs are integrated directly within the entity.
 * 
 * @author Leonel Delmat AZANGUE
 * @date 30.09.25
 */
@Entity
@Table(name = "journal_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "journal_audit_id")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "ecriture_id")
    private UUID ecriture_comptable_id;

    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String action;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime date_action;

    @Column(length = 255)
    private String utilisateur;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip", length = 50)
    private String adresse_ip;

    @Column(name = "donnees_avant", columnDefinition = "TEXT")
    private String donnees_avant;

    @Column(name = "donnees_apres", columnDefinition = "TEXT")
    private String donnees_apres;

    /** Date of creation */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;

    /** Date of last update */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;

    /** User who created the record */
    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String created_by;

    /** User who last updated the record */
    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    /**
     * Set creation and action dates before persistence.
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.created_at = now;
        this.updated_at = now;
        if (this.date_action == null) {
            this.date_action = now;
        }
    }

    /**
     * Update the last modified date before update.
     */
    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}

package com.yowyob.erp.accounting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting journal (Journal Comptable).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "journaux_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "code_journal", nullable = false, length = 20)
    private String code_journal;

    @Column(nullable = false, length = 255)
    private String libelle;

    @Column(name = "type_journal", nullable = false, length = 50)
    private String type_journal; // VENTES, ACHATS, TRESORERIE...

    @Column(length = 255)
    private String notes;

    @Builder.Default
    private Boolean actif = true;

    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @Size(max = 255)
    @Column(name = "created_by", length = 255)
    private String created_by;

    @Size(max = 255)
    @Column(name = "updated_by", length = 255)
    private String updated_by;

    @PrePersist
    public void onCreate() {
        this.created_at = LocalDateTime.now();
        this.updated_at = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updated_at = LocalDateTime.now();
    }
}
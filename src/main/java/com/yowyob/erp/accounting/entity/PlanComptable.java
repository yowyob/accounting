package com.yowyob.erp.accounting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an accounting account (Plan Comptable).
 * Follows snake_case naming as per Development Charter.
 * 
 * @author ALD
 * @date 30.09.25
 */
@Entity
@Table(name = "plans_comptables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private Integer classe;

    @Column(nullable = false, unique = true, length = 20)
    private String no_compte;

    @Column(nullable = false, length = 255)
    private String libelle;

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

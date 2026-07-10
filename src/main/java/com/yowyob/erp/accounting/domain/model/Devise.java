package com.yowyob.erp.accounting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a Currency (Devise) for R2DBC.
 */
@Table(name = "devises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Devise implements com.yowyob.erp.shared.infrastructure.persistence.SettablePersistable<UUID> {

    @Id
    private UUID id;

    @NotBlank
    @Column("code")
    private String code;

    @NotBlank
    @Column("nom")
    private String nom;

    @Column("symbole")
    private String symbole;

    @Builder.Default
    @Column("est_nationale")
    private boolean est_nationale = false;

    @Builder.Default
    @Column("actif")
    private boolean actif = true;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime created_at = LocalDateTime.now();

    @Column("updated_at")
    private LocalDateTime updated_at;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    @Transient
    public boolean isNew() {
        return isNew || id == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}

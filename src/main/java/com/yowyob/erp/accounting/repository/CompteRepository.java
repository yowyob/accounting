package com.yowyob.erp.accounting.repository;

import com.yowyob.erp.accounting.entity.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour la gestion des comptes comptables OHADA.
 * 
 * Implémente les opérations de recherche multi-tenant, 
 * ainsi que les filtres par numéro de compte et classe.
 * 
 * Conforme à la Charte de Développement Yowyob :
 * - requêtes claires
 * - noms explicites
 * - isolation par tenant_id
 */
@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {

    /** Recherche un compte par tenant et numéro de compte */
    Optional<Compte> findByTenantIdAndNoCompte(UUID tenantId, String noCompte);

    /** Liste des comptes actifs d’un tenant */
    List<Compte> findByTenantIdAndActifTrue(UUID tenantId);

    /** Liste des comptes d’un tenant par classe OHADA */
    List<Compte> findByTenantIdAndClasse(UUID tenantId, Integer classe);

    /** Vérifie si un compte existe pour un tenant et un numéro donné */
    boolean existsByTenantIdAndNoCompte(UUID tenantId, String noCompte);

    /** Recherche des comptes dont le numéro commence par un préfixe donné */
    @Query("SELECT c FROM Compte c WHERE c.tenantId = :tenantId AND c.noCompte LIKE CONCAT(:prefix, '%')")
    List<Compte> findByTenantIdAndNoCompteStartingWith(UUID tenantId, String prefix);

    /** Tous les comptes d’un tenant (y compris inactifs) */
    List<Compte> findAllByTenantId(UUID tenantId);
}

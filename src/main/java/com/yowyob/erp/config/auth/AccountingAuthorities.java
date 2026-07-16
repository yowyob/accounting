package com.yowyob.erp.config.auth;

/**
 * Expressions SpEL réutilisables pour {@code @PreAuthorize} sur les endpoints comptables.
 *
 * <p>Source unique de vérité, alignée sur la matrice de rôles du frontend
 * ({@code FRONTEND/src/lib/auth/roles.ts}) et la spécification fonctionnelle :</p>
 * <ul>
 *   <li><b>AIDE_COMPTABLE</b>       : lecture + saisie de brouillons</li>
 *   <li><b>COMPTABLE</b>            : + gestion comptes/journaux/taxes + validation</li>
 *   <li><b>RESPONSABLE_COMPTABLE</b>: accès intégral au module (clôtures, paramètres, périodes)</li>
 *   <li><b>ADMIN / OWNER / ORGANIZATION_ADMIN</b> : super-utilisateurs — toujours autorisés
 *       (OWNER = ADMIN : le propriétaire/admin d'org pilote la compta sans rôle comptable dédié)</li>
 * </ul>
 *
 * <p>Rappel chaîne d'auth : le claim JWT {@code permissions} du Kernel contient la chaîne
 * brute (ex. {@code COMPTABLE}) ; {@code JwtAuthenticationFilter} préfixe {@code ROLE_} →
 * {@code hasRole('COMPTABLE')} teste l'autorité {@code ROLE_COMPTABLE}.</p>
 */
public final class AccountingAuthorities {

    private AccountingAuthorities() {
    }

    /**
     * Rôles « super-utilisateur » toujours autorisés sur la compta : administrateur transverse,
     * ainsi que le propriétaire d'organisation et l'administrateur d'organisation (OWNER = ADMIN).
     * Ils accèdent au module sans qu'on ait à leur assigner un rôle comptable dédié.
     */
    private static final String ADMINS = "'ADMIN','OWNER','ORGANIZATION_ADMIN'";

    /** Lecture / consultation — tous les rôles comptables. */
    public static final String READ =
            "hasAnyRole(" + ADMINS + ",'RESPONSABLE_COMPTABLE','COMPTABLE','AIDE_COMPTABLE')";

    /** Saisie de brouillons (pièces en attente) — inclut l'aide-comptable. */
    public static final String DRAFT = READ;

    /** Gestion comptable (comptes, journaux, taxes, validation) — comptable et au-dessus. */
    public static final String MANAGE =
            "hasAnyRole(" + ADMINS + ",'RESPONSABLE_COMPTABLE','COMPTABLE')";

    /** Opérations sensibles (clôtures, paramètres, verrouillage de période) — responsable et au-dessus. */
    public static final String SUPERVISE =
            "hasAnyRole(" + ADMINS + ",'RESPONSABLE_COMPTABLE')";

    /** Opérations purement techniques (synchronisation, purge de cache) — administrateur (et owner) uniquement. */
    public static final String ADMIN_ONLY = "hasAnyRole(" + ADMINS + ")";
}

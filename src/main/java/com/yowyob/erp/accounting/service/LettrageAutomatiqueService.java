package com.yowyob.erp.accounting.service;

import com.yowyob.erp.accounting.repository.DetailEcritureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.transaction.annotation.Transactional;

import java.util.*;


/**
 * Service for automatic letttering of entries.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LettrageAutomatiqueService {

    private final EntityManager em;

    @Transactional
    public int lettrerToutLeTenant(UUID tenantId) {
        String sql = """
            WITH candidats AS (
                SELECT 
                    d1.id as id_debit,
                    d2.id as id_credit
                FROM details_ecritures d1
                JOIN details_ecritures d2 
                  ON d1.tenant_id = d2.tenant_id
                 AND d1.ecriture_id <> d2.ecriture_id
                 AND COALESCE(d1.lettree, false) = false 
                 AND COALESCE(d2.lettree, false) = false
                 AND d1.compte_id = d2.compte_id
                 AND d1.montant_debit = d2.montant_credit
                 AND ABS(d2.montant_credit - d1.montant_debit) < 0.01
                 AND ABS(EXTRACT(DAY FROM (d2.date_ecriture - d1.date_ecriture))) <= 120
                WHERE d1.tenant_id = :tenantId
                  AND d1.sens = 'DEBIT'
                  AND d2.sens = 'CREDIT'
            )
            UPDATE details_ecritures d
            SET lettree = true,
                date_lettrage = CURRENT_DATE
            FROM candidats c
            WHERE d.id = c.id_debit OR d.id = c.id_credit;
            """;

        Query q = em.createNativeQuery(sql);
        q.setParameter("tenantId", tenantId);
        int lignes = q.executeUpdate();
        return lignes / 2;
    }
}
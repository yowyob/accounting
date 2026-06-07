package com.yowyob.erp.shared.application.service;

import com.yowyob.erp.shared.domain.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ValidationService {

    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{1,7}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Valide un numéro de compte selon les règles OHADA
     */
    public void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new BusinessException("Le numéro de compte est obligatoire");
        }
        
        if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new BusinessException("Format de numéro de compte invalide selon OHADA");
        }
    }

    /**
     * Valide l'équilibre comptable (débit = crédit)
     */
    public void validateAccountingBalance(Double totalDebit, Double totalCredit) {
        if (totalDebit == null || totalCredit == null) {
            throw new BusinessException("Les montants débit et crédit sont obligatoires");
        }
        
        if (!totalDebit.equals(totalCredit)) {
            throw new BusinessException(
                String.format("L'écriture n'est pas équilibrée: Débit=%.2f, Crédit=%.2f", 
                             totalDebit, totalCredit));
        }
    }

    /**
     * Valide un montant
     */
    public void validateAmount(Double amount, String field) {
        if (amount == null) {
            throw new BusinessException("Le montant " + field + " est obligatoire");
        }
        
        if (amount < 0) {
            throw new BusinessException("Le montant " + field + " ne peut pas être négatif");
        }
        
        if (amount > 999999999.99) {
            throw new BusinessException("Le montant " + field + " est trop élevé");
        }
    }

    /**
     * Valide une période comptable
     */
    public void validateAccountingPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("Les dates de début et fin sont obligatoires");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin");
        }
        
        if (startDate.isBefore(LocalDate.now().minusYears(10))) {
            throw new BusinessException("La période ne peut pas être antérieure à 10 ans");
        }
    }

    /**
     * Valide un email
     */
    public void validateEmail(String email) {
        if (email != null && !email.trim().isEmpty() && 
            !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("Format d'email invalide");
        }
    }

    /**
     * Valide le plafond client
     */
    public void validateClientLimit(Double currentBalance, Double limit, Double transactionAmount) {
        if (limit != null && limit > 0) {
            Double newBalance = (currentBalance != null ? currentBalance : 0.0) + transactionAmount;
            if (newBalance > limit) {
                throw new BusinessException(
                    String.format("Plafond client dépassé. Limite: %.2f, Nouveau solde: %.2f", 
                                 limit, newBalance));
            }
        }
    }
}
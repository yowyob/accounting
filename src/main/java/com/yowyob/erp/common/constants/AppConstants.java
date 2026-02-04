// Constantes de l'application
package com.yowyob.erp.common.constants;

public class AppConstants {

    // Codes de comptes OHADA
    public static final class AccountCodes {
        public static final String CASH_ACCOUNT_PREFIX = "57"; // Comptes de caisse
        public static final String CLIENT_ACCOUNT_PREFIX = "411"; // Clients
        public static final String SUPPLIER_ACCOUNT_PREFIX = "401"; // Fournisseurs
        public static final String SALES_ACCOUNT_PREFIX = "701"; // Ventes
        public static final String PURCHASE_ACCOUNT_PREFIX = "603"; // Achats
        public static final String INVENTORY_ACCOUNT_PREFIX = "31"; // Stocks
        public static final String VAT_ACCOUNT_PREFIX = "443"; // TVA
    }

    // Types d'opérations comptables
    public static final class OperationTypes {
        public static final String SALE = "SALE";
        public static final String PURCHASE = "PURCHASE";
        public static final String PAYMENT = "PAYMENT";
        public static final String RECEIPT = "RECEIPT";
        public static final String TRANSFER = "TRANSFER";
    }

    // Modes de règlement
    public static final class PaymentMethods {
        public static final String CASH = "CASH";
        public static final String CREDIT = "CREDIT";
        public static final String BANK_TRANSFER = "BANK_TRANSFER";
        public static final String CHECK = "CHECK";
        public static final String MOBILE_MONEY = "MOBILE_MONEY";
    }

    // Types de montant
    public static final class AmountTypes {
        public static final String HT = "HT"; // Hors Taxes
        public static final String TTC = "TTC"; // Toutes Taxes Comprises
        public static final String TVA = "TVA"; // Taxe sur la Valeur Ajoutée
        public static final String PAU = "PAU"; // Prix d'Achat Unitaire
    }

    // Sens comptable
    public static final class AccountingSense {
        public static final String DEBIT = "DEBIT";
        public static final String CREDIT = "CREDIT";
    }

    // Types de journaux
    public static final class JournalTypes {
        public static final String SALES = "SALES";
        public static final String PURCHASES = "PURCHASES";
        public static final String CASH = "CASH";
        public static final String BANK = "BANK";
        public static final String GENERAL = "GENERAL";
    }

    // Événements Kafka
    public static final class KafkaEvents {
        public static final String ACCOUNTING_ENTRY_CREATED = "ACCOUNTING_ENTRY_CREATED";
        public static final String ACCOUNTING_ENTRY_VALIDATED = "ACCOUNTING_ENTRY_VALIDATED";
        public static final String INVOICE_CREATED = "INVOICE_CREATED";
        public static final String INVOICE_PAID = "INVOICE_PAID";
        public static final String PERIOD_CLOSED = "PERIOD_CLOSED";
    }

    // Rôles utilisateur
    public static final class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String ACCOUNTANT = "ACCOUNTANT";
        public static final String USER = "USER";
        public static final String AUDITOR = "AUDITOR";
    }
}

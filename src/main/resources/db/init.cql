-- Creation du keyspace avec réplication NetworkTopologyStrategy
-- Changement du facteur de réplication à 1 pour un cluster à un seul nœud
CREATE KEYSPACE IF NOT EXISTS yowyob_erp
WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1}
AND durable_writes = true
AND TABLETS = {'enabled': false};

USE yowyob_erp;

-- Table pour les comptes
CREATE TABLE IF NOT EXISTS compte (
    organization_id UUID,
    id UUID,
    no_compte TEXT,
    libelle TEXT,
    notes TEXT,
    soldes DECIMAL,
    type_compte TEXT,
    classe INT,
    actif BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Index pour le numéro de compte
CREATE INDEX IF NOT EXISTS idx_no_compte ON compte (no_compte);

-- Table pour les contreparties
CREATE TABLE IF NOT EXISTS contrepartie (
    organization_id UUID,
    id UUID,
    compte TEXT,
    est_compte_tiers BOOLEAN,
    sens TEXT,
    type_montant TEXT,
    journal_comptable_id UUID,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id, compte)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Table pour les déclarations fiscales
CREATE TABLE IF NOT EXISTS declaration_fiscale (
    organization_id UUID,
    id UUID,
    type_declaration TEXT,
    periode_debut DATE,
    periode_fin DATE,
    montant_total DOUBLE,
    date_generation DATE,
    statut TEXT,
    numero_declaration TEXT,
    donnees_declaration TEXT,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id, type_declaration)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Table pour les détails d écriture
CREATE TABLE IF NOT EXISTS detail_ecriture (
    organization_id UUID,
    id UUID,
    ecriture_id UUID,
    compte_comptable_id UUID,
    libelle TEXT,
    sens TEXT,
    montant_debit DOUBLE,
    montant_credit DOUBLE,
    notes TEXT,
    date_ecriture TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Index pour trier par ecriture_comptable
CREATE INDEX IF NOT EXISTS ON detail_ecriture(ecriture_id);

-- Table pour les écritures comptables
CREATE TABLE IF NOT EXISTS ecriture_comptable (
    organization_id UUID,
    id UUID,
    numero_ecriture TEXT,
    libelle TEXT,
    date_ecriture DATE,
    journal_comptable_id UUID,
    periode_comptable_id UUID,
    montant_total_debit DOUBLE,
    montant_total_credit DOUBLE,
    validee BOOLEAN,
    date_validation TIMESTAMP,
    utilisateur_validation TEXT,
    reference_externe TEXT,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

CREATE INDEX IF NOT EXISTS idx_ecriture_numero ON ecriture_comptable (numero_ecriture);
CREATE INDEX IF NOT EXISTS idx_ecriture_journal ON ecriture_comptable (journal_comptable_id);

-- Table pour les journaux d audit
CREATE TABLE IF NOT EXISTS journal_audit (
    organization_id UUID,
    date_action TIMESTAMP,
    id UUID,
    ecriture_comptable_id UUID,
    action TEXT,
    utilisateur TEXT,
    details TEXT,
    adresse_ip TEXT,
    donnees_avant TEXT,
    donnees_apres TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, date_action, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'}
AND CLUSTERING ORDER BY (date_action DESC, id ASC);

-- Table pour les journaux comptables
CREATE TABLE IF NOT EXISTS journal_comptable (
    organization_id UUID,
    id UUID,
    code_journal TEXT,
    libelle TEXT,
    type_journal TEXT,
    notes TEXT,
    actif BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Création d index sur les journaux comptables
CREATE INDEX IF NOT EXISTS ON journal_comptable(code_journal);
CREATE INDEX IF NOT EXISTS ON journal_comptable(actif);

-- Table pour les opérations comptables
CREATE TABLE IF NOT EXISTS operation_comptable (
    organization_id UUID,
    id UUID,
    operation_comptable_id UUID,
    type_operation TEXT,
    mode_reglement TEXT,
    compte_principal TEXT,
    est_compte_statique BOOLEAN,
    sens_principal TEXT,
    journal_comptable_id UUID,
    type_montant TEXT,
    plafond_client DOUBLE,
    actif BOOLEAN,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

CREATE INDEX IF NOT EXISTS ON operation_comptable(operation_comptable_id);
CREATE INDEX IF NOT EXISTS ON operation_comptable(actif);
CREATE INDEX IF NOT EXISTS ON operation_comptable(journal_comptable_id);    
CREATE INDEX IF NOT EXISTS ON operation_comptable(compte_principal);    
CREATE INDEX IF NOT EXISTS ON operation_comptable(type_operation); 
CREATE INDEX IF NOT EXISTS ON operation_comptable(mode_reglement); 

-- Table pour les périodes comptables
CREATE TABLE IF NOT EXISTS periode_comptable (
    organization_id UUID,
    id UUID,
    code TEXT,
    date_debut DATE,
    date_fin DATE,
    cloturee BOOLEAN,
    date_cloture DATE,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

CREATE INDEX IF NOT EXISTS ON periode_comptable(code);

-- Table pour les plan comptables
CREATE TABLE IF NOT EXISTS plan_comptable (
    organization_id UUID,
    id UUID,
    no_compte TEXT,
    code_classe INT,
    libelle TEXT,
    notes TEXT,
    actif BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Création des index pour pouvoir effectuer des filtres sur des colonnes non-clés
CREATE INDEX IF NOT EXISTS ON plan_comptable (actif);
CREATE INDEX IF NOT EXISTS ON plan_comptable (code_classe);
CREATE INDEX IF NOT EXISTS ON plan_comptable (no_compte);

-- Table pour les transactions
CREATE TABLE IF NOT EXISTS transaction (
    organization_id UUID,
    id UUID,
    numero_recu TEXT,
    operation_comptable_id UUID,
    montant_transaction DOUBLE,
    montant_lettre TEXT,
    est_montant_ttc BOOLEAN,
    date_transaction TIMESTAMP,
    est_validee BOOLEAN,
    date_validation TIMESTAMP,
    reference_objet TEXT,
    caissier TEXT,
    est_comptabilisee BOOLEAN,
    ecriture_comptable_id UUID,
    notes TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id, numero_recu)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

-- Table pour les organizations
CREATE TABLE IF NOT EXISTS organizations (
    organization_id UUID,
    id UUID,
    name TEXT,
    legal_name TEXT,
    registration_number TEXT,
    tax_id TEXT,
    address TEXT,
    phone TEXT,
    email TEXT,
    currency TEXT,
    is_active BOOLEAN,
    accounting_code TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by TEXT,
    updated_by TEXT,
    PRIMARY KEY (organization_id)
)
WITH compression = {'sstable_compression': 'LZ4Compressor'};

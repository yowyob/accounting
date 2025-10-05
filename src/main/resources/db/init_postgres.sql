
-- init_postgres.sql
-- Migration from ScyllaDB (init.cql) to PostgreSQL 16
-- Conforms to Yowyob Charte (naming, layering) and OHADA accounting model
-- Generated: now

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ======================
-- SCHEMA & TENANCY
-- ======================
CREATE SCHEMA IF NOT EXISTS public;

-- Tenants
CREATE TABLE IF NOT EXISTS tenants (
  tenant_id UUID PRIMARY KEY,
  name TEXT,
  tax_id TEXT,
  address TEXT,
  phone TEXT,
  email TEXT,
  currency TEXT,
  is_active BOOLEAN DEFAULT TRUE,
  accounting_code TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

-- ======================
-- Reference: Plan Comptable (compte)
-- ======================
CREATE TABLE IF NOT EXISTS plan_comptable (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  no_compte VARCHAR(20) NOT NULL,
  libelle TEXT NOT NULL,
  code_classe SMALLINT,               -- 1..9 (OHADA classes)
  actif BOOLEAN DEFAULT TRUE,
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT,
  UNIQUE (tenant_id, no_compte)
);

CREATE INDEX IF NOT EXISTS idx_plan_comptable_tenant ON plan_comptable(tenant_id);
CREATE INDEX IF NOT EXISTS idx_plan_comptable_actif ON plan_comptable(actif);
CREATE INDEX IF NOT EXISTS idx_plan_comptable_code_classe ON plan_comptable(code_classe);

-- Alias 'compte' table name from CQL -> keep a view for compatibility (optional)
CREATE OR REPLACE VIEW compte AS
  SELECT tenant_id, id, no_compte, libelle, notes, created_at, updated_at, created_by, updated_by
  FROM plan_comptable;

-- ======================
-- Journaux
-- ======================
CREATE TABLE IF NOT EXISTS journal_comptable (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  code_journal VARCHAR(20) NOT NULL,
  libelle TEXT NOT NULL,
  type_journal TEXT,                  -- ventes, achats, tresorerie, etc.
  notes TEXT,
  actif BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT,
  UNIQUE (tenant_id, code_journal)
);

CREATE INDEX IF NOT EXISTS idx_journal_comptable_tenant ON journal_comptable(tenant_id);
CREATE INDEX IF NOT EXISTS idx_journal_comptable_code ON journal_comptable(code_journal);
CREATE INDEX IF NOT EXISTS idx_journal_comptable_actif ON journal_comptable(actif);

-- ======================
-- Périodes comptables
-- ======================
CREATE TABLE IF NOT EXISTS periode_comptable (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  code TEXT NOT NULL,
  date_debut DATE NOT NULL,
  date_fin DATE NOT NULL,
  cloturee BOOLEAN DEFAULT FALSE,
  date_cloture TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT,
  UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_periode_comptable_tenant ON periode_comptable(tenant_id);
CREATE INDEX IF NOT EXISTS idx_periode_comptable_cloturee ON periode_comptable(cloturee);

-- ======================
-- Opérations comptables (paramétrage)
-- ======================
CREATE TABLE IF NOT EXISTS operation_comptable (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  type_operation TEXT NOT NULL,                 -- vente, achat, tresorerie, etc.
  mode_reglement TEXT,                          -- espece, credit, etc.
  est_compte_statique BOOLEAN DEFAULT FALSE,
  compte_principal_id UUID REFERENCES plan_comptable(id),  -- statique; si dynamique alors null et est_compte_tiers = TRUE
  est_compte_tiers BOOLEAN DEFAULT FALSE,
  journal_comptable_id UUID REFERENCES journal_comptable(id),
  type_montant TEXT,                             -- HT, TTC, TVA, PAU ...
  sens_comptable TEXT,                           -- DEBIT/CREDIT pour le compte principal
  plafond_client NUMERIC(18,2),
  actif BOOLEAN DEFAULT TRUE,
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_operation_comptable_tenant ON operation_comptable(tenant_id);
CREATE INDEX IF NOT EXISTS idx_operation_comptable_type ON operation_comptable(type_operation);

-- Contreparties de l'opération (Phase 2)
CREATE TABLE IF NOT EXISTS contrepartie (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  operation_comptable_id UUID NOT NULL REFERENCES operation_comptable(id) ON DELETE CASCADE,
  compte_id UUID REFERENCES plan_comptable(id),     -- null si compte tiers
  est_compte_tiers BOOLEAN DEFAULT FALSE,
  sens_comptable TEXT CHECK (sens_comptable IN ('DEBIT','CREDIT')),
  type_montant TEXT,                                 -- HT, TTC, TVA, PAU...
  journal_comptable_id UUID REFERENCES journal_comptable(id),
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_contrepartie_operation ON contrepartie(operation_comptable_id);

-- ======================
-- Ecritures comptables
-- ======================
CREATE TABLE IF NOT EXISTS ecriture_comptable (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  journal_comptable_id UUID REFERENCES journal_comptable(id),
  periode_id UUID REFERENCES periode_comptable(id),
  numero_piece TEXT,
  date_ecriture TIMESTAMPTZ DEFAULT NOW(),
  statut TEXT DEFAULT 'BROUILLARD',               -- BROUILLARD | VALIDE
  reference_objet TEXT,                            -- lien externe (facture, livraison, etc.)
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_ecriture_tenant ON ecriture_comptable(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ecriture_journal ON ecriture_comptable(journal_comptable_id);
CREATE INDEX IF NOT EXISTS idx_ecriture_periode ON ecriture_comptable(periode_id);

CREATE TABLE IF NOT EXISTS detail_ecriture (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  ecriture_id UUID NOT NULL REFERENCES ecriture_comptable(id) ON DELETE CASCADE,
  plan_comptable_id UUID REFERENCES plan_comptable(id),
  libelle TEXT,
  sens TEXT CHECK (sens IN ('DEBIT','CREDIT')),
  montant NUMERIC(18,2) NOT NULL DEFAULT 0,
  notes TEXT,
  date_ecriture TIMESTAMPTZ,
  valider BOOLEAN DEFAULT FALSE,
  date_validation TIMESTAMPTZ,
  utilisateur_validation TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_detail_ecriture_ecriture ON detail_ecriture(ecriture_id);
CREATE INDEX IF NOT EXISTS idx_detail_ecriture_plan ON detail_ecriture(plan_comptable_id);

-- Journal d'audit
CREATE TABLE IF NOT EXISTS journal_audit (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  ecriture_id UUID REFERENCES ecriture_comptable(id) ON DELETE SET NULL,
  action TEXT,                         -- creation/validation/modification
  utilisateur TEXT,
  date_action TIMESTAMPTZ DEFAULT NOW(),
  details TEXT
);

CREATE INDEX IF NOT EXISTS idx_journal_audit_tenant_date ON journal_audit(tenant_id, date_action DESC);

-- ======================
-- Transactions (source des écritures)
-- ======================
CREATE TABLE IF NOT EXISTS transaction (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  numero_recu TEXT,
  operation_comptable_id UUID REFERENCES operation_comptable(id),
  montant_transaction NUMERIC(18,2),
  montant_lettre TEXT,
  est_montant_ttc BOOLEAN,
  date_transaction TIMESTAMPTZ,
  est_validee BOOLEAN DEFAULT FALSE,
  date_validation TIMESTAMPTZ,
  reference_objet TEXT,
  caissier TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_transaction_tenant ON transaction(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transaction_operation ON transaction(operation_comptable_id);

-- ======================
-- Déclarations fiscales (TVA, etc.)
-- ======================
CREATE TABLE IF NOT EXISTS declaration_fiscale (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  type_declaration TEXT NOT NULL,         -- TVA, etc.
  periode TEXT NOT NULL,                  -- e.g. 2025-07
  montant NUMERIC(18,2) DEFAULT 0,
  date_generation TIMESTAMPTZ DEFAULT NOW(),
  statut TEXT,                             -- BROUILLARD/VALIDE/ENVOYEE
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  created_by TEXT,
  updated_by TEXT,
  UNIQUE (tenant_id, type_declaration, periode)
);

-- ======================
-- Helper constraints: enforce balanced entries
-- ======================
-- Optional: ensure sum(debit)=sum(credit) per ecriture when statut passes to VALIDE
-- This is typically enforced at application/service layer; a database trigger can be added later.


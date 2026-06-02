# 📚 Documentation — YOWYOB ERP Backend

Ce dossier centralise toute la documentation du projet **KSM-ERP-YOWYOB Backend**.

---

## 📂 Organisation

```
docs/
├── conception/          → Cahier de conception détaillée
│   ├── cahier_conception.pdf   → Document compilé (PDF)
│   ├── cahier_conception.tex   → Source LaTeX
│   ├── chapters/               → Chapitres LaTeX
│   └── annexes/                → Annexes
│
├── architecture/        → Diagrammes d'architecture et d'états
│   ├── DiagStateEntries.png    → Diagramme d'états des écritures
│   └── DiagStateExercice.png   → Diagramme d'états des exercices
│
├── reports/             → Rapports PDF et sources LaTeX
│   ├── BACKEND_REQUIREMENTS.pdf
│   ├── KSM-ERP-Yowyob System Project Book.pdf
│   ├── Charte de Développement et contraintes Technologiques.pdf
│   ├── Yowyob-KSM-ERP-Rapport_modelisation2-+Rmq TDN.pdf
│   ├── rapport_erp.tex          → Source LaTeX du rapport ERP
│   ├── page_de_garde.tex        → Page de garde
│   └── phase.tex                → Description des phases
│
├── guides/              → Guides techniques
│   ├── DEPLOYMENT.md            → Guide de déploiement sur Render
│   ├── integration_liquibase.md → Intégration Liquibase
│   └── perspectives.md          → Perspectives et évolutions futures
│
└── data/                → Données de référence
    ├── plan-ohada-2025.csv         → Plan comptable OHADA 2025
    ├── sample_supplier_invoice.json → Exemple de facture fournisseur
    ├── core-docs.json               → Documentation API exportée
    └── *.csv                        → Variantes du plan comptable (référence)
```

---

## 🔗 Liens rapides

| Document | Description |
|----------|-------------|
| [Cahier de conception](conception/cahier_conception.pdf) | Architecture et conception détaillée du système |
| [Guide de déploiement](guides/DEPLOYMENT.md) | Déployer sur Render (prod) |
| [Intégration Liquibase](guides/integration_liquibase.md) | Gestion des migrations de base de données |
| [Perspectives](guides/perspectives.md) | Roadmap et évolutions futures |
| [Plan OHADA 2025](data/plan-ohada-2025.csv) | Plan comptable OHADA de référence |

---

## 📝 Convention pour la documentation

- **Guides** : Écrits en Markdown, placés dans `guides/`
- **Rapports** : Sources LaTeX dans `reports/`, PDF compilés aussi dans `reports/`
- **Diagrammes** : PNG/SVG dans `architecture/`
- **Données** : CSV/JSON dans `data/`

> ⚠️ Ne pas versionner les artefacts de compilation LaTeX (`.aux`, `.log`, `.toc`, etc.) — ils sont couverts par le `.gitignore`.

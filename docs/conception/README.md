## Cahier de conception (LaTeX)

- **Source**: `cahier_conception.tex`
- **Objectif**: document de conception généré par reverse engineering du backend.

### Compiler

Si vous avez LaTeX installé:

```bash
cd docs/conception
pdflatex cahier_conception.tex
pdflatex cahier_conception.tex
```

Ou avec latexmk:

```bash
cd docs/conception
latexmk -pdf cahier_conception.tex
```


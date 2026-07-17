#!/usr/bin/env python3
# Régénère src/main/resources/plan_comptable_ohada_713.csv depuis le PDF officiel
# SYSCOHADA 2025. Usage :
#   pdftotext -layout <plan-syscohada-2025.pdf> /tmp/sysc_layout.txt
#   python3 docs/data/parse_syscohada.py
# Parse colonne-aware (2 colonnes/page), fusionne les libellés multi-lignes,
# dérive classe/niveau/compte_mère, préfixe les fragments par le libellé parent.
# Parseur COLONNE-AWARE du plan SYSCOHADA 2025 (pdftotext -layout).
# Chaque page a 2 colonnes : on lit la colonne gauche en entier (haut->bas) puis la droite.
import re

SRC = "/tmp/sysc_layout.txt"
DST = "/home/delmat/Stage/Yowyob/KSM-ERP-YOWYOB/BACKEND/src/main/resources/plan_comptable_ohada_713.csv"

CLASS_ROOTS = {
    1: "COMPTES DE RESSOURCES DURABLES", 2: "COMPTES DES IMMOBILISATIONS",
    3: "COMPTES DE STOCKS", 4: "COMPTES DE TIERS", 5: "COMPTES DE TRESORERIE",
    6: "COMPTES DE CHARGES DES ACTIVITES ORDINAIRES",
    7: "COMPTES DES PRODUITS DES ACTIVITES ORDINAIRES",
    8: "COMPTES DES AUTRES CHARGES ET DES AUTRES PRODUITS",
}
SENS = {1: "C", 2: "D", 3: "D", 4: "D/C", 5: "D", 6: "D", 7: "C", 8: "D/C"}

acct_re = re.compile(r"^([1-9]\d{0,5})\.{0,2}\s+(\S.*)$")

text = open(SRC, encoding="utf-8").read()
start = text.find("CLASSE 1")         # on saute l'intro (paragraphes pleine largeur de la page 1)
end = text.find("CLASSE 9")           # on arrête avant la classe 9 (engagements) + glossaire
text = text[start if start > 0 else 0: end if end > 0 else len(text)]
pages = text.split("\f")

def gutter(lines):
    """Colonne de coupure : la + blanche dans [46,60] parmi les lignes non vides."""
    ne = [l for l in lines if l.strip()]
    if not ne:
        return 56
    best, best_blank = 56, -1
    for col in range(46, 61):
        blank = sum(1 for l in ne if col >= len(l) or l[col] == " ")
        if blank >= best_blank:
            best_blank, best = blank, col
    return best

def is_noise(t):
    return (not t or re.fullmatch(r"\d{1,3}", t) or "Plan de compte" in t
            or t.startswith("CLASSE "))

def collect(cells, collines):
    for raw in collines:
        t = raw.strip()
        if is_noise(t):
            continue
        m = acct_re.match(t)
        if m:
            cells.append([m.group(1), m.group(2).strip()])
        elif cells and re.match(r"^[A-Za-zÀ-ÿ(]", t):
            cells[-1][1] += " " + t          # continuation du libellé (même colonne)

cells = []
for page in pages:
    lines = page.split("\n")
    g = gutter(lines)
    left = [l[:g] for l in lines]
    right = [l[g:] for l in lines]
    collect(cells, left)     # colonne gauche d'abord
    collect(cells, right)    # puis colonne droite

# Dédup (garde 1re occurrence), racines, nettoyage.
seen, accounts = set(), []
for numero, lib in cells:
    if 1 <= int(numero[0]) <= 8 and numero not in seen:
        seen.add(numero)
        accounts.append([numero, lib])
for c, lib in CLASS_ROOTS.items():
    if str(c) not in seen:
        seen.add(str(c)); accounts.append([str(c), lib])

def clean(lib):
    lib = re.sub(r"\s+", " ", lib).strip().rstrip(".")
    return lib.replace(";", ",")

accounts = [[n, clean(l)] for n, l in accounts]
numeros = set(n for n, _ in accounts)
accounts.sort(key=lambda x: (x[0][0], x[0]))

def parent(n):
    for k in range(len(n) - 1, 0, -1):
        if n[:k] in numeros:
            return n[:k]
    return ""

label_by_num = {n: l for n, l in accounts}

def contextual_label(numero, lib):
    # Fragments SYSCOHADA (sous-compte libellé par son différenciateur, ex. "dans la Région")
    # -> on préfixe par le libellé du parent pour un intitulé lisible.
    if lib[:1].islower():
        p = parent(numero)
        if p and label_by_num.get(p):
            return f"{label_by_num[p]} - {lib}"
    return lib

with open(DST, "w", encoding="utf-8") as f:
    f.write("numero;libelle;classe;sens;lettrable;collectif;niveau;compte_mere;actif\n")
    for numero, lib in accounts:
        classe = int(numero[0])
        lib = contextual_label(numero, lib)
        f.write(";".join([numero, lib, str(classe), SENS[classe],
                          "true" if classe in (4, 5) else "false", "false",
                          str(len(numero) - 1), parent(numero), "true"]) + "\n")

print(f"écrit {len(accounts)} comptes")

#!/usr/bin/env python3
# Régénère src/main/resources/plan_comptable_ohada_713.csv depuis le PDF officiel
# SYSCOHADA 2025. Usage :
#   pdftotext -layout <plan-syscohada-2025.pdf> /tmp/sysc_layout.txt
#   grep -rhoE '"[1-9][0-9]{5}"' src/main/java | tr -d '"' | sort -u > /tmp/backend_accounts.txt
#   python3 docs/data/parse_syscohada.py
# Parse colonne-aware (2 col/page), fusionne les libellés multi-lignes, dérive
# classe/niveau/compte_mère, préfixe les fragments par le parent, ALIGNE les comptes
# de détail sur 6 chiffres (411->411000) et injecte les comptes de posting attendus
# par le backend s'ils manquent.
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
orig_nums = set(n for n, _ in accounts)
label_by_num = {n: l for n, l in accounts}

def parent_orig(n):
    for k in range(len(n) - 1, 0, -1):
        if n[:k] in orig_nums:
            return n[:k]
    return ""

def pad6(code):
    # Convention 6 chiffres : les comptes de détail (>=3 chiffres) sont padés à droite
    # par des zéros (411 -> 411000, 44571 -> 445710). Les têtes 1-2 chiffres (classes,
    # groupes) restent à leur longueur naturelle.
    return code if len(code) <= 2 else code + "0" * (6 - len(code))

def contextual_label(numero, lib):
    if lib[:1].islower():
        p = parent_orig(numero)
        if p and label_by_num.get(p):
            return f"{label_by_num[p]} - {lib}"
    return lib

rows = {}
collisions = []
for numero, lib in accounts:
    out = pad6(numero)
    if out in rows:
        collisions.append((numero, out))
        continue
    classe = int(numero[0])
    po = parent_orig(numero)
    rows[out] = [out, contextual_label(numero, lib), str(classe), SENS[classe],
                 "true" if classe in (4, 5) else "false", "false",
                 str(len(numero) - 1), pad6(po) if po else "", "true"]

# Injecte les comptes 6 chiffres attendus par le backend s'ils manquent (postings).
BACKEND_LABELS = {
    "120000": "Report à nouveau", "121000": "Report à nouveau créditeur",
    "131000": "Résultat net : bénéfice", "139000": "Résultat net : perte",
    "311000": "Marchandises", "445660": "TVA déductible / récupérable",
    "445710": "TVA collectée", "601100": "Achats de marchandises",
    "603100": "Variation des stocks de marchandises", "701100": "Ventes de marchandises",
}
try:
    backend_needed = [l.strip() for l in open("/tmp/backend_accounts.txt") if l.strip()]
except FileNotFoundError:
    backend_needed = []
injected = []
for acc in backend_needed:
    if acc in rows:
        continue
    classe = int(acc[0])
    sub = acc[:3] + "000"
    cm = sub if (sub in rows and sub != acc) else pad6(acc[:2])
    rows[acc] = [acc, BACKEND_LABELS.get(acc, "Compte de posting"), str(classe), SENS[classe],
                 "true" if classe in (4, 5) else "false", "false", "3", cm, "true"]
    injected.append(acc)

with open(DST, "w", encoding="utf-8") as f:
    f.write("numero;libelle;classe;sens;lettrable;collectif;niveau;compte_mere;actif\n")
    for out in sorted(rows, key=lambda x: (x[0], x)):
        f.write(";".join(rows[out]) + "\n")

print(f"écrit {len(rows)} comptes | collisions: {len(collisions)} | injectés backend: {injected}")

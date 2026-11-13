# Cardistry — Game Design

> Document vivant. Les éléments marqués **[À DÉCIDER]** sont ouverts : on les tranche à deux, pas Claude Code.

## Boucle de jeu (existant)
- Combat tour par tour contre une suite d'adversaires (Hydre/feu, Sirène/eau, Goblin/nature).
- À chaque tour : jouer une carte **ou** piocher. Main de 7 max. Puis l'adversaire attaque.
- Cartes : **Sortilège** (dégâts), **Heal** (soin, boosté dans le terrain du même type), **Terrain** (change le terrain, modifie l'attaque adverse).
- Éléments : **Feu > Nature > Eau > Feu**.

## Structure d'une run (cible)
- Carte à **embranchements** façon Slay the Spire, générée par seed.
- Types de nœuds : Combat, Élite, Shop, Événement (`?`), Repos, Boss. **[À DÉCIDER]** nombre d'étages, nœuds par étage.
- Mort = fin de la run. **[À DÉCIDER]** méta-progression entre les runs (déblocages) ou pas.
- **[À DÉCIDER]** passer à un vrai deck (pioche / défausse / deck construit au fil de la run) au lieu de la pioche infinie aléatoire. Recommandé pour un roguelite.

## Objets et trinkets
| | Objet | Trinket |
|---|---|---|
| Nature | Consommable, usage unique | Passif, actif toute la run |
| Slots | **[À DÉCIDER]** ex. 3 | **[À DÉCIDER]** ex. 5 |
| Exemple | Fiole de feu : 20 dégâts feu | Briquet : +25 % dégâts feu |

### Effets sur les stats
Stats modifiables : PV max, taille de main, cartes piochées, or gagné, multiplicateur par élément, efficacité des soins, réduction de dégâts. **[À DÉCIDER]** ajouter crit / esquive ?
Ordre de calcul : base → bonus plats → multiplicateurs.

### Rareté
| Rareté | Couleur | Poids drop (indicatif) | Prix shop (indicatif) |
|---|---|---|---|
| Commun | blanc | 60 | 50 |
| Peu commun | vert | 25 | 80 |
| Rare | bleu | 12 | 130 |
| Légendaire | jaune | 3 | 200 |
| Secret | ??? | n'apparaît pas en drop normal | — |

### Synergies
- Chaque objet/trinket porte des **tags** (élément, archétype : Brûlure, Soin, Terrain, Pioche…).
- Synergies de set : 2/3/4 trinkets du même tag → bonus croissant.
- Synergies directes : un trinket qui modifie l'effet d'un autre (ex. « tes trinkets Feu affectent aussi les Heals »).
- **[À DÉCIDER]** liste des premiers trinkets (viser ~15 pour le premier jet).

## Shop
- Monnaie : or gagné à chaque combat (plus en élite/boss).
- Vend : cartes, objets, trinkets. Services : retirer une carte, soin. **[À DÉCIDER]**
- Reroll payant, coût croissant.

## Secrets / easter eggs
- Trinkets « Secret » débloqués par conditions cachées (seed spéciale, combo de cartes, code clavier…).
- Garder la liste des secrets dans `docs/SECRETS.md` (spoilers !).
- Existant : terrain « EPFL » de type feu, heal « Petit pétou ».

## Interface (terminal ASCII, Lanterna)
- Plein écran, input direct au clavier (plus de « Appuie sur Entrée »).
- Écrans : titre, carte de la run, combat, shop, récompense, game over.
- Combat : sprite ASCII de l'ennemi, barres de PV, main de cartes dessinées en cadres ASCII, trinkets affichés, journal de combat.
- Animations : dégâts qui clignotent, chiffres flottants, tremblement de l'ennemi, transitions d'écran.
- Couleur de rareté et d'élément partout.

## Questions ouvertes
- Tables d'éléments : le code et les commentaires de `Magic.java` se contredisent (voir `CLAUDE.md`). Quelle est la bonne version ?
- Le nouvel adversaire doit-il attaquer le tour où il arrive ?
- Nom définitif du jeu.

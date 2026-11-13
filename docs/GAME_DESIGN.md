# Cardistry — Game Design

> Document vivant. Les éléments marqués **[À DÉCIDER]** sont ouverts : on les tranche à deux, pas Claude Code.

## Boucle de jeu (existant)
- Combat tour par tour contre une suite d'adversaires (Hydre/feu, Sirène/eau, Goblin/nature).
- À chaque tour : jouer une carte **ou** piocher. Main de 7 max. Puis l'adversaire attaque.
- Quand un adversaire meurt, le suivant arrive mais **n'attaque pas** ce tour-là.
- Cartes : **Sortilège** (dégâts), **Heal** (soin, boosté dans le terrain du même type), **Terrain** (change le terrain, modifie l'attaque adverse).
- Éléments : **Feu > Nature > Eau > Feu**.

### Multiplicateurs d'éléments (décidés)
Source de vérité dans le code : `cardistry.model.ElementRules` (testé).

**Sortilège → dégâts infligés**

| Sort \ Adversaire | Feu | Eau | Nature |
|---|---|---|---|
| Feu | ×1 | ×0.5 | ×1.5 |
| Eau | ×1.5 | ×1 | ×0.5 |
| Nature | ×0.5 | ×1.5 | ×1 |

×1.5 si le sort bat l'adversaire, ×0.5 si l'adversaire bat le sort, ×1 si même élément.

**Terrain → attaque de l'adversaire** (le terrain modifie ce que l'adversaire *inflige*, pas ce qu'il reçoit)

| Terrain \ Adversaire | Feu | Eau | Nature |
|---|---|---|---|
| Feu | ×1.25 | ×1 | ×0.75 |
| Eau | ×0.75 | ×1.25 | ×1 |
| Nature | ×1 | ×0.75 | ×1.25 |

×1.25 sur un terrain de son élément, ×0.75 si le terrain bat son élément, ×1 sinon.

**Soin → PV rendus selon le terrain** : ×1.5 sur un terrain du même élément, ×0.75 si le terrain bat l'élément du soin, ×1 sinon.

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
- Nom définitif du jeu (« Cardistry » affiché en attendant).
- Les soins n'ont pas de plafond : les PV peuvent dépasser 100. Faut-il plafonner aux PV max ?
- Pioche infinie avec remise (les doublons du registre servent de poids) : voir le passage à un vrai deck dans « Structure d'une run ».

### Tranchées
- ~~Tables d'éléments contradictoires~~ → sortilèges : table (×1.5 / ×0.5 / ×1) ; terrain : multiplie l'attaque adverse (×1.25 / ×0.75 / ×1). Voir « Multiplicateurs d'éléments ».
- ~~Le nouvel adversaire attaque-t-il le tour où il arrive ?~~ → Non.

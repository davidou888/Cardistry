# Cardistry — Game Design

> Document vivant. Les éléments marqués **[À DÉCIDER]** sont ouverts : c'est Louis qui les tranche, pas Claude Code.

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
| Slots | **[À DÉCIDER]** ex. 3 | **[À DÉCIDER]** ex. 5 (+1 avec Diplôme EPFL) |
| Exemple | Maté : +10 PV | Redistribution des briquets : +25 % dégâts feu |

### Effets sur les stats
Stats modifiables : PV max (plafond des soins), taille de main, cartes piochées, or gagné, multiplicateur par élément, efficacité des soins, réduction de dégâts. **[À DÉCIDER]** ajouter crit / esquive ?
Ordre de calcul : base → bonus plats → pourcentages. Les pourcentages **s'additionnent** : deux trinkets +25 % feu = +50 % (×1.5), pas ×1.5625. Formule : `(base + plats) × (1 + somme des %)`, plancher à 0.

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
- Tags utilisés par le premier jet : **EPFL**, **Soirée**, **Boisson**, **Méta**, éléments (Feu, Eau, Nature), archétypes (Soin, Pioche, Terrain, Défense).
- **[À DÉCIDER]** quels tags déclenchent des bonus de set, et lesquels.

### Ivresse
- Chaque **boisson** (objet tagué Boisson) donne **+1 Ivresse**. L'Ivresse retombe à 0 après chaque combat.
- À **3 Ivresse** → malédiction **« Gueule de bois »** : −1 taille de main au prochain combat.
- Modifiée par : Foie d'ingénieur (seuil à 5), Ptit Sat ? (+10 % dégâts par point d'Ivresse).

### Contenu — premier jet
Colonne **Méca.** : **a** = faisable avec les mécaniques existantes (stats, hooks sur les événements actuels, dégâts / soins / pioche / terrain) ; **b** = demande une nouvelle mécanique (voir « Nouvelles mécaniques »).

#### Trinkets
| Rareté | Nom | Effet | Tags | Méca. |
|---|---|---|---|---|
| Commun | Redistribution des briquets | +25 % dégâts feu | Feu, Soirée | a |
| Commun | Pichet de Sat | +25 % dégâts eau | Eau, EPFL | a |
| Commun | Plante du Rolex | +25 % dégâts nature | Nature, EPFL | a |
| Commun | Polycopié d'Analyse I | −2 dégâts à chaque attaque subie | Défense, EPFL | a |
| Commun | Latte macchiato vanille | +1 carte au premier tour de chaque combat | Pioche, EPFL | a (+ notion de combat, M8) |
| Peu commun | M1 | Changer de terrain rend 5 PV | Terrain, EPFL | a |
| Peu commun | Pax | +1 taille de main | Pioche, Soirée | a |
| Peu commun | Monde Snack | +10 PV à chaque adversaire vaincu | Soin, Soirée | a |
| Peu commun | Grinder | Soins de type feu +50 % | Soin, Soirée | a |
| Peu commun | Plan du campus | Jouer un terrain inflige aussi 5 dégâts | Terrain, EPFL | a |
| Rare | Photocopieuse | Piocher donne 2 cartes | Pioche, EPFL | a |
| Rare | Compréhension algébrique | 20 % des dégâts de sorts te soignent | Soin, Nature | a |
| Rare | Foie d'ingénieur | Gueule de bois à 5 Ivresse au lieu de 3 | Soirée | b (M1) |
| Rare | CMS | +50 % dégâts, mais l'adversaire aussi +25 % | EPFL | a |
| Légendaire | Paquet de cartes | Pioche non aléatoire : tu vois 3 cartes et en choisis une | Méta | b (M3) |
| Légendaire | Diplôme EPFL | +1 emplacement de trinket ; effet des trinkets EPFL doublé | EPFL | b (M4) |
| Légendaire | Ptit Sat ? | +10 % dégâts par point d'Ivresse | Soirée | b (M1) |
| Légendaire | Pétou ancestral | +8 PV par tour, mais toi et l'adversaire ne jouez qu'un tour sur deux | Soirée, Feu | b (M2) |
| Légendaire | George | *« À l'origine de la redistribution des biens communs. »* À chaque carte piochée, une autre carte de ta main (tirée au hasard) est remplacée par une carte aléatoire du pool | Soirée | a |
| Secret | 3 trinkets | voir `docs/SECRETS.md` (spoilers) | Méta | b (M5, M6, M7) |

#### Objets (consommables)
Raretés marquées *(proposée)* : à valider.

| Rareté | Nom | Effet | Tags | Méca. |
|---|---|---|---|---|
| Commun | Maté | +10 PV | Soin | a |
| Commun *(proposée)* | Pichet vide | 15 dégâts à l'adversaire, tu perds 5 PV, +1 Ivresse | Soirée, Boisson | a + b (M1) |
| Commun *(proposée)* | Cafffeeeeinnnn | Pioche 2 cartes | EPFL | a |
| Commun *(proposée)* | Gaz du bat Inf | 20 dégâts d'un élément aléatoire | EPFL | a |
| Peu commun *(proposée)* | Spritz | +30 PV, +1 Ivresse | Soirée, Boisson | a + b (M1) |
| Peu commun *(proposée)* | Shot de Jäger | Prochain sort ×2, +1 Ivresse | Soirée, Boisson | b (M1, M2) |
| Peu commun *(proposée)* | Moscow Mule | Annule la prochaine attaque, +1 Ivresse | Soirée, Boisson | b (M1, M2) |
| Peu commun *(proposée)* | Raclette | +50 PV, mais tu sautes ton prochain tour | EPFL | b (M2) |
| Rare *(proposée)* | Space cake | Au bout de 3 tours : +40 PV et +50 % dégâts pendant 2 tours | Soirée | b (M2) |

#### Nouvelles mécaniques nécessaires
- **M1 — Ivresse** : compteur du joueur, remis à 0 à chaque fin de combat, seuil de Gueule de bois modifiable.
- **M2 — Effets temporaires** : « prochain sort ×2 », « annule la prochaine attaque », effet différé (dans N tours), bonus pendant N tours, tour sauté.
- **M3 — Pioche au choix** : le jeu propose N cartes, le joueur en choisit une.
- **M4 — Puissance des trinkets** : un multiplicateur d'effet par trinket (Diplôme EPFL).
- **M5 — Carte morte / mort instantanée** : carte « NO CARDS » injouable, adversaire tué sur le coup.
- **M6 — Annulation de tour** : revenir à l'état du début du tour précédent (faisable en rejouant la seed et les actions depuis le début).
- **M7 — Tirage avantageux** : tirer deux fois et garder le meilleur.
- **M8 — Combats et malédictions** : découper la partie en combats (aujourd'hui : un adversaire = un combat ?), malédictions qui durent jusqu'au combat suivant (Gueule de bois).

## Shop
- Monnaie : or gagné à chaque combat (plus en élite/boss).
- Vend : cartes, objets, trinkets. Services : retirer une carte, soin. **[À DÉCIDER]**
- Reroll payant, coût croissant.

## Secrets / easter eggs
- Trinkets « Secret » débloqués par conditions cachées (seed spéciale, combo de cartes, code clavier…).
- Garder la liste des secrets dans `docs/SECRETS.md` (spoilers !).
- Existant : terrain « EPFL » de type feu, heal « Petit pétou ».

## Interface (terminal ASCII, Lanterna)
Fait : écrans titre, règles, combat, fin de partie ; clavier direct (←/→ + Entrée, 1-7, P piocher, Q quitter avec confirmation) ; sprites ASCII des 3 adversaires ; barres de PV ; main en cadres ASCII colorés par élément ; aperçu de l'effet réel de la carte sélectionnée ; journal de combat ; animations (dégâts qui clignotent, chiffres flottants, tremblement/charge de l'ennemi, mort, arrivée, pioche).
Reste : carte de la run, shop, récompenses, affichage des trinkets, transitions d'écran.

- Plein écran, input direct au clavier (plus de « Appuie sur Entrée »).
- Écrans : titre, carte de la run, combat, shop, récompense, game over.
- Combat : sprite ASCII de l'ennemi, barres de PV, main de cartes dessinées en cadres ASCII, trinkets affichés, journal de combat.
- Animations : dégâts qui clignotent, chiffres flottants, tremblement de l'ennemi, transitions d'écran.
- Couleur de rareté et d'élément partout.

## Questions ouvertes
- Nom définitif du jeu (« Cardistry » affiché en attendant).
- **[À DÉCIDER]** Valeur des PV max de départ (100 provisoirement, stat `MAX_HP`).
- **[À DÉCIDER]** Obtention des objets/trinkets tant qu'il n'y a ni run ni shop (proposition : choisir 1 récompense parmi 3 après chaque adversaire vaincu).
- **[À DÉCIDER]** Utiliser un objet : action gratuite ou fin du tour ?
- **[À DÉCIDER]** Qu'est-ce qu'un « combat » (reset de l'Ivresse, Gueule de bois, Latte macchiato, Ctrl+Z) : un adversaire, ou la série des trois ?
- **[À DÉCIDER]** Diplôme EPFL : doubler s'applique-t-il aussi aux contreparties (CMS : adversaire +50 % ?) et aux effets entiers (Photocopieuse : 3 cartes ?) ?
- **[À DÉCIDER]** CMS : « +50 % dégâts » = sorts seulement, ou aussi objets et trinkets ?
- **[À DÉCIDER]** Gueule de bois quand la main est déjà pleine au début du combat : on distribue une carte de moins ?

### Tranchées
- ~~Tables d'éléments contradictoires~~ → sortilèges : table (×1.5 / ×0.5 / ×1) ; terrain : multiplie l'attaque adverse (×1.25 / ×0.75 / ×1). Voir « Multiplicateurs d'éléments ».
- ~~Le nouvel adversaire attaque-t-il le tour où il arrive ?~~ → Non.
- ~~Cumul des multiplicateurs de stats~~ → ils s'additionnent.
- ~~Soins sans plafond~~ → les soins sont plafonnés aux PV max, que des objets/trinkets peuvent augmenter.
- ~~Pioche infinie~~ → on la garde pour l'instant (doublons du registre = poids). Le vrai deck reste à décider (« Structure d'une run »).

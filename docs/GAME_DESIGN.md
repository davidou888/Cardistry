# Cardistry — Game Design

> Document vivant. Les éléments marqués **[À DÉCIDER]** sont ouverts : c'est Louis qui les tranche, pas Claude Code.

## Boucle de jeu (existant)
- Une partie = une suite de **combats** ; **un combat = un adversaire** (Hydre/feu, Sirène/eau, Goblin/nature).
- Chaque combat commence avec une **nouvelle main de 5 cartes** (la main précédente est défaussée). Taille max : 7.
- À chaque tour : jouer une carte **ou** piocher, puis l'adversaire attaque. Utiliser un objet est une **action gratuite** (pas de fin de tour, pas d'attaque adverse), possible avant de jouer ou piocher.
- Quand un adversaire meurt : choix d'**1 récompense parmi 3** (objet ou trinket), puis le combat suivant commence. Le nouvel adversaire **n'attaque pas** ce tour-là.
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
| Slots | 3 | 5 (+1 avec Diplôme EPFL) |
| Inventaire plein | le joueur remplace un objet existant ou refuse la récompense | idem |
| Obtention (en attendant run et shop) | récompense après chaque adversaire vaincu : 1 au choix parmi 3, tirés selon les poids de rareté, sans trinket déjà possédé | idem |
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

#### Bonus de set (décidés, étape 6)
Seuls les **trinkets équipés** comptent (pas les objets). Un trinket compte pour **chacun de ses tags**. Les paliers se **cumulent** (à 4, on a aussi ceux de 2 et 3). Recalculés à chaque changement d'inventaire ; notification quand un palier s'active ; détail dans l'inventaire (I). Noms provisoires.

| Set | 2 | 3 | 4 |
|---|---|---|---|
| EPFL « Cursus » | +10 % dégâts élémentaires | +1 taille de main | +25 PV max et +15 % dégâts élémentaires |
| Soirée « Before → Afterparty » | +1 clope au début de chaque combat (respecte le max du paquet) | Seuil de Gueule de bois +1 | Chaque boisson soigne aussi 5 PV |
| Soin | +15 % soins | — | — |
| Pioche | +1 carte dans la main de départ | — | — |

**Diplôme EPFL** (Légendaire, EPFL) : +1 emplacement de trinket, effet des trinkets EPFL ×2, bonus de set EPFL ×2 ; il compte lui-même dans le set EPFL. Effet boule de neige **voulu**.
- Si le Diplôme est remplacé alors que 6 trinkets sont équipés, les 6 sont gardés (au-dessus de la limite) ; il faudra en remplacer un pour en ajouter un autre.
- Choix provisoires : « effet ×2 » s'applique à tout l'effet, contrepartie comprise (CMS : +100 % / adversaire +50 % ; Photocopieuse : 3 cartes ; Latte macchiato vanille : +2 cartes ; M1 et Plan du campus : 10) ; le Diplôme ne se double pas lui-même (+1 emplacement).

### Ivresse
- Chaque **boisson** (objet tagué Boisson) donne **+1 Ivresse**. L'Ivresse retombe à 0 après chaque combat.
- À **3 Ivresse** → malédiction **« Gueule de bois »** : −1 taille de main au prochain combat.
- Modifiée par : Foie d'ingénieur (seuil à 5), Ptit Sat ? (+10 % dégâts par point d'Ivresse).
- Implémentation (étape 3) : la Gueule de bois s'active au début du combat suivant et dure tout ce combat ; une seule par combat (l'Ivresse continue de monter au-delà du seuil) ; aucune pendant le dernier combat (il n'y a pas de suivant).
- ⚠️ Équilibrage : avec 3 adversaires et 1 récompense par combat, on ne peut pas avoir 3 boissons avant le dernier combat. La Gueule de bois (et donc Foie d'ingénieur) n'a aucun effet tant que les runs ne sont pas plus longues ou que le shop n'existe pas ; Ptit Sat ? plafonne à +20 % en pratique.

### Clopes
- Ressource de run : **5 clopes** au début de chaque partie, conservées entre les combats, **20 maximum** (un paquet ; stat `CIGARETTE_MAX`).
- **Fumer (F)** : action gratuite. −5 PV (ignorent Polycopié d'Analyse I, peuvent tuer), puis réduit la Gueule de bois de 1. On peut fumer sans Gueule de bois.
- **Rouler (G)** : remplace l'action du tour (comme piocher) : +1 clope, puis l'adversaire attaque. Impossible si le paquet est plein.
- La **Gueule de bois a une intensité** (1 par défaut = −1 taille de main). Chaque clope retire 1 d'intensité ; à 0, elle disparaît. Si elle est seulement « à venir », la clope la réduit à l'avance. Si elle est à la fois active et à venir, la clope réduit d'abord celle du combat en cours.
- Affichage : un petit paquet ASCII en bas de l'écran, avec le nombre de clopes et les touches.

### Contenu — premier jet
Implémentés à ce jour — étape 6 : bonus de set, Diplôme EPFL. Étape 5 : clopes, Googz, Honoré, Ross. Étape 4 : Caïpirinha, Petit pétou (objets), Shot de Jäger, Moscow Mule, Raclette, Space cake, Pétou ancestral. Étape 3 : Spritz, Pichet vide, Foie d'ingénieur, Ptit Sat ?. Étape 2 (tout ce qui est **a**) : 14 trinkets (Redistribution des briquets, Pichet de Sat, Plante du Rolex, Polycopié d'Analyse I, Latte macchiato vanille, M1, Pax, Monde Snack, Grinder, Plan du campus, Photocopieuse, Compréhension algébrique, CMS, George) et 3 objets (Maté, Cafffeeeeinnnn, Gaz du bat Inf). Le reste suit l'ordre de « Nouvelles mécaniques ».

Précisions d'implémentation :
- Les soins des objets et trinkets passent par l'efficacité des soins ; Grinder ajoute +50 % aux cartes de soin feu en plus.
- Polycopié d'Analyse I ne réduit que les attaques adverses (pas les dégâts qu'on s'inflige soi-même), jamais sous 0.
- Compréhension algébrique ne compte que les dégâts des sorts joués (pas ceux des objets ni de Plan du campus).
- Plan du campus et les dégâts « bruts » des objets ignorent éléments et stats ; Gaz du bat Inf est traité comme un sort de l'élément tiré.
- George ne se déclenche que sur les pioches (pas sur la main distribuée en début de combat).
- Pichet vide : les 5 PV perdus ignorent Polycopié d'Analyse I et peuvent tuer le joueur ; si le joueur et l'adversaire meurent en même temps, c'est une défaite.
- Ptit Sat ? : même portée que CMS (dégâts élémentaires), recalculé à chaque changement d'Ivresse.

Colonne **Méca.** : **a** = faisable avec les mécaniques existantes (stats, hooks sur les événements actuels, dégâts / soins / pioche / terrain) ; **b** = demande une nouvelle mécanique (voir « Nouvelles mécaniques »).

#### Trinkets
| Rareté | Nom | Effet | Tags | Méca. |
|---|---|---|---|---|
| Commun | Redistribution des briquets | +25 % dégâts feu | Feu, Soirée | a |
| Commun | Pichet de Sat | +25 % dégâts eau | Eau, EPFL | a |
| Commun | Plante du Rolex | +25 % dégâts nature | Nature, EPFL | a |
| Commun | Polycopié d'Analyse I | −2 dégâts à chaque attaque subie | Défense, EPFL | a |
| Commun | Honoré | Jauge Honoré : +1 par tour joué ; à 5, une clope gratuite et retour à 0. La jauge persiste entre les combats. Paquet plein : la jauge reste à 5 (« clope prête ») et la clope arrive dès qu'une place se libère (validé) | Soirée | b (M9) |
| Commun | Latte macchiato vanille | +1 carte au premier tour de chaque combat | Pioche, EPFL | a (+ notion de combat, M8) |
| Peu commun | Googz | Quand tu fumes, 20 % de chances que la clope se transforme en Petit pétou : effet de l'objet Petit pétou (+15 PV soin feu, −1 Ivresse) **à la place** des −5 PV ; réduit quand même la Gueule de bois | Soirée | b (M9) |
| Peu commun | M1 | Changer de terrain rend 5 PV | Terrain, EPFL | a |
| Peu commun | Pax | +1 taille de main | Pioche, Soirée | a |
| Peu commun | Monde Snack | +10 PV à chaque adversaire vaincu | Soin, Soirée | a |
| Peu commun | Grinder | Soins de type feu +50 % ; Petit pétou deux fois plus fréquent dans la pioche | Soin, Soirée | a |
| Peu commun | Plan du campus | Jouer un terrain inflige aussi 5 dégâts | Terrain, EPFL | a |
| Rare | Photocopieuse | Piocher donne 2 cartes | Pioche, EPFL | a |
| Rare | Compréhension algébrique | 20 % des dégâts de sorts te soignent | Soin, Nature | a |
| Rare | Foie d'ingénieur | Gueule de bois à 5 Ivresse au lieu de 3 | Soirée | b (M1) |
| Rare | CMS | +50 % dégâts, mais l'adversaire aussi +25 % | EPFL | a |
| Légendaire | Ross | Tous les soins ×2 (multiplicateur final, après tous les autres bonus de soin) ; les soins ne sont plus plafonnés par les PV max | Soin | b (M9) |
| Légendaire | Paquet de cartes | Pioche non aléatoire : tu vois 3 cartes et en choisis une | Méta | b (M3) |
| Légendaire | Diplôme EPFL | +1 emplacement de trinket ; effet des trinkets EPFL doublé (effet boule de neige **voulu**, voir plus bas) | EPFL | b (M4) |
| Légendaire | Ptit Sat ? | +10 % dégâts par point d'Ivresse | Soirée | b (M1) |
| Légendaire | Pétou ancestral | +10 PV par tour, mais tu ne joues qu'un tour sur deux (l'adversaire attaque normalement) | Soirée, Feu | b (M2) |
| Légendaire | George | *« À l'origine de la redistribution des biens communs. »* À chaque carte piochée, une autre carte de ta main (tirée au hasard) est remplacée par une carte aléatoire du pool | Soirée | a |
| Secret | 3 trinkets | voir `docs/SECRETS.md` (spoilers) | Méta | b (M5, M6, M7) |

#### Objets (consommables)
| Rareté | Nom | Effet | Tags | Méca. |
|---|---|---|---|---|
| Commun | Maté | +10 PV | Soin | a |
| Commun | Pichet vide | 15 dégâts à l'adversaire, tu perds 5 PV, +1 Ivresse | Soirée, Boisson | a + b (M1) |
| Commun | Caïpirinha | +20 PV, +1 Ivresse | Soirée, Boisson | a + b (M1) |
| Commun | Petit pétou | +15 PV, −1 Ivresse (minimum 0). Compte comme un soin feu (boosté par Grinder) | Soirée | a + b (M1) |
| Commun | Cafffeeeeinnnn | Pioche 2 cartes | EPFL | a |
| Commun | Gaz du bat Inf | 20 dégâts d'un élément aléatoire | EPFL | a |
| Peu commun | Spritz | +30 PV, +1 Ivresse | Soirée, Boisson | a + b (M1) |
| Peu commun | Shot de Jäger | Prochain **sort** ×2, +1 Ivresse. Actif jusqu'à ce qu'un sort soit joué, perdu à la fin du combat | Soirée, Boisson | b (M1, M2) |
| Peu commun | Moscow Mule | Annule la prochaine attaque adverse, +1 Ivresse. Perdu à la fin du combat s'il n'a pas servi | Soirée, Boisson | b (M1, M2) |
| Peu commun | Raclette | +50 PV, puis tu sautes ton prochain tour (l'adversaire attaque normalement) | EPFL | b (M2) |
| Rare | Space cake | Au bout de 3 tours : +40 PV et +50 % dégâts pendant 2 tours. Si le combat se termine avant le déclenchement, le compte à rebours continue au combat suivant | Soirée | b (M2) |

Les cartes Soin « Caïpirinha » et « Petit pétou » existent aussi : objet et carte gardent le même nom, mais sont toujours distingués à l'écran (voir Interface) et ont des identifiants distincts.

#### Choix assumés
- **Diplôme EPFL** fait boule de neige avec les trinkets EPFL et double aussi le bonus de set EPFL : c'est voulu (synergies façon *The Binding of Isaac*).
- Les soins restent plafonnés aux PV max, même si ça gaspille une partie des gros soins — **sauf avec Ross**.
- **Ross + Pétou ancestral** : +20 PV par tour sans plafond, presque invincible. Voulu (synergie façon Isaac), comme Diplôme EPFL.
- Ross : ×2 appliqué **après** tous les autres bonus de soin (multiplicateur final, pas +100 % additif). Les PV peuvent dépasser le max (affichés en doré) ; les PV max ne bougent pas. Si on perd Ross, les PV en trop sont gardés mais les soins suivants sont de nouveau plafonnés.

#### Nouvelles mécaniques nécessaires
- **M1 — Ivresse** : compteur du joueur, remis à 0 à chaque fin de combat, seuil de Gueule de bois modifiable.
- **M2 — Effets temporaires** : système générique (comme les malédictions) : délai avant déclenchement, durée en tours et/ou en déclenchements (charges), perdu ou gardé à la fin du combat, modificateurs de stats pendant l'effet, effets possibles : multiplier le prochain sort, annuler la prochaine attaque, faire sauter un tour. Affichés à l'écran avec leur durée restante.
  - Raclette + Pétou ancestral : Raclette fait sauter *le* tour suivant ; si Pétou ancestral le faisait déjà sauter, les deux se chevauchent (pas de tour perdu en plus).
  - Choix provisoires : deux Shot de Jäger ne se cumulent pas sur le même sort (un par sort) ; Raclette est perdue si le combat se termine avant le tour sauté ; Pétou ancestral fait jouer les tours impairs de chaque combat et sauter les pairs, +10 PV au début de chaque tour (joué ou sauté) ; Space cake utilisé au tour T se déclenche au début du tour T+3, et ses +50 % (dégâts élémentaires, comme CMS) valent pour les tours T+3 et T+4 ; un tour sauté compte comme un tour ; Petit pétou (objet) se comporte comme une carte de soin feu (terrain, efficacité des soins, Grinder).
- **M3 — Pioche au choix** : le jeu propose N cartes, le joueur en choisit une.
- **M4 — Puissance des trinkets** : un multiplicateur d'effet par trinket (Diplôme EPFL).
- **M5 — Carte morte / mort instantanée** : carte « NO CARDS » injouable, adversaire tué sur le coup.
- **M6 — Annulation de tour** : revenir à l'état du début du tour précédent (faisable en rejouant la seed et les actions depuis le début).
- **M7 — Tirage avantageux** : tirer deux fois et garder le meilleur.
- **M9 — Clopes** : ressource de run, actions Fumer / Rouler, intensité de malédiction, multiplicateur final de stat (Ross), soins non plafonnés.
- **M8 — Combats et malédictions** : la partie est découpée en combats (un par adversaire) ; malédictions qui durent jusqu'au combat suivant (Gueule de bois).

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
Fait aussi : sets dans l'inventaire (palier atteint, prochain palier, ×2 si amplifié) et bandeau « ★ Set … activé » ; trinkets amplifiés marqués « (effet ×2) » ; paquet de clopes (petit sprite ASCII en bas à droite, n/20, touches F fumer / G rouler, jauge Honoré) ; PV au-dessus du max en doré (Ross) ; transformation Googz affichée (journal + texte flottant) ; effets temporaires affichés avec leur durée restante (ligne au-dessus du terrain, liste complète dans l'inventaire) ; objets et cartes homonymes (Caïpirinha, Petit pétou) distingués à l'écran (« la carte … » / « l'objet … » dans le journal, couleur d'élément pour les cartes, de rareté pour les objets) ; écran de récompense (cartes côte à côte, couleur de rareté), écran de remplacement si inventaire plein, inventaire (touche I), objets utilisables avec W/E/R (clavier QWERTZ, action gratuite), trinkets affichés en combat.
Reste : carte de la run, shop, transitions d'écran.

- Plein écran, input direct au clavier (plus de « Appuie sur Entrée »).
- Écrans : titre, carte de la run, combat, shop, récompense, game over.
- Combat : sprite ASCII de l'ennemi, barres de PV, main de cartes dessinées en cadres ASCII, trinkets affichés, journal de combat.
- Animations : dégâts qui clignotent, chiffres flottants, tremblement de l'ennemi, transitions d'écran.
- Couleur de rareté et d'élément partout.

## Questions ouvertes
- Nom définitif du jeu (« Cardistry » affiché en attendant).
- **[À DÉCIDER]** Valeur des PV max de départ (100 provisoirement, stat `MAX_HP`).
- **[À DÉCIDER]** Diplôme EPFL : contreparties doublées (CMS +100 % / adversaire +50 %), Photocopieuse 3 cartes, le Diplôme ne se double pas lui-même — ok ? Perdre le Diplôme avec 6 trinkets : on les garde tous — ok ?
- **[À DÉCIDER]** CMS : « +50 % dégâts » = sorts seulement, ou aussi objets et trinkets ? *Provisoire : +50 % sur les dégâts élémentaires (sorts et Gaz du bat Inf), pas sur les dégâts bruts (Plan du campus, Pichet vide).*

### Tranchées
- ~~Tables d'éléments contradictoires~~ → sortilèges : table (×1.5 / ×0.5 / ×1) ; terrain : multiplie l'attaque adverse (×1.25 / ×0.75 / ×1). Voir « Multiplicateurs d'éléments ».
- ~~Le nouvel adversaire attaque-t-il le tour où il arrive ?~~ → Non.
- ~~Cumul des multiplicateurs de stats~~ → ils s'additionnent.
- ~~Soins sans plafond~~ → les soins sont plafonnés aux PV max, que des objets/trinkets peuvent augmenter.
- ~~Obtention des objets/trinkets~~ → 1 récompense au choix parmi 3 après chaque adversaire vaincu ; inventaire plein → remplacer ou refuser.
- ~~Slots~~ → 3 objets, 5 trinkets.
- ~~Utiliser un objet~~ → action gratuite.
- ~~Qu'est-ce qu'un combat~~ → un adversaire. Chaque combat commence avec une nouvelle main de 5 cartes.
- ~~Gueule de bois : combien, quand~~ → une seule par combat, aucune pendant le dernier combat.
- ~~Ptit Sat ?~~ → dégâts élémentaires, comme CMS.
- ~~Pichet vide mortel~~ → peut tuer le joueur ; mort simultanée = défaite.
- ~~Gueule de bois avec une main pleine~~ → sans objet : la main de départ (5) reste sous la taille max (6 avec la malédiction).
- ~~Pioche infinie~~ → on la garde pour l'instant (doublons du registre = poids). Le vrai deck reste à décider (« Structure d'une run »).

# Cardistry — Game Design

> Document vivant. Les éléments marqués **[À DÉCIDER]** sont ouverts : c'est Louis qui les tranche, pas Claude Code.

## Boucle de jeu (existant)
- Une partie = une suite de **combats** ; **un combat = un adversaire** (voir « Adversaires »).
- Chaque combat commence avec une **nouvelle main de 5 cartes** (la main précédente est défaussée). Taille max : 7.
- À chaque tour : jouer une carte **ou** piocher, puis l'adversaire attaque. Utiliser un objet est une **action gratuite** (pas de fin de tour, pas d'attaque adverse), possible avant de jouer ou piocher.
- Quand un adversaire meurt : Choomies, puis choix d'**1 récompense parmi 3** (objet ou trinket), puis retour à la carte. Chaque combat commence par ton tour.
- Cartes : **Sortilège** (dégâts), **Heal** (soin, boosté dans le terrain du même type), **Terrain** (change le terrain, modifie l'attaque adverse).
- Éléments : **Feu > Plante > Eau > Feu**. « Plante » est le nom affiché de l'élément nature (l'enum du code reste `NATURE`).
- **Neutre** (adversaires seulement) : ×1 contre tout sort, et les terrains n'ont aucun effet sur son attaque.

### Multiplicateurs d'éléments (décidés)
Source de vérité dans le code : `cardistry.model.ElementRules` (testé).

**Sortilège → dégâts infligés**

| Sort \ Adversaire | Feu | Eau | Plante |
|---|---|---|---|
| Feu | ×1 | ×0.5 | ×1.5 |
| Eau | ×1.5 | ×1 | ×0.5 |
| Plante | ×0.5 | ×1.5 | ×1 |

×1.5 si le sort bat l'adversaire, ×0.5 si l'adversaire bat le sort, ×1 si même élément.

**Terrain → attaque de l'adversaire** (le terrain modifie ce que l'adversaire *inflige*, pas ce qu'il reçoit)

| Terrain \ Adversaire | Feu | Eau | Plante |
|---|---|---|---|
| Feu | ×1.25 | ×1 | ×0.75 |
| Eau | ×0.75 | ×1.25 | ×1 |
| Plante | ×1 | ×0.75 | ×1.25 |

×1.25 sur un terrain de son élément, ×0.75 si le terrain bat son élément, ×1 sinon.

**Soin → PV rendus selon le terrain** : ×1.5 sur un terrain du même élément, ×0.75 si le terrain bat l'élément du soin, ×1 sinon.

## Adversaires
Registre : `cardistry.model.enemy.EnemyRegistry` (testé). Chaque adversaire a une liste d'attaques et
**annonce la prochaine** (intent, affiché avec les dégâts réels : niveau, terrain, réduction). Ses tirages
(Gamble, coup raté) ne sont **pas** doublés par CLAUDE.md.

**Scaling par niveau** : PV ×1 / ×1.4 / ×1.9, dégâts ×1 / ×1.25 / ×1.5 (les boss ont des PV fixés, leurs
dégâts suivent le niveau). Valeurs ci-dessous au niveau 1.

| Rang | Nom | Élément | PV | Attaques |
|---|---|---|---|---|
| Commun | Caddie énervé | Neutre | 28 | Roue voilée 6 ; tous les 3 tours, Charge de parking 12 (×2, annoncée) |
| Commun | Choomah | Plante | 22 | coup 4 ; tous les 3 tours, Vol à la tire : 5 Choomies, sinon 1 clope, sinon un coup. Attaques, descriptions et journal **criés** (« ooOOOaAAAOaAOOA ») ; les chiffres restent lisibles |
| Commun | Choomah pressé | Plante | 18 | coup 3 ; vole un tour sur deux |
| Commun | Choomah costaud | Plante | 30 | coup 6 ; vole tous les 3 tours |
| Commun | Camé | Feu | 25 | coup 5 ; un tour sur deux, Taxe : te prend 1 clope, ou deux coups de 5 si tu n'en as pas |
| Commun | Paresseux | Plante | 24 | accroché à sa branche, peu de dégâts : Griffe molle 3 ; un tour sur deux, « Badoing, badoing ! » : 50 % d'esquiver ton prochain sort, sinon 2 × 2 |
| Commun | Clarence | Neutre | 1 | « Clarence te regarde. » N'attaque jamais. 3 % des combats communs ; 75 Choomies |
| Commun | Seau de fuite | Eau | 26 | Ça goutte 2, 3, 4, 5 (le seau se remplit) ; au 5e tour Débordement 14, puis retour à zéro |
| Élite | Vafi | Feu | 55 | Correction 8 ; tous les 3 tours, Série d'algèbre : une carte de ta main devient « Exercice 3b » (injouable pour ce combat) ; sous 50 % PV, Pause pétou (+12 PV, n'attaque pas), 2 fois max |
| Élite | Le Peneu | Neutre | 90 | Rebond (passif) : renvoie 20 % des dégâts reçus, brut, sans jamais tuer ; Crevaison 6, +2 à chaque utilisation |
| Élite | T-Rex défoncé | Plante | 65 | Morsure 13 ; bras trop courts : 30 % de rater, et il se soigne de 8 quand il rate (fringale) |
| Élite | Étudiant post-examen | Eau | 60 | Larmes : 6, +3 par carte de moins que 5 dans ta main ; sous 50 % PV, Crise de nerfs : deux attaques par tour |
| Boss | Boss du niveau 1 / 2 / 3 (**noms à venir**) | Feu / Eau / Plante | 130 / 200 / 280 | Coup 10 ; toutes les 4 attaques, Grosse attaque 22 |

Règles communes :
- L'intent est choisi au début du combat et après chaque attaque. Un tour sauté (Pétou ancestral) garde
  l'intent et n'avance pas les compteurs (Seau, Crevaison).
- **Moscow Mule** annule toute l'attaque (tous ses coups), mais pas les vols ni la Taxe (qui ne sont pas
  des attaques ; la Taxe sans clope, elle, frappe et s'annule).
- Esquive : seul le prochain **sort** est esquivé (un Shot de Jäger actif est consommé quand même).
- Cartes mortes (« NO CARDS », « Exercice 3b ») : injouables jusqu'à la fin du combat, jamais remplacées.
- Choomah : variante tirée au hasard ; deux Choomah ne se suivent pas, ni deux fois le même commun.

## Structure d'une run
- **3 niveaux** (sans nom) de **8 étages** chacun, boss compris : étages 1-6 au choix, 7 = Sieste, 8 = Boss.
  Un boss différent à la fin de chaque niveau ; battre celui du niveau 3 gagne la partie.
- Carte à **embranchements** façon Slay the Spire, générée par un **sous-flux nommé de la seed**
  (`Rng.fork("carte-<niveau>")`) : même seed = même carte, quels que soient les autres tirages. Les
  adversaires sont tirés dans un autre sous-flux (`rencontres-<niveau>`). Code : `run.map.MapGenerator`.
- Génération : 5 colonnes, 4 chemins qui montent d'au plus une colonne par étage ; ils se rejoignent
  mais ne se croisent jamais en X ; au moins deux départs.
- Types de nœuds : Combat `C`, Élite `E`, Shop `$`, Événement `?`, Sieste `Z`, Boss `B`.

| Étage | Contenu |
|---|---|
| 1 | Combat uniquement |
| 2-3 | Combat 60 %, Événement 25 %, Shop 15 % (pas d'élite ni de sieste) |
| 4-6 | Combat 45 %, Événement 22 %, Élite 15 %, Shop 10 %, Sieste 8 % (pas de sieste à l'étage 6) |
| 7 | Sieste partout |
| 8 | Boss |

- Contraintes : jamais deux Shop, deux Élites ou deux Siestes à la suite sur un chemin ; au moins un
  Shop par carte ; au moins un chemin sans élite.
- Écran de carte (fenêtre) : étage 1 à gauche, boss à droite ; icônes pixel art, chemins en
  pointillés courbes et nœuds décalés tirés du sous-flux `Rng.fork("carte-dessin-<niveau>")` (le
  dessin ne consomme aucun tirage de la carte) ; chemin parcouru en or, nœuds devenus inatteignables
  estompés. ←/→ ↑/↓ (ou 1-n) pour choisir, Entrée pour y aller, I inventaire.
- Terminal (`--terminal`, `--no-images`) : carte en lettres, boss en haut, étage 1 en bas. Mode
  texte : la carte en ASCII, les choix numérotés.
- Nœud `?` : un **événement** (voir « Événements »).
- **Sieste** (étage 7, parfois aux étages 4-5) : un choix parmi « Dormir » (+30 % des PV max ; c'est un
  **soin** : efficacité des soins et Ross s'appliquent, sans plafond avec Ross), « Rouler tranquille » (+3 clopes, sans dépasser le paquet), « Faire le tri » (retirer une
  carte du deck, avec le deck construit). Elle n'efface **pas** la Gueule de bois (les clopes sont là pour ça).
- **Récompenses** : 1 parmi 3 après chaque combat ; élite : toutes au moins Peu commun ; boss : toutes au
  moins Rare (les secrets débloqués restent possibles).
- **Après chaque boss** (sauf le dernier) : la moitié des PV manquants est rendue — une **récupération**,
  pas un soin (aucun bonus de soin, pas de Ross) —, puis le niveau suivant commence.
- Mort = fin de la run. Méta-progression : seulement les secrets débloqués (voir `docs/SECRETS.md`).

### Deck construit (lot 1 bis)
- **Deck de départ, 12 cartes** : 6 sorts (Boule de feu 10, Pluie de flammes 15, Jet d'eau 10, Tsunami 15,
  Fouet de lianes 10, Tempête d'épines 15), 3 soins (Potion, Caïpirinha, Petit pétou), 3 terrains (Enfer,
  Atlantis, Amazonie). Le terrain de départ (EPFL) est en jeu, pas dans le deck.
- Au début de chaque combat, tout le deck est mélangé en une **pioche** ; la main de 5 en est tirée.
  Les cartes jouées, remplacées (George) ou transformées (NO CARDS, Exercice 3b) vont à la **défausse** ;
  les cartes mortes, elles, disparaissent à la fin du combat. Pioche vide : la défausse est remélangée.
  Pioche et défausse vides : piocher est refusé (« Plus aucune carte à piocher »).
- **Récompense de carte** après chaque combat (sauf le boss final), **après** la récompense d'objet :
  3 cartes de noms différents tirées du registre (poids des trinkets compris), on en ajoute 1 au deck ou
  on passe (en plus du 1 parmi 3 d'objets).
- **Retirer une carte** : Sieste « Faire le tri » (gratuit) ou shop (payant) ; jamais sous **5 cartes**.
- Grinder : +1 Petit pétou dans le deck à l'obtention, Petit pétou ×2 dans les récompenses de cartes (et
  pour George). Paquet de cartes : 3 cartes de la pioche, les 2 autres à la défausse. George : carte du
  registre (elle ne reste pas dans le deck).
- Écran de combat : « Ta main 5/7 · Pioche 7 · Défausse 0 » ; inventaire (I) : le deck.

## Personnages
Au début de chaque partie (après l'écran titre), on choisit un personnage. Sa capacité est permanente,
comme un trinket qu'on ne peut ni perdre ni remplacer. Code : `hero.HeroRegistry` (tous les chiffres).

| Personnage | PV | Capacité | Effet |
|---|---|---|---|
| **Louis** | 100 | MAN | Le premier sort de chaque combat inflige +50 % de dégâts (×1.5). Le malus de Gueule de bois est doublé : −2 taille de main par point d'intensité. |
| **Dave** | 100 | Full skill Mr. Le Juge | Un sort du même élément que le terrain **et** qui bat l'élément de l'adversaire (ex. Boule de feu à l'EPFL contre un adversaire plante) soigne des PV enlevés à l'adversaire. |
| **Sassy** | 130 | wadayatalkinabit | À chaque début de tour, son alien inflige 4 dégâts bruts à l'adversaire, en plus de l'action du joueur. 10 % de chance que l'alien rate son tour et fume un joint à la place (aucun effet, pour le lore). |

Choix d'implémentation :
- Louis : MAN est un effet « prochain sort ×1.5 » posé au début de chaque combat. Il se consomme au premier
  sort joué, même esquivé ; avec un Shot de Jäger, un seul bonus par sort (MAN d'abord). Une clope retire
  toujours 1 d'intensité, donc annule −2 d'un coup.
- Dave : le soin vaut les PV **réellement** enlevés (pas les dégâts en trop sur un adversaire achevé) ; rien
  si le sort est esquivé. *Provisoire : c'est un soin comme les autres (efficacité des soins, Ross, plafond
  des PV max).*
- Sassy : l'alien attaque aussi pendant un tour sauté et peut achever l'adversaire. Dégâts bruts (aucun
  multiplicateur, le Rebond du Peneu s'applique). Son raté n'est pas un tirage de chance : CLAUDE.md ne le
  double pas.

## Objets et trinkets
| | Objet | Trinket |
|---|---|---|
| Nature | Consommable, usage unique | Passif, actif toute la run |
| Slots | 3 | 5 (+1 avec Diplôme EPFL) |
| Inventaire plein | le joueur remplace un objet existant ou refuse la récompense | idem |
| Obtention (en attendant run et shop) | récompense après chaque adversaire vaincu : 1 au choix parmi 3, tirés selon les poids de rareté, sans trinket déjà possédé | idem |
| Exemple | Maté : +10 PV | Redistribution des briquets : +25 % dégâts feu |

### Effets sur les stats
Stats modifiables : PV max (plafond des soins), taille de main, cartes piochées, Choomies gagnés, multiplicateur par élément, efficacité des soins, réduction de dégâts. **[À DÉCIDER]** ajouter crit / esquive ?
Ordre de calcul : base → bonus plats → pourcentages. Les pourcentages **s'additionnent** : deux trinkets +25 % feu = +50 % (×1.5), pas ×1.5625. Formule : `(base + plats) × (1 + somme des %)`, plancher à 0.

### Rareté
La couleur de rareté est la même partout : texte en terminal ; dans la fenêtre, cadre de l'icône en combat, dans les récompenses et au shop. La couleur d'élément (feu rouge, eau bleu, plante vert, neutre gris) est réservée aux cadres de cartes.

| Rareté | Couleur | Poids drop (indicatif) | Prix shop (indicatif) |
|---|---|---|---|
| Commun | blanc | 60 | 50 |
| Peu commun | vert | 25 | 80 |
| Rare | bleu | 12 | 130 |
| Légendaire | jaune | 3 | 200 |
| Secret | magenta | seulement une fois débloqué (poids 2), voir `docs/SECRETS.md` | — |

### Synergies
- Chaque objet/trinket porte des **tags** (élément, archétype : Brûlure, Soin, Terrain, Pioche…).
- Synergies de set : 2/3/4 trinkets du même tag → bonus croissant.
- Synergies directes : un trinket qui modifie l'effet d'un autre (ex. « tes trinkets Feu affectent aussi les Heals »).
- Tags utilisés par le premier jet : **EPFL**, **Soirée**, **Boisson**, **Méta**, éléments (Feu, Eau, Plante), archétypes (Soin, Pioche, Terrain, Défense).

#### Bonus de set (décidés, étape 6)
Seuls les **trinkets équipés** comptent (pas les objets). Un trinket compte pour **chacun de ses tags**. Les paliers se **cumulent** (à 4, on a aussi ceux de 2 et 3). Recalculés à chaque changement d'inventaire ; notification quand un palier s'active ; détail dans l'inventaire (I). Noms provisoires.

| Set | 2 | 3 | 4 |
|---|---|---|---|
| EPFL « Cursus » | +10 % dégâts élémentaires | +1 taille de main | +25 PV max et +15 % dégâts élémentaires |
| Soirée « Before → Afterparty » | +1 clope au début de chaque combat (respecte le max du paquet) | Seuil de Gueule de bois +1 | Chaque boisson soigne aussi 5 PV |
| Soin | +15 % soins | — | — |
| Pioche | +1 carte dans la main de départ | — | — |

**Diplôme EPFL** (Légendaire, EPFL) : +1 emplacement de trinket, effet des trinkets EPFL ×2, bonus de set EPFL ×2 ; il compte lui-même dans le set EPFL. Effet boule de neige **voulu**.
- **Tout l'effet des trinkets EPFL est doublé, malus compris** (CMS : +100 % / adversaire +50 % ; Photocopieuse : 3 cartes ; Latte macchiato vanille : +2 cartes ; M1 et Plan du campus : 10). Le Diplôme ne se double pas lui-même (+1 emplacement seulement).
- Si on perd le Diplôme avec 6 trinkets équipés, le joueur doit **immédiatement choisir un trinket à jeter** (écran de choix, pas de tirage au hasard) — sinon c'est exploitable.

### Ivresse
- Chaque **boisson** (objet tagué Boisson) donne **+1 Ivresse**. L'Ivresse retombe à 0 après chaque combat.
- **Paliers** (`core.DrunkTier`) : l'Ivresse donne un bonus de dégâts élémentaires (comme CMS, additionné aux autres pourcentages) tant qu'elle dure :

| Ivresse | Palier | Effet |
|---|---|---|
| 1 | Pompette | +10 % dégâts |
| 2 jusqu'au seuil | Bourré | +25 % dégâts |
| seuil (3) et plus | Cuite | +50 % dégâts, et **Gueule de bois** au combat suivant (−1 taille de main ; −2 pour Louis) |
- Modifiée par : Foie d'ingénieur (Cuite et Gueule de bois à 5 : Bourré de 2 à 4), Ptit Sat ? (+10 % dégâts par point d'Ivresse, **en plus** du palier), la clope (−1 Ivresse).
- Implémentation (étape 3) : la Gueule de bois s'active au début du combat suivant (quels que soient les nœuds entre les deux : Sieste, shop…) et dure tout ce combat ; une à chaque fois qu'on franchit le seuil vers le haut (l'Ivresse continue de monter au-delà, sans nouvelle Gueule de bois ni bonus supplémentaire) ; aucune pendant le boss final (il n'y a pas de suivant).
- Avec la carte (environ 18 combats par run), la Gueule de bois, Foie d'ingénieur et Ptit Sat ? deviennent réellement utiles.

### Clopes
- Ressource de run : **5 clopes** au début de chaque partie, conservées entre les combats, **20 maximum** (un paquet ; stat `CIGARETTE_MAX`).
- **Fumer (F)** : action gratuite. **+5 PV** (un soin : efficacité des soins, Ross, plafond des PV max) et **−1 Ivresse** (le palier redescend donc : on échange des dégâts contre des PV). On peut fumer à jeun.
- **Rouler (G)** : remplace l'action du tour (comme piocher) : +1 clope, puis l'adversaire attaque. Impossible si le paquet est plein.
- La **Gueule de bois a une intensité** (1 par défaut = −1 taille de main). Chaque clope retire 1 d'intensité ; à 0, elle disparaît. Si elle est seulement « à venir », la clope la réduit à l'avance. Si elle est à la fois active et à venir, la clope réduit d'abord celle du combat en cours. Exception : tant que l'Ivresse reste au seuil ou au-dessus après la clope, la Gueule de bois à venir n'est pas réduite (on ne garde pas la Cuite sans la payer) ; repasser le seuil vers le haut en redonne une.
- Affichage : un petit paquet ASCII en bas de l'écran, avec le nombre de clopes et les touches.

### Contenu — premier jet
Implémentés à ce jour — étape 7 : Paquet de cartes (pioche au choix), défausse obligatoire quand on perd le Diplôme EPFL avec 6 trinkets. Étape 6 : bonus de set, Diplôme EPFL. Étape 5 : clopes, Googz, Honoré, Ross. Étape 4 : Caïpirinha, Petit pétou (objets), Shot de Jäger, Moscow Mule, Raclette, Space cake, Pétou ancestral. Étape 3 : Spritz, Pichet vide, Foie d'ingénieur, Ptit Sat ?. Étape 2 (tout ce qui est **a**) : 14 trinkets (Redistribution des briquets, Pichet de Sat, Plante du Rolex, Polycopié d'Analyse I, Latte macchiato vanille, M1, Pax, Monde Snack, Grinder, Plan du campus, Photocopieuse, Compréhension algébrique, CMS, George) et 3 objets (Maté, Cafffeeeeinnnn, Gaz du bat Inf). Le reste suit l'ordre de « Nouvelles mécaniques ».

Précisions d'implémentation :
- Les soins des objets et trinkets passent par l'efficacité des soins ; Grinder ajoute +50 % aux cartes de soin feu en plus.
- Polycopié d'Analyse I ne réduit que les attaques adverses (pas les dégâts qu'on s'inflige soi-même), jamais sous 0.
- Compréhension algébrique ne compte que les dégâts des sorts joués (pas ceux des objets ni de Plan du campus).
- Plan du campus et les dégâts « bruts » des objets ignorent éléments et stats ; Gaz du bat Inf est traité comme un sort de l'élément tiré.
- George ne se déclenche que sur les pioches (pas sur la main distribuée en début de combat). La carte remplacée part à la défausse ; la nouvelle vient du registre et ne rejoint pas le deck.
- Pichet vide : les 5 PV perdus ignorent Polycopié d'Analyse I et peuvent tuer le joueur ; si le joueur et l'adversaire meurent en même temps, c'est une défaite.
- Ptit Sat ? : même portée que CMS (dégâts élémentaires), recalculé à chaque changement d'Ivresse.

Colonne **Méca.** : **a** = faisable avec les mécaniques existantes (stats, hooks sur les événements actuels, dégâts / soins / pioche / terrain) ; **b** = demande une nouvelle mécanique (voir « Nouvelles mécaniques »).

#### Trinkets
| Rareté | Nom | Effet | Tags | Méca. |
|---|---|---|---|---|
| Commun | Redistribution des briquets | +25 % dégâts feu | Feu, Soirée | a |
| Commun | Pichet de Sat | +25 % dégâts eau | Eau, EPFL | a |
| Commun | Plante du Rolex | +25 % dégâts plante | Plante, EPFL | a |
| Commun | Polycopié d'Analyse I | −2 dégâts à chaque attaque subie | Défense, EPFL | a |
| Commun | Honoré | Jauge Honoré : +1 par tour joué ; à 5, une clope gratuite et retour à 0. La jauge persiste entre les combats. Paquet plein : la jauge reste à 5 (« clope prête ») et la clope arrive dès qu'une place se libère (validé) | Soirée | b (M9) |
| Commun | Latte macchiato vanille | +1 carte au premier tour de chaque combat | Pioche, EPFL | a (+ notion de combat, M8) |
| Peu commun | Googz | Quand tu fumes, 20 % de chances que la clope se transforme en Petit pétou : effet de l'objet Petit pétou (+15 PV soin feu, −1 Ivresse) **à la place** des +5 PV ; réduit quand même la Gueule de bois | Soirée | b (M9) |
| Peu commun | M1 | Changer de terrain rend 5 PV | Terrain, EPFL | a |
| Peu commun | Pax | +1 taille de main | Pioche, Soirée | a |
| Peu commun | Monde Snack | +10 PV à chaque adversaire vaincu | Soin, Soirée | a |
| Peu commun | Grinder | Soins de type feu +50 % ; Petit pétou deux fois plus fréquent dans la pioche | Soin, Soirée | a |
| Peu commun | Plan du campus | Jouer un terrain inflige aussi 5 dégâts | Terrain, EPFL | a |
| Rare | Photocopieuse | Piocher donne 2 cartes | Pioche, EPFL | a |
| Rare | Compréhension algébrique | 20 % des dégâts de sorts te soignent | Soin, Plante | a |
| Rare | Foie d'ingénieur | Cuite et Gueule de bois à 5 Ivresse au lieu de 3 | Soirée | b (M1) |
| Rare | CMS | +50 % dégâts, mais l'adversaire aussi +25 % | EPFL | a |
| Légendaire | Ross | Tous les soins ×2 (multiplicateur final, après tous les autres bonus de soin) ; les soins ne sont plus plafonnés par les PV max | Soin | b (M9) |
| Légendaire | Paquet de cartes | Chaque carte piochée pendant un combat devient un choix : 3 cartes tirées du pool, on en garde 1, les 2 autres sont défaussées | Méta | b (M3) |
| Légendaire | Diplôme EPFL | +1 emplacement de trinket ; effet des trinkets EPFL doublé (effet boule de neige **voulu**, voir plus bas) | EPFL | b (M4) |
| Légendaire | Ptit Sat ? | +10 % dégâts par point d'Ivresse, en plus du bonus d'Ivresse | Soirée | b (M1) |
| Légendaire | Pétou ancestral | À chaque combat gagné : +8 PV (soin feu : terrain, Grinder, ×2 avec Ross). Au début de chaque combat, l'adversaire saute son premier tour (« [Adversaire] passe son tour : tu as fait tourner le j. ») | Soirée, Feu | b (M2) |
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
- Ross : ×2 appliqué **après** tous les autres bonus de soin (multiplicateur final, pas +100 % additif). Les PV peuvent dépasser le max (affichés en doré) ; les PV max ne bougent pas. Si on perd Ross, les PV en trop sont gardés mais les soins suivants sont de nouveau plafonnés.

#### Nouvelles mécaniques nécessaires
- **M1 — Ivresse** : compteur du joueur, remis à 0 à chaque fin de combat, seuil de Gueule de bois modifiable.
- **M2 — Effets temporaires** : système générique (comme les malédictions) : délai avant déclenchement, durée en tours et/ou en déclenchements (charges), perdu ou gardé à la fin du combat, modificateurs de stats pendant l'effet, effets possibles : multiplier le prochain sort, annuler la prochaine attaque, faire sauter un tour. Affichés à l'écran avec leur durée restante.
  - Tour sauté de l'adversaire (Pétou ancestral) : vérifié avant les annulations d'attaque, donc un Moscow Mule reste pour l'attaque suivante. L'effet est posé au début du combat : équiper Pétou ancestral en cours de combat ne fait effet qu'au combat suivant.
  - Choix provisoires : deux Shot de Jäger ne se cumulent pas sur le même sort (un par sort) ; Raclette est perdue si le combat se termine avant le tour sauté ; Space cake utilisé au tour T se déclenche au début du tour T+3, et ses +50 % (dégâts élémentaires, comme CMS) valent pour les tours T+3 et T+4 ; un tour sauté compte comme un tour ; Petit pétou (objet) se comporte comme une carte de soin feu (terrain, efficacité des soins, Grinder).
- **M3 — Pioche au choix** (Paquet de cartes, étape 7) : chaque carte piochée pendant un combat (action piocher, Photocopieuse, Cafffeeeeinnnn, Latte macchiato vanille…) devient un choix de 3 ; pioche multiple = un choix par carte, l'un après l'autre ; la main de départ n'est pas concernée ; George se déclenche après le choix (la carte choisie est protégée) ; Grinder s'applique aux propositions. Écran : 3 cartes côte à côte, touches 1/2/3, phrase méta au premier déclenchement (« Un jeu de cartes dans ton jeu de cartes. »).
- **M4 — Puissance des trinkets** : un multiplicateur d'effet par trinket (Diplôme EPFL).
- **M5 — Carte morte / mort instantanée** : carte « NO CARDS » injouable jusqu'à la fin du combat, adversaire tué sur le coup (jamais un boss). Fait.
- **M6 — Annulation de tour** : revenir à l'état du début du tour précédent, en rejouant la seed et les actions depuis le début. Fait (`run.Run`).
- **M7 — Tirage avantageux** : seuls les tirages de chance en faveur du joueur sont faits deux fois (Googz, NullPointerException, rareté des récompenses) ; on garde le favorable. Fait (`core.Luck`).
- **M10 — Déblocages durables** : conditions remplies pendant une partie → secret dans le pool à partir de la partie suivante, sauvegardé dans le dossier de sauvegarde. Fait (détails et spoilers : `docs/SECRETS.md`).
- **M9 — Clopes** : ressource de run, actions Fumer / Rouler, intensité de malédiction, multiplicateur final de stat (Ross), soins non plafonnés.
- **M8 — Combats et malédictions** : la partie est découpée en combats (un par adversaire) ; malédictions qui durent jusqu'au combat suivant (Gueule de bois).

## Choomies (monnaie)
- **15 Choomies** au début de la partie. Gagnés à chaque victoire : commun 12-18, élite 30-40, boss 80-100
  (tirés au hasard), Clarence 75. Multipliés par la stat `CHOOMIES_GAIN` (arrondi).
- Les Choomah en volent (5 par vol). Jamais sous 0.
- Affichage : en haut de l'écran de combat (« 15 Choomies »), dans l'état en mode texte.

## Événements (lot 3)
Code : `cardistry.story` (`Story`, `StoryChoice`, `StoryContext`, `StoryRegistry` = registre), joué par
`CombatEngine.startStory` / `chooseStory`.
- Un événement = un titre, un texte, des choix. Un choix peut demander une condition (ex. « il te faut 20
  Choomies ») : sinon il est affiché grisé avec ce qui manque, et on ne peut pas le prendre.
- Conséquences possibles (`StoryContext`) : Choomies (gain ou paiement), soin (un vrai soin : efficacité,
  Ross), dégâts (**jamais mortels** hors combat : il reste au moins 1 PV), clopes, Gueule de bois au
  prochain combat, carte ajoutée au deck, et des tirages de chance (en faveur du joueur : doublés par
  CLAUDE.md). On en ajoutera selon les vrais événements.
- Tirage : un sous-flux de la seed (`evenements`) ; on ne revoit un événement qu'après les avoir tous vus.
- Après le choix, un écran montre ce qui s'est passé (« Entrée continuer »). Ctrl+Z rejoue les choix.
- **Placeholders** (en attendant les vrais, références EPFL et soirées) :

| Événement | Choix |
|---|---|
| Distributeur en panne | Secouer : 50 % +25 Choomies, sinon −6 PV · Passer ton chemin |
| Un inconnu généreux | Accepter : +15 PV et Gueule de bois au prochain combat · Lui demander une astuce : +1 Boule de feu dans le deck · Refuser |
| Machine à clopes | Acheter un paquet (20 Choomies) : +4 clopes · Taper dessus : 30 % +2 clopes, sinon −4 PV · Partir |

## Shop (lot 2)
Code : `CombatEngine.openShop` / `shopAction`, `core.Shop`, `core.shop` (`ShopPrices`, `ShopOffer`, `ShopAction`).
- Nœud `$` de la carte. **5 cartes** (noms différents, tirées du registre avec les poids des trinkets),
  **2 objets**, **2 trinkets** (jamais un trinket déjà équipé, jamais un secret). La rareté des articles
  n'est **pas** doublée par CLAUDE.md (au shop, plus rare = plus cher, ce n'est pas un avantage).
- **Prix** (Choomies) : objets et trinkets selon la rareté, 50 / 80 / 130 / 200. Cartes : **60 %** du prix
  d'un objet de même rareté, arrondi à 5 → **30 / 50 / 80 / 120**. Rareté d'une carte *(provisoire)* :
  sort de 15+ ou soin de 15 = Peu commun, soin de 20+ = Rare, le reste (sorts et soins de base,
  terrains) = Commun.
- **Retrait d'une carte** : 75, +25 à chaque retrait de la **partie** (75, 100, 125…) ; jamais sous 5 cartes ;
  autant de fois qu'on veut tant qu'on peut payer.
- **Reroll** : 20, +10 à chaque reroll dans **ce** shop (le prix repart à 20 au shop suivant) ; renouvelle les
  9 articles (vendus compris).
- Inventaire plein à l'achat : même écran de remplacement que pour les récompenses (ou retour au shop).
- Écran : cartes en haut, objets et trinkets en dessous, prix grisés si pas assez de Choomies, « vendu »
  après achat, détail de l'article sélectionné ; R reroll, T retirer une carte, P partir (Échap ouvre la pause, comme ailleurs). Mode texte :
  liste numérotée, `r`, `t`, `0`.
- Accueil **provisoire** : « Entre, entre ! Tout est à vendre... enfin, presque. » (nom du vendeur et
  réplique à venir).
- Hooks : `onShopEnter`, `onItemBought` (Grinder acheté ajoute aussi son Petit pétou).
- Ctrl+Z : les achats sont rejoués à l'identique (mêmes articles, mêmes rerolls).

## Secrets / easter eggs
- Trinkets « Secret » débloqués durablement par des conditions remplies pendant une partie (façon *Isaac*) ; des codes (seed spéciale, code clavier) les donnent pour une seule partie.
- Garder la liste des secrets dans `docs/SECRETS.md` (spoilers !).
- Existant : terrain « EPFL » de type feu, heal « Petit pétou ».

## Interface (terminal ASCII, Lanterna)
Fait : écrans titre, règles, combat, fin de partie ; clavier direct (←/→ + Entrée, 1-7, P piocher, Q quitter avec confirmation) ; sprites ASCII des 3 adversaires ; barres de PV ; main en cadres ASCII colorés par élément ; aperçu de l'effet réel de la carte sélectionnée ; journal de combat ; animations (dégâts qui clignotent, chiffres flottants, tremblement/charge de l'ennemi, mort, arrivée, pioche).
Fait aussi : écran de pioche au choix (Paquet de cartes : 3 cartes, touches 1/2/3, effet réel de la sélectionnée, phrase méta au premier déclenchement) ; écran « jeter un trinket » obligatoire (Diplôme perdu) ; sets dans l'inventaire (palier atteint, prochain palier, ×2 si amplifié) et bandeau « ★ Set … activé » ; trinkets amplifiés marqués « (effet ×2) » ; paquet de clopes (petit sprite ASCII en bas à droite, n/20, touches F fumer / G rouler, jauge Honoré) ; PV au-dessus du max en doré (Ross) ; transformation Googz affichée (journal + texte flottant) ; effets temporaires affichés avec leur durée restante (ligne au-dessus du terrain, liste complète dans l'inventaire) ; objets et cartes homonymes (Caïpirinha, Petit pétou) distingués à l'écran (« la carte … » / « l'objet … » dans le journal, couleur d'élément pour les cartes, de rareté pour les objets) ; écran de récompense (cartes côte à côte, couleur de rareté), écran de remplacement si inventaire plein, inventaire (touche I), objets utilisables avec W/E/R (clavier QWERTZ, action gratuite), trinkets affichés en combat.
Fait aussi : carte de la run (écran Lanterna + mode texte), intents des adversaires, sprites animés et visionneuse `--sprites`.
Fait aussi : shop (plein écran + mode texte) ; fenêtre du jeu par défaut (police pixel embarquée, double buffer, images PNG avec repli ASCII, filtre CRT désactivé par défaut).
Fait aussi : événements (plein écran + mode texte).
Fait aussi : son (musique par contexte avec fondu, effets sur les événements, placeholders), menu pause (Échap), Options (volumes, muet) depuis le titre et la pause.
Reste : transitions d'écran ; vrais sons et musiques.

- Plein écran, input direct au clavier (plus de « Appuie sur Entrée »).
- Écrans : titre, carte de la run, combat, shop, récompense, game over.
- Combat : sprite ASCII de l'ennemi, barres de PV, main de cartes dessinées en cadres ASCII, trinkets affichés, journal de combat.
- Animations : dégâts qui clignotent, chiffres flottants, tremblement de l'ennemi, transitions d'écran.
- Couleur de rareté et d'élément partout.

## Questions ouvertes
- **[À DÉCIDER]** Noms et attaques des 3 boss (placeholders « Boss du niveau 1/2/3 »).
- **[À DÉCIDER]** Noms des variantes de Choomah (provisoires : « Choomah pressé », « Choomah costaud »).
- **[À DÉCIDER]** Nom du vendeur du shop et sa réplique d'accueil. Il a un sprite (ébauche : bonnet, barbe, clope au bec) et il fume.
- **[À DÉCIDER]** Dave : son soin profite-t-il des bonus de soin (efficacité, Ross) ou est-il brut ? *Provisoire : il en profite.*
- Provisoire : chiffres des paliers d'Ivresse (+10 / +25 / +50 %) et de la clope (+5 PV, sans limite par tour) à ajuster en jouant.
- **[À DÉCIDER]** Sprites des trois personnages (placeholder commun en attendant) et affichage de l'alien de Sassy.
- **[À DÉCIDER]** Les vrais événements (les 3 actuels sont des placeholders).
- Provisoire : rareté des cartes au shop (voir « Shop »).
- Provisoire : les intents des Choomah gardent les chiffres lisibles dans le cri.
- Nom définitif du jeu (« Cardistry » affiché en attendant).
- **[À DÉCIDER]** Valeur des PV max de départ (100 provisoirement, stat `MAX_HP`).
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
- ~~Diplôme EPFL : portée du doublement, perte avec 6 trinkets~~ → tout l'effet doublé malus compris ; +1 emplacement seulement ; perdre le Diplôme avec 6 trinkets oblige à en jeter un (au choix).
- ~~Gueule de bois : combien, quand~~ → une seule par combat, aucune pendant le dernier combat.
- ~~Ptit Sat ?~~ → dégâts élémentaires, comme CMS.
- ~~Pichet vide mortel~~ → peut tuer le joueur ; mort simultanée = défaite.
- ~~Gueule de bois avec une main pleine~~ → sans objet : la main de départ (5) reste sous la taille max (6 avec la malédiction).
- ~~Secrets : déblocage~~ → durable, par conditions remplies pendant une partie ; le secret entre dans le pool (poids 2) à la partie suivante ; les codes ne valent que pour la partie en cours. NullPointerException : NO CARDS injouable jusqu'à la fin du combat, le dernier adversaire compte comme boss. CLAUDE.md : seuls les tirages de chance en faveur du joueur sont doublés ; un secret débloqué pendant la partie gagnante compte. Ctrl+Z : une fois par combat, ni après la mort ni au premier tour d'un combat. `--seed null` : seed aléatoire affichée. Mode texte : seul le code « claude » (détails : `docs/SECRETS.md`).
- ~~Pioche infinie~~ → remplacée par un deck construit (lot 1 bis de la run complète).
- ~~Run complète (lot 1)~~ → 3 niveaux de 8 étages boss compris (7 = Sieste, 8 = Boss) ; valeurs d'adversaires, de boss et de scaling validées pour démarrer ; Rebond du Peneu ne tue pas ; Sieste : soin 30 %, +3 clopes ou retirer une carte, n'efface pas la Gueule de bois ; après chaque boss, la moitié des PV manquants ; NullPointerException peut annuler une élite (seuls les 3 boss sont immunisés) ; Moscow Mule annule toute une attaque mais pas les vols ni la Taxe ; tirages des adversaires hors CLAUDE.md ; carte tirée d'un sous-flux nommé de la seed ; « Plante » affiché pour l'élément nature.
- ~~Lot 1 : choix provisoires~~ → récompense de carte à part, après l'objet ; Peu commun en élite, Rare au boss ; la Sieste est un soin (efficacité, Ross), la récupération après boss non ; le Shot de Jäger est consommé si le Paresseux esquive ; Sieste possible aux étages 4-5.
- ~~Koala~~ → c'est un **Paresseux** accroché à sa branche (« Badoing, badoing ! »), peu de dégâts, pas de saut. ~~Contrôle surprise~~ → **Série d'algèbre** (Vafi). Les Choomah parlent en cris (« ooOOOaAAAOaAOOA »).

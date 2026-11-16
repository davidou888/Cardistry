# Cardistry — Système de statistiques
Proposition de design (V3) : stats des personnages et des ennemis, formule de combat, modifications des cartes existantes. État du jeu relevé dans le dépôt le 16 novembre 2026.
## 1. Où en est le jeu
Le plan V2 est entièrement implémenté et intégré dans GAME_DESIGN.md. Ce document part de cet état.

| Domaine | État actuel |
|---|---|
| Personnages | 4 jouables : Louis (MAN, +50 % dégâts), Dave (soin quand élément du terrain = élément du sort qui bat l'ennemi), Sassy (130 PV, alien 4 dégâts/tour), Frederic BLC (aucune capacité) |
| Éléments | Deux triangles : Feu > Plante > Eau > Feu et Géométrique > Algébrique > Spirituel > Géométrique. Neutre ne bat rien. GLITCH bat tout. |
| Cartes | 44 au total : 21 sorts, 13 terrains joueur, 10 soins. Rareté affichée (C/P/R/L), pouvoirs (★) |
| Ennemis | 20 : 12 communs (dont 3 Choomah et Clarence), 5 élites (dont Engeance des cookies, GLITCH), 3 boss provisoires (130 / 200 / 280 PV) |
| Statuts | Intox, Fons, Hypnotisé, Bondage, Brûlure — sur le joueur et les ennemis |
| Terrains | Effets de terrain du joueur (Dojo, Gare, Migros, EPFL…), terrain propre à chaque ennemi avec malus |
| Stats existantes | Joueur uniquement : PV max, taille de main, dégâts par élément (%), soins, réduction de dégâts, dégâts subis… Les ennemis n'ont que PV + dégâts de leurs attaques |


Constat : le joueur a déjà un système de stats complet (bonus plats, pourcentages, multiplicateurs finaux), mais il ne sert qu'aux trinkets et terrains, et les ennemis n'en ont pas. L'idée est d'en faire de vraies stats de personnage, partagées par le joueur et les ennemis.
## 2. Les 6 statistiques
Principe : peu de stats, de petites valeurs (0 à 10), chacune liée à un élément ou une mécanique existante, pour que le joueur puisse calculer de tête.

| Stat | Abrév. | Effet par point | Liée à |
|---|---|---|---|
| Force | FOR | +1 dégât sur les sorts feu, eau, plante, neutre | Style « frapper fort » |
| Intelligence | INT | +1 dégât sur les sorts algébriques et géométriques ; +3 % de chance d'infliger un statut | Nouveaux éléments, statuts |
| Esprit | ESP | +1 dégât sur les sorts spirituels ; +1 PV sur chaque soin | Spirituel, soins |
| Défense | DEF | −1 dégât subi par coup (remplace la réduction de dégâts actuelle) | Polycopié, terrains |
| Résistance | RES | −5 % de chance de subir un statut ; à 5 ou plus, durée des statuts subis −1 tour (min. 1) | Intox, Brûlure, Fons… |
| Chance | CHA | +3 % de coup critique (×1,5) sur les sorts | Moindre carré, CLAUDE.md |


Les sorts GLITCH (aucun pour l'instant) utiliseraient la meilleure stat offensive du lanceur. Les ennemis ont exactement les mêmes 6 stats, avec les mêmes effets dans l'autre sens.

## 3. Formules
### Dégâts d'un sort ou d'un coup
dégâts = (base + stat offensive) × multiplicateurs × critique − DEF de la cible, minimum 1
- Stat offensive : FOR, INT ou ESP selon l'élément du sort (voir §2). Pour un coup d'ennemi : sa FOR.
- Multiplicateurs : élément, terrain, statuts (Hypnotisé), trinkets en %, comme aujourd'hui.
- DEF soustraite à la fin, en plat : elle compte beaucoup contre les petits coups et peu contre les gros. C'est ce qui crée les choix tactiques (petits sorts inutiles contre Le Peneu).
- Minimum 1 si la carte a des dégâts. Un sort à 0 dégât (Pet de Mamouz) reste à 0.
- Inchangé : les dégâts non multipliés (Professeur d'or) et les dégâts de statut restent bruts : ni stats, ni DEF.
Exemple : Boule de feu (10) lancée par Louis (FOR 3) sur un ennemi plante (×1,5) avec DEF 2 : (10 + 3) × 1,5 − 2 = 17,5 → 18.
### Soins
soin = (base + ESP) × multiplicateurs de soin, puis plafond PV max (sauf Ross). Les soins en % des PV max (Fons) ne reçoivent pas l'ESP.
### Chance d'infliger un statut
chance = chance de la carte + 3 % × INT du lanceur − 5 % × RES de la cible, bornée entre 0 et 100 %.
- Proposition : la RES s'applique aussi aux statuts « à coup sûr » (Pet de Mamouz, Vafi). Sinon la RES ne sert à rien contre eux. Contre un ennemi RES 4, Pet de Mamouz passe à 80 %.
- Tirages doublés par CLAUDE.md : inchangé (seulement les tirages en faveur du joueur, critiques compris).
## 4. Stats de base des personnages
Chaque personnage a 8 points répartis selon sa capacité, sauf Frederic BLC : sans capacité, il est compensé par des stats « parfaitement moyennes » (12 points, 2 partout). Ça lui donne enfin une identité : le perso des builds de stats.

| Personnage | PV | FOR | INT | ESP | DEF | RES | CHA | Logique |
|---|---|---|---|---|---|---|---|---|
| Louis | 100 | 3 | 2 | 0 | 1 | 1 | 1 | MAN (+50 % dégâts) : profil offensif, FOR et INT |
| Dave | 100 | 1 | 3 | 1 | 1 | 1 | 1 | Joue les éléments : INT pour les statuts et les sorts algébriques/géométriques |
| Sassy | 130 | 1 | 0 | 2 | 2 | 2 | 1 | Déjà 130 PV et l'alien : profil endurant (DEF, RES, ESP pour les soins) |
| Frederic BLC | 100 | 2 | 2 | 2 | 2 | 2 | 2 | Aucune capacité → 2 partout, le plus de points |


### Progression pendant la run (proposition)
- Après chaque boss : +2 points à répartir (écran de choix). Soit +4 sur la run.
- Certains événements peuvent donner ou retirer 1 point (« Tu révises toute la nuit : +1 INT, −1 RES »).
- Les objets et trinkets donnent des stats (voir §7).
## 5. Stats des ennemis
Valeurs au niveau 1. PV et attaques inchangés. Proposition de scaling : +1 FOR et +1 DEF par niveau (niveau 2 et 3), en plus des multiplicateurs actuels de PV et de dégâts. Les valeurs restent basses chez les communs, parce que la FOR s'ajoute à chaque coup (un coup de 4 avec FOR 2 fait déjà 6).
### Communs

| Ennemi | Élément | PV | FOR | INT | ESP | DEF | RES | CHA | Logique |
|---|---|---|---|---|---|---|---|---|---|
| Caddie énervé | Neutre | 28 | 1 | 0 | 0 | 2 | 2 | 0 | Carcasse en métal : DEF |
| Choomah | Plante | 22 | 0 | 0 | 0 | 0 | 1 | 3 | Voleur : CHA |
| Choomah pressé | Plante | 18 | 0 | 0 | 0 | 0 | 0 | 4 | Rapide et chanceux, fragile |
| Choomah costaud | Plante | 30 | 2 | 0 | 0 | 1 | 1 | 1 | Plus de FOR |
| Camé | Feu | 25 | 1 | 2 | 0 | 0 | 3 | 1 | INT : Brûlure plus fréquente ; RES : habitué aux substances |
| Paresseux | Plante | 24 | 0 | 1 | 1 | 1 | 2 | 2 | Esquiveur |
| Clarence | Neutre | 1 | 0 | 0 | 0 | 0 | 0 | 0 | Rien. Évidemment. |
| Seau de fuite | Eau | 26 | 0 | 0 | 0 | 1 | 4 | 0 | Un seau ne s'intoxique pas : RES |
| Gourou du Sat | Spirituel | 24 | 0 | 1 | 3 | 0 | 2 | 0 | ESP |
| NG | Algébrique | 26 | 0 | 4 | 0 | 1 | 1 | 0 | INT |
| Hypercube misogyne | Géométrique | 25 | 1 | 2 | 0 | 2 | 1 | 1 | Forme solide : DEF |
| Gobelin | Plante | 22 | 1 | 0 | 0 | 0 | 1 | 3 | « 67 » : CHA |


### Élites

| Ennemi | Élément | PV | FOR | INT | ESP | DEF | RES | CHA | Logique |
|---|---|---|---|---|---|---|---|---|---|
| Vafi | Feu | 55 | 2 | 4 | 2 | 1 | 3 | 1 | Prof d'algèbre : INT ; ESP pour la Pause pétou |
| Le Peneu | Neutre | 90 | 1 | 0 | 0 | 4 | 5 | 0 | Caoutchouc : DEF et RES max. Les petits sorts rebondissent |
| T-Rex défoncé | Plante | 65 | 4 | 0 | 1 | 2 | 0 | 0 | FOR énorme, RES nulle : facile à statuer |
| Étudiant post-examen | Eau | 60 | 1 | 3 | 0 | 0 | 0 | 2 | Épuisé : aucune défense |
| Engeance des cookies | GLITCH | 60 | 2 | 3 | 2 | 1 | 3 | 2 | Équilibrée et imprévisible |


### Boss (provisoires)

| Boss | Élément | PV | FOR | INT | ESP | DEF | RES | CHA | Logique |
|---|---|---|---|---|---|---|---|---|---|
| Boss niveau 1 | Feu | 130 | 3 | 1 | 1 | 2 | 3 | 1 | Test des stats |
| Boss niveau 2 | Eau | 200 | 4 | 2 | 2 | 3 | 4 | 2 | Exige un build |
| Boss niveau 3 | Plante | 280 | 5 | 3 | 3 | 4 | 5 | 3 | Fin de run |


Les boss ont une RES élevée pour qu'on ne puisse pas les enchaîner en Fons ou en Intox. Quand vous aurez leurs vrais noms et attaques, leurs stats seront à revoir avec eux.
## 6. Modifications proposées des cartes actuelles
Objectif : donner une identité à chaque carte grâce aux stats, et départager les cartes qui font aujourd'hui la même chose. Les cartes non citées ne changent pas (elles profitent automatiquement de la formule).
### Sorts

| Carte | Actuel | Proposé | Pourquoi |
|---|---|---|---|
| Jet d'eau (C) | 10 | 10 ; l'ennemi perd 1 DEF pour le combat | Identité de l'eau : l'érosion (le feu brûle, la plante attache, l'eau use) |
| Tsunami (P) | 15 | 15 ; ignore la DEF | La vague passe par-dessus tout |
| Pluie de flammes (P) | 15 | 15 ; 20 % de Brûlure | Version forte de la Boule de feu |
| Tempête d'épines (P) | 15 | 15 ; 20 % de Bondage | Version forte du Fouet de lianes |
| Carte basique pas ouf (C) | 10 | 6 + 2 × FOR | « Pas ouf, sauf si t'es fort » : nulle au début, bonne en build FOR |
| Lancer de manette (C) | 12 | 12 ; critique garanti si CHA ≥ 3 | Se distingue de CAYOUUU |
| CAYOUUU (C) | 12 | 12 ; ignore 2 points de DEF | Se distingue du Lancer de manette |
| Pain pita (C) | 10 | 10 ; ignore la DEF | Géométrie : trouver le bon angle |
| Moindre carré (P) | 1 à 40 | 1 à (30 + 2 × INT) | La carte du build INT |
| dist(PLAYER) (P) | 10 + étages | 10 + étages (inchangé) | Profite déjà de l'INT via la formule |
| Contre-exemple (R) | annule 1 statut | annule 1 + INT ÷ 3 statuts | Récompense l'INT |
| Big Lez Shotgun (L) | 80 | 80 ; −2 FOR pour le reste du combat (recul) | Un vrai coût pour une Légendaire aussi forte |


### Soins

| Carte | Actuel | Proposé | Pourquoi |
|---|---|---|---|
| Stingin' Roger (C) | −5 PV, +10 % dégâts (combat, cumulable) | −5 PV, +1 FOR (combat, cumulable) | Plus lisible avec les stats |
| Sparadrap (C) | +12 | +12 ; +1 DEF pour le combat | Se distingue de Potion et Verre d'eau |
| Berceuse inconnue (P) | retire les statuts | retire les statuts ; +2 × ESP PV | Récompense l'ESP |
| Soupe de mamie (C) | +10, retire l'Intox | +10, retire l'Intox ; +1 RES pour le combat | La soupe de mamie immunise |


### Terrains

| Terrain | Actuel | Proposé |
|---|---|---|
| Dojo | +10 % de dégâts | +1 FOR (on s'entraîne) |
| Désert kaléidoscopique | hypnose ×4 | inchangé, + 1 ESP |
| Espace de dimension n | rejeu à 50 % | inchangé, + 1 INT |
| Terrains ennemis | malus en % ou en taille de main | Plusieurs malus deviennent des stats : Bord d'autoroute « +2 dégâts subis » → −2 DEF ; Amphi d'examen −20 % sorts → −2 INT ; Toit qui fuit −20 % soins → −2 ESP ; Passage sous-gare → −2 RES |


## 7. Objets et trinkets liés aux stats
### Nouveaux trinkets simples (Communs)

| Trinket | Effet | Tags |
|---|---|---|
| Donut Musclé | +2 FOR | Force |
| Carte graphique | +2 INT | EPFL |
| Bâton d'encens | +2 ESP | Soirée |
| Veste de ski | +2 DEF | Défense |
| Carnet de vaccination | +2 RES | Défense |
| Benediction QCM | +2 CHA | EPFL |


### Trinkets existants à convertir
- Polycopié d'Analyse I : « −2 dégâts subis » devient +2 DEF (même effet, plus lisible).
- CMS : « +50 % dégâts, adversaire +25 % » peut rester tel quel ou devenir +3 FOR, +3 INT, mais l'adversaire +2 FOR.
### Objets consommables (stats temporaires)
- Protéine en poudre : +3 FOR pour le combat.
- Documentation LATEX : +3 INT pour le combat.
- Thé odorant : +3 RES pour le combat et retire un statut.
## 8. Affichage
- Fiche personnage (touche C) : les 6 stats, avec le détail des bonus (base, trinkets, terrain, statuts).
- Ennemi : ses stats visibles dans la fiche ennemi (une touche dédiée) ; DEF et RES affichées en petit sous sa barre de PV, car ce sont celles qui changent les choix du joueur.
- Fiche de carte (V) : déjà existante, elle montre chaque multiplicateur ; y ajouter la ligne « + FOR » et « − DEF ».
- Intent : les dégâts annoncés incluent déjà terrain et réduction ; ils incluront la FOR de l'ennemi et ta DEF.
## 9. Implémentation (pour Claude Code)
- Ajouter FOR, INT, ESP, DEF, RES, CHA dans StatType, et donner un objet Stats aux ennemis (même système que le joueur).
- Fusionner DAMAGE_REDUCTION et DAMAGE_TAKEN dans DEF (les trinkets et terrains existants passent à DEF).
- Centraliser la formule de dégâts, de soin et de chance de statut dans un seul endroit (ElementRules ou une classe dédiée), testée.
- Stats de base des personnages dans HeroRegistry, des ennemis dans EnemyRegistry, scaling par niveau.
- Puis les modifications de cartes (§6), les trinkets (§7), l'affichage (§8), par petits lots.
- Attention à l'équilibrage : la DEF ennemie réduit tous les sorts du joueur. Rejouer une partie complète avec chaque personnage après la formule, avant d'ajouter le contenu

## 10. Détails d’implémentation
- Ajouter le sprite de l’alien de sassy, se place à ses coté, animation d’attaques.
- Pour les placeholder, implementer les sprite placeholder pour les animation (afin de simplifier les modifications plus tard).
- Modification du poids des légendaire dans les pool :
  - Constat : les légendaire spawn trop.
  - Fix : baisser le spawn rate des légendaires.
## 11. Questions ouvertes

| # | Question | Proposition par défaut |
|---|---|---|
| 1 | La RES s'applique-t-elle aussi aux statuts « à coup sûr » ? | Oui (sinon la RES est inutile contre eux) |
| 2 | Multiplicateur de critique ? | ×1,5 |
| 3 | Les stats montent-elles pendant la run ? | +2 points à répartir après chaque boss |
| 4 | Frederic BLC : 2 partout (12 points) ? | Oui, c'est son identité |
| 5 | Scaling des ennemis par niveau ? | +1 FOR et +1 DEF par niveau |
| 6 | Les sorts GLITCH (s'il y en a) utilisent quelle stat ? | La meilleure stat offensive du lanceur |
| 7 | CMS : tel quel ou converti en stats ? | À trancher |


### Petites remarques relevées dans le code
- Element : les noms affichés « algebrique » et « geometrique » n'ont pas d'accents (« algébrique », « géométrique »).
- Element.PLAYABLE contient GLITCH : le Gaz du bat Inf peut tirer un élément GLITCH (×1,5 contre tout). Voulu ?
- CardPool : Seau d'eau, Berceuse inconnue et Taffe de foufou bâtard sont rangés sous le commentaire // COMMON alors qu'ils sont Peu commun / Rare (sans effet sur le jeu, juste la lisibilité).


# Cardistry — Plan V2 : statuts, pouvoirs de cartes, terrains

> Document de référence pour le prochain chantier. Les décisions ci-dessous sont **tranchées** ;
> les points encore ouverts sont regroupés dans « Questions ouvertes » à la fin.
> Les règles « qui bat qui » de Spirituel / Algébrique / Géométrique ne sont **pas** traitées ici.

---

## 1. Fix : PV entiers

- Les PV du joueur et des ennemis sont **toujours des entiers**.
- Arrondi à l'entier le plus proche **au moment où les PV changent** (dégâts, soins, statuts), pas seulement à l'affichage.
- Les autres valeurs (multiplicateurs, pourcentages) restent décimales.
- Prérequis pour la carte **Matrix** (test sur le dernier chiffre des PV).

---

## 2. Statuts

### Règles communes
- Applicables au **joueur et aux ennemis**.
- Les effets par tour s'appliquent **au début du tour de la cible**.
- Affichage : icône + tours restants à côté de la barre de PV. Un ennemi en Fons affiche « Endormi » comme intent.
- Tous les tirages (probabilités, durées aléatoires) passent par la seed.
- Chaque statut a une icône (planche Aseprite) et un son.

### Liste

| Statut | Effet | Durée | Réapplication |
|---|---|---|---|
| **Intox** | −5 PV par tour | 3 tours | **impossible** tant qu'Intox est actif |
| **Fons** | la cible ne joue pas (le joueur passe ses tours, l'ennemi n'attaque pas) | 1 à 3 tours (tiré au hasard) | durée remise au max |
| **Hypnotisé** | les cartes d'attaque font **×2** à la cible | 1 tour (certaines cartes : plus) | durée remise au max |
| **Bondage** | vole 5 PV par tour à la cible, au profit de celui qui l'a infligé ; **8 PV** si le terrain est de type plante | 3 tours | durée remise au max |
| **Brûlure** | −8 PV/tour sur terrain feu, −4 PV/tour sur les autres | 3 tours | durée remise au max |

- **Brûlure et eau** : ne peut pas être appliquée sur un terrain eau, et **s'éteint immédiatement** quand un terrain eau est posé.
- **Statuts négatifs du joueur** (pour la Berceuse et les effets de purge) : Intox, Fons, Bondage subi, Brûlure. **Pas** la Gueule de bois ni l'Ivresse.

### Statuts infligés par les ennemis
| Ennemi | Attaque | Statut |
|---|---|---|
| Camé | chaque attaque | 30 % de Brûlure |
| Choomah (et variantes) | Vol à la tire | Intox |
| Vafi | Série d'algèbre | Fons 1 tour (on s'endort en cours) |
| Paresseux | « Badoing, badoing ! » | 25 % de Fons 1 tour |

---

## 3. Pouvoirs de cartes

- Une carte peut avoir un **pouvoir** : un texte affiché + un effet (`Consumer<EffectContext>`, comme les objets), déclenché **après** l'effet principal.
- `copy()` conserve le pouvoir (test obligatoire).
- `EffectContext` doit permettre : appliquer un statut (à l'ennemi ou au joueur, avec probabilité), lire les PV de l'ennemi, lire le nombre d'étages parcourus, infliger des dégâts **non multipliés**, ajouter un bonus de dégâts pour le reste du combat, retirer les statuts négatifs du joueur.
- Le pouvoir s'affiche sur la carte et dans la ligne de description sous la main.

---

## 4. Effets de terrain (terrains du joueur)

### Règles
- Un terrain peut porter des **bonus de Stats** (source « terrain ») et/ou un **effet spécial**.
- Ses effets ne sont actifs **que tant qu'il est en jeu** : retirés quand le terrain change (retirer l'ancien **puis** ajouter le nouveau) et à la fin du combat.

### Liste

| Terrain | Élément | Rareté | Effet |
|---|---|---|---|
| Migros | neutre | Commun | si le combat se termine sur ce terrain : **+25 % de Choomies** |
| Gare | neutre | Commun | jouer une carte terrain depuis la Gare **ne coûte pas le tour** et donne **+1 clope** |
| Dojo | neutre | Commun | **+10 % de dégâts** |
| Amazonie | plante | Peu commun | attaques plante **+20 %** |
| Enfer | feu | Peu commun | attaques feu **+20 %** |
| Atlantis | eau | Peu commun | attaques eau **+20 %** |
| Désert kaléidoscopique | spirituel | Rare | l'hypnose fait **×4** au lieu de ×2 |
| Espace de dimension n | algébrique | Rare | chaque carte jouée est **rejouée une fois à 50 %** au tour suivant, **seulement si on est encore sur l'Espace** (statuts réappliqués ; les probabilités sont retirées) |
| Fractale | géométrique | Rare | **tous** les multiplicateurs (joueur ET ennemi) sont arrondis à l'entier supérieur (×1.25 → ×2, ×0.5 → ×1) |
| EPFL | feu | Légendaire | voir §5 |

### 5. EPFL (Légendaire)
- Pendant **3 tours**, toutes les valeurs du joueur sont **réellement doublées** : dégâts de toutes les cartes, soins, réduction de dégâts, **PV max et PV actuels** (×2 à l'arrivée).
- **Pas** doublés : taille de main, Choomies, clopes.
- En quittant l'EPFL (changement de terrain, fin des 3 tours ou fin du combat) : **PV actuels ÷ 2** (arrondis), bonus retirés.
- Après 3 tours : retour automatique sur le **Dojo**. Si le joueur change de terrain avant, le minuteur est annulé.
- Tests obligatoires : changement avant 3 tours ; combat terminé sur l'EPFL (PV divisés **avant** la fin du combat) ; dégâts subis pendant l'EPFL (proportion conservée).

---

## 6. Terrains des ennemis

- Chaque combat commence sur le **terrain de l'ennemi**. Ces terrains ne sont **jamais** dans le pool (ni récompense, ni shop).
- Ils donnent des **malus au joueur** tant qu'il reste dessus. Jouer une carte terrain = « sortir de chez l'ennemi ».
- La règle existante s'applique : un ennemi sur un terrain de son élément attaque à ×1.25.
- Boss provisoires : départ sur le Dojo.

| Ennemi | Terrain | Élément | Malus pour le joueur |
|---|---|---|---|
| Caddie énervé | Parking du Migros | neutre | −1 taille de main |
| Clarence | Le canapé | neutre | aucun |
| Le Peneu | Bord d'autoroute | neutre | +2 dégâts subis par attaque |
| Choomah | Buisson louche | plante | −20 % soins |
| Paresseux | Canopée | plante | −15 % dégâts des sorts |
| T-Rex défoncé | Jurassic Ecublens | plante | ennemi +15 % dégâts |
| Camé | Passage sous-gare | feu | ennemi +15 % dégâts |
| Vafi | Amphi d'examen | feu | −20 % dégâts des sorts |
| Seau de fuite | Toit qui fuit | eau | −20 % soins |
| Étudiant post-examen | Biblio à 3h du mat | eau | −1 taille de main |

À vérifier : une main déjà pleine quand la taille de main baisse.

---

## 7. Cartes modifiées

| Carte | Changement |
|---|---|
| Boule de feu | 10 % d'infliger Brûlure |
| Pet de Mamouz | **0 dégât**, 100 % d'infliger Intox |
| Fouet de lianes | 10 % d'infliger Bondage |
| Petit pétou → **Petite taffe** | soin 15 ; 15 % de tomber en Fons ; pendant ce Fons, +5 % des PV max par tour. ⚠️ Renommer aussi dans le Grinder (et vérifier l'objet Petit pétou / Googz). |
| Désert kaléidoscopique, Espace de dimension n, Fractale | passent **Rare** |
| Enfer, Atlantis | passent **Peu commun** |

---

## 8. Nouvelles cartes

### Sorts
| Carte | Élément | Rareté | Effet |
|---|---|---|---|
| CAYOUUU | neutre | Commun | 12 dégâts |
| Pain pita | géométrique | Commun | 10 dégâts |
| Matrix | algébrique | Commun | 10 dégâts ; **×2** si les PV de l'ennemi finissent par 0 ou 1 (vérifié avant le coup) |
| Moindre carré | algébrique | Peu commun | dégâts aléatoires de 1 à 40 |
| dist(PLAYER) | géométrique | Peu commun | 10 dégâts + nombre d'étages parcourus |
| Purification des Chakra | spirituel | Rare | 0 dégât ; inflige Hypnotisé à l'ennemi pour son prochain tour |
| Professeur d'or | spirituel | Légendaire | 35 dégâts **non multipliés** ; Hypnotisé pendant 5 tours |
| Pyromane | feu | Rare | 15 dégâts, **×2** si l'ennemi brûle |
| Somnifère | spirituel | Peu commun | 0 dégât ; Fons 1 tour à l'ennemi |
| Réveil brutal | neutre | Peu commun | 20 dégâts à un ennemi en Fons (et le réveille) ; sinon 8 |
| Contre-exemple | algébrique | Rare | annule le prochain statut que le joueur subirait |

### Soins
| Carte | Élément | Rareté | Effet |
|---|---|---|---|
| Verre d'eau | eau | Commun | +10 PV |
| Sparadrap | neutre | Commun | +12 PV |
| Soupe de mamie | neutre | Commun | +10 PV et retire l'Intox |
| Stingin' Roger | feu | Commun | −5 PV, +10 % de dégâts pour tout le combat (cumulable) |
| Seau d'eau | eau | Peu commun | +20 PV |
| Berceuse inconnue | spirituel | Peu commun | retire tous les statuts négatifs du joueur (sauf Gueule de bois et Ivresse) |
| Taffe de foufou bâtard | feu | Rare | Fons 3 tours ; pendant ce Fons : **immunisé aux dégâts directs** et +8 % des PV max par tour ; reste sensible aux statuts infligés par les ennemis |

---

## 9. Questions ouvertes

1. **Moindre carré et CLAUDE.md** : le tirage 1-40 compte-t-il comme un tirage « en ta faveur » (fait deux fois, on garde le meilleur) ?
2. **Rareté de la Petite taffe** : reste Commun (code actuel) ou passe Peu commun (soin 15 + risque de Fons) ?
3. **Règles « qui bat qui »** de Spirituel / Algébrique / Géométrique : à décider.
4. **Ennemis des nouveaux éléments** : aucun ennemi n'est Spirituel / Algébrique / Géométrique pour l'instant.

---

## 10. Ordre d'implémentation

1. Fix PV entiers.
2. Système de statuts générique (testé d'abord avec Intox seul), affichage, statuts ennemis.
3. Pouvoirs de cartes.
4. Effets de terrain du joueur, puis EPFL.
5. Terrains des ennemis.
6. Contenu : cartes modifiées, puis nouvelles cartes, par petits lots, avec une partie jouée entre chaque lot.

# Secrets — SPOILERS

> Ne pas lire si tu veux découvrir le jeu.

## Existants
- Terrain « EPFL » (feu) : terrain de départ.
- Soin « Petit pétou » (feu).

## Trinkets Secret

Absents du pool tant qu'ils ne sont pas débloqués. Tous trois tag Méta.

| Nom | Effet | Déblocage | Code (cette partie seulement) | Méca. |
|---|---|---|---|---|
| NullPointerException | Au début de chaque combat, un tirage unique : 5 % que l'adversaire devienne null (mort sur le coup, jamais le boss), 10 % qu'une de tes cartes devienne « NO CARDS » | Gagner un combat avec 0 carte en main | `--seed null` | M5 |
| Ctrl+Z | Une fois par combat, annule ton dernier tour | Perdre une partie contre le dernier adversaire alors qu'il lui reste ≤ 5 PV | Ctrl+Z sur l'écran titre | M6 |
| CLAUDE.md | Chaque tirage de chance en ta faveur est fait deux fois, tu gardes le meilleur | Gagner une partie en ayant déjà débloqué les deux autres secrets | taper « claude » sur l'écran titre | M7 |

### Déblocage durable (façon Isaac)
- Une condition remplie pendant une partie débloque le secret **pour de bon** ; il rejoint le pool des
  récompenses **à partir de la partie suivante** (le pool d'une partie est figé à son lancement).
- Dans le pool, un secret débloqué a un **poids de 2** (un commun pèse 60) : il n'est jamais équipé
  d'office.
- Écran de fin : « ✦ Secret débloqué : <nom> ✦ » (une ligne par secret ; rien si aucun).
- Le déblocage est sauvegardé dès que la condition est remplie (quitter ensuite ne le perd pas).
- Les **codes** donnent le secret équipé pour la partie en cours **uniquement** : aucun déblocage durable.

### Sauvegarde
- `unlocks.txt` dans le dossier de sauvegarde : `$XDG_DATA_HOME/cardistry/`, sinon
  `~/.local/share/cardistry/` (macOS : `~/Library/Application Support/Cardistry/`, Windows :
  `%APPDATA%\Cardistry\`).
- Format texte : en-tête `cardistry-unlocks 1`, puis un identifiant par ligne (`null-pointer-exception`,
  `ctrl-z`, `claude-md`) ; lignes vides et `#` ignorées.
- Fichier absent, illisible ou corrompu (en-tête manquant, ligne inconnue) : on repart de zéro sans
  planter ; il est réécrit au prochain déblocage. Écriture impossible : la partie continue.
- `--reset-unlocks` efface les déblocages, puis lance le jeu normalement.

### Règles détaillées
**NullPointerException**
- Le tirage a lieu quand la main du combat est distribuée. Le dernier adversaire compte comme boss :
  pour lui, les 5 % « null » ne donnent rien.
- « NO CARDS » : carte sans élément, **injouable jusqu'à la fin du combat** (jouer = refusé), elle occupe
  un emplacement de la main. La carte remplacée est tirée au hasard dans la main.
- Condition de déblocage : le combat gagné doit l'être avec la main vide au moment de la mort de
  l'adversaire (le dernier combat compte aussi).

**Ctrl+Z**
- Touche Ctrl+Z pendant ton tour (mode texte : `z`). Retour au **début du tour précédent** du même combat :
  tes actions gratuites de ce tour et du tour en cours sont annulées aussi.
- **Une fois par combat** : de nouveau disponible au combat suivant. L'écran de combat affiche
  « Ctrl+Z : disponible » ou « Ctrl+Z : utilisé » (à droite de « Ta main » ; en mode texte, dans l'état).
- Implémenté en rejouant la partie depuis la seed avec tous tes choix : rejouer la même action redonne
  exactement le même résultat (même pioche, mêmes tirages).

**CLAUDE.md** — « le meilleur »
- Seuls les tirages de **chance en faveur du joueur** sont faits deux fois, on garde le favorable :
  - Googz : la clope se transforme si l'un des deux tirages réussit (20 % → 36 %) ;
  - NullPointerException : le plus favorable des deux résultats (null > rien > NO CARDS) ;
  - rareté des récompenses : pour chaque proposition, on garde la plus rare des deux.
- Pas concernés : la pioche de cartes, le choix des 3 récompenses (seule leur rareté l'est).

### Choix tranchés
- Ctrl+Z après la mort : impossible, la défaite est définitive (l'annulation ne se fait que pendant ton tour).
- Ctrl+Z au premier tour d'un combat : impossible (pas de retour au combat précédent ni à l'écran de récompense).
- CLAUDE.md : un secret débloqué pendant la partie gagnante compte comme « déjà débloqué » (on vérifie au
  moment de la victoire).
- `--seed null` : seed tirée au hasard, affichée comme d'habitude.
- Codes en mode texte : seulement « claude » (le terminal intercepte Ctrl+Z avant le jeu). En plein écran et
  en fenêtre, Lanterna reçoit Ctrl+Z comme une touche : le jeu n'est pas suspendu par Ctrl+Z.
- Une carte NO CARDS n'est jamais remplacée par George ni par un autre effet de remplacement.

# Cardistry

Jeu de cartes roguelite au tour par tour, en Java, joué dans le terminal (interface plein écran
[Lanterna](https://github.com/mabe02/lanterna)). Tourne sous **Linux, macOS et Windows**.

## Prérequis

- **JDK 21 ou plus récent** (`java -version` pour vérifier). Gradle n'a pas besoin d'être installé :
  le wrapper (`gradlew`, `gradlew.bat`) le télécharge.
- Un terminal d'au moins **80×24** pour le plein écran.

## Lancer le jeu

| OS | Commande |
|---|---|
| Linux, macOS | `./play` |
| Windows | `play.bat` |

Par défaut, le jeu s'ouvre dans **sa propre fenêtre** sur les trois OS : il y dessine lui-même sa grille
de caractères avec une police pixel embarquée (aucun scintillement), et peut afficher des images.

- `--terminal` : plein écran dans le terminal (Lanterna), comme avant. Il faut un vrai terminal d'au
  moins **80×24**.
- `--no-ui` : mode texte simple (lignes, saisie + Entrée).
- Sans affichage graphique (SSH, CI) : plein écran terminal si c'est un vrai terminal, sinon mode texte.

> `./gradlew run` lance aussi le jeu ; en `--terminal`, Gradle ne transmet pas un vrai terminal et le
> jeu passe en mode texte.

### Menus

- Écran titre : **Jouer**, **Règles**, **Options**, **Quitter** (↑/↓ ou ←/→, Entrée ; R et Q en raccourci).
- En jeu, **Échap** ouvre le menu **pause** (Reprendre, Options, Quitter) sur la carte, en combat, à la
  Sieste, dans un événement et sur les écrans de récompense. Dans un sous-écran (remplacer un objet,
  faire le tri, inventaire…), Échap annule d'abord ; au shop, Échap fait partir.
- **Options** : volume de la musique et des effets (0 à 10, ←/→, appliqué tout de suite) et son coupé.
  Sauvegardé dans `settings.txt`, à côté des déblocages. En mode texte : taper `options` à l'écran titre.

### Dans la fenêtre

| Touche | Effet |
|---|---|
| F10 | filtre CRT (lignes de balayage) : activer / couper |
| F11 | agrandir / restaurer la fenêtre |
| Ctrl + / Ctrl − (Cmd sous macOS) | taille de la police (multiples nets de la police pixel) |

La fenêtre se redimensionne librement (80×24 cellules au minimum). La fermer, ou Cmd+Q sous macOS,
quitte le jeu (les déblocages sont déjà sauvegardés).

### Options

| Option | Effet |
|---|---|
| `--seed N` | rejoue la partie de seed N |
| `--reset-unlocks` | efface les secrets débloqués, puis lance le jeu |
| `--sprites` | visionneuse : fait défiler les sprites des adversaires et leurs animations, sans partie |
| `--fast` | sans pauses ni animations |
| `--terminal` | plein écran dans le terminal au lieu de la fenêtre |
| `--no-ui` | mode texte simple |
| `--crt` | fenêtre : filtre CRT activé au départ (désactivé par défaut) |
| `--no-images` | fenêtre : sprites ASCII même quand une image existe |
| `--mute` | sans aucun son (cette session seulement) |
| `--no-music` | sans musique, effets gardés (cette session seulement) |
| `--ascii` | ASCII pur (cadres `+-|`, barres `#`, accents retirés) |
| `--no-color` | mode texte sans couleurs ANSI |
| `--window` | ancien nom de la fenêtre (c'est le mode par défaut) |

Exemples : `./play --seed 42 --crt`, `./play --terminal`, `play.bat --no-ui --no-color`.

## Particularités par OS

- **Linux et macOS**, en `--terminal` : l'ASCII pur s'active tout seul si le terminal n'est pas en UTF-8
  (ex. `LANG=C`) ; Ctrl+Z est une touche du jeu, il ne suspend pas le programme.
- **Windows**, en `--no-ui` dans la console : les couleurs ne sont activées que dans **Windows Terminal**
  ou un terminal de type Unix (Git Bash) ; dans la vieille console `cmd`, elles sont coupées
  automatiquement. Si la console n'est pas en UTF-8, le mode texte passe en ASCII pur (`chcp 65001` pour
  les accents). Lanterna 3.1 n'a pas de plein écran dans la console Windows : sous Windows, utiliser la
  fenêtre (par défaut).

## Polices

La fenêtre embarque deux polices, toutes deux sous **SIL Open Font License 1.1** (licences complètes dans
`src/main/resources/fonts/`) :

- **Departure Mono** 1.500 (© 2022–2024 Helena Zhang) — police pixel principale ;
  https://github.com/rektdeckard/departure-mono
- **JetBrains Mono** 2.304 (© 2020 The JetBrains Mono Project Authors) — secours pour les caractères
  absents de la première (▶) ; https://github.com/JetBrains/JetBrainsMono

## Sauvegarde

Le jeu range ses données (secrets débloqués, dans `unlocks.txt`, un fichier texte lisible) dans :

| OS | Dossier |
|---|---|
| Linux | `$XDG_DATA_HOME/cardistry`, sinon `~/.local/share/cardistry` |
| macOS | `~/Library/Application Support/Cardistry` |
| Windows | `%APPDATA%\Cardistry` |

Fichier absent ou abîmé : le jeu repart de zéro sans planter. `--reset-unlocks` remet tout à zéro.

## Sprites des adversaires

Un fichier texte par sprite : `src/main/resources/sprites/<id>.txt` (l'`id` est celui du registre des
adversaires, ex. `caddie-enerve`). Zone maximale : **30 colonnes × 10 lignes**. Le dessin est en
**ASCII pur** (un test le vérifie), donc il s'affiche tel quel avec `--ascii`.

```
@name Caddie énervé       nom affiché par la visionneuse (facultatif)
@# commentaire
@frame idle               une frame ; 2-3 blocs « idle » = animation au repos (en boucle)
...le dessin...
@colors                   facultatif : couleurs de la frame juste au-dessus
...une lettre par caractère du dessin
@frame idle
...
@frame attack             frames jouées quand l'adversaire attaque
@frame hurt               quand il prend des dégâts (sinon : clignotement)
@frame death              à sa mort (sinon : clignotement puis disparition)
```

- **Couleurs** : `r` rouge, `g` vert, `y` jaune, `b` bleu, `m` magenta, `c` cyan, `w` blanc, `k` gris ;
  en majuscule, la version vive. `.` ou espace : couleur de l'élément de l'adversaire.
- Une frame sans `@colors` reprend les couleurs de la première frame du fichier.
- Un fichier sans aucune directive est une seule frame idle, sans couleurs.
- Pas de fichier : sprite `inconnu.txt`. Choomah, Clarence et les boss ont un cadre provisoire à
  remplacer.
- Pour voir le résultat : `./play --sprites` (←/→ change de sprite, 1-4 joue idle / attaque / dégâts /
  mort, Q quitte ; en mode texte, les frames défilent).

## Images (fenêtre)

Dans la fenêtre, une image PNG peut remplacer un sprite ASCII ; sans image (ou avec `--terminal`,
`--no-images`), le sprite ASCII reste affiché.

- Adversaire : `src/main/resources/images/enemies/<id>.png` (même `id` que le sprite ASCII). Frames
  d'animation : `<id>.<animation>.<n>.png`, `n` à partir de 1, animations `idle`, `attack`, `hurt`,
  `death` (ex. `came.idle.1.png`, `came.idle.2.png`, `came.attack.1.png`). Une frame absente → `<id>.png`.
- L'image est centrée dans la zone du sprite (30×10 cellules, soit 210×140 pixels à la taille 1),
  agrandie d'un facteur **entier** sans lissage : dessiner en pixel art à petite taille (ex. 70×35).
  Les dégâts la font clignoter en blanc ; la mort l'estompe.
- Objet ou trinket : `src/main/resources/images/items/<id>.png` (icône de 2×1 cellules, ex. 14×14 px),
  affichée dans l'inventaire, les récompenses et le shop.
- `./play --sprites` montre l'image et son repli ASCII côte à côte.

## Sons et musiques

Le son passe par une seule sortie audio (44,1 kHz stéréo) : il marche dans la fenêtre, en `--terminal` et
en `--no-ui`, sous Linux (PipeWire, PulseAudio), macOS et Windows. Sans carte son, le jeu tourne
simplement en silence. Les tests ne jouent jamais de son.

**Les sons actuels sont des placeholders générés** (bips et petites boucles chiptune) :
`tools/generate-sounds.sh` les recrée (Java + ffmpeg avec libvorbis).

### Ajouter ou remplacer un son

1. Mettre le fichier dans `src/main/resources/audio/` (`sfx/` pour les effets, `music/` pour les
   musiques).
2. Le déclarer dans **`src/main/resources/audio/sounds.txt`**, la table nom → fichier :

   ```
   music.combat = music/combat.ogg ; loop=4.0      point de boucle (secondes)
   music.victory = music/victory.ogg ; loop=no     jouée une seule fois (jingle)
   sfx.card.fire = sfx/card-fire.wav ; volume=0.8  volume propre à ce son (0 à 1)
   sfx.choomah = sfx/choomah-1.wav, sfx/choomah-2.wav   variantes, jouées chacune à son tour
   ```

   Les noms (`music.*`, `sfx.*`) sont ceux que le jeu demande : garder le nom, changer le fichier. Un
   fichier ou une ligne absente donne du silence, jamais une erreur.

| | Effets | Musiques |
|---|---|---|
| Format | **WAV** PCM 16 bits | **OGG Vorbis** (qualité 4 à 6) ; WAV accepté |
| Fréquence | 44,1 kHz (22,05 kHz possible) | 44,1 kHz |
| Canaux | mono conseillé | stéréo |
| Niveau | crête vers **−3 dBFS** | environ **−16 LUFS** intégré, crêtes sous **−1 dBFS** |
| Durée | court ; **pas de silence au début** (sinon le son paraît en retard) | boucle de 30 s à 2 min |

- **Boucles** : la musique reprend au point `loop=` (0 par défaut) quand elle arrive au bout. Pour une
  boucle propre : couper sur un temps fort, sans fondu ni silence à la fin, et faire coïncider la fin
  avec le point de boucle (pas de clic : couper sur un passage à zéro). Une intro qui ne doit pas se
  répéter va avant le point de boucle.
- Les **sons fréquents** (curseur, pioche, gain de Choomies) sont réglés plus bas dans la table
  (`volume=0.35` à `0.4`) : garder ce genre de réglage pour qu'ils restent discrets.
- Changement d'écran : fondu enchaîné de 1,5 s entre deux musiques.
- Licence : n'ajouter que des sons dont on a le droit (les siens, CC0, CC-BY avec crédit dans ce README).

### Composants tiers

Polices et bibliothèques OGG (JOrbis, VorbisSPI, Tritonus-share, sous LGPL, livrées comme jars
séparés) : voir [`licenses/THIRD-PARTY.md`](licenses/THIRD-PARTY.md).

## Développement

- Tests : `./gradlew test` (Windows : `gradlew.bat test`). La CI GitHub Actions les lance sur Ubuntu,
  macOS et Windows.
- Les tests d'interface écrivent chaque écran testé dans `build/screens/*.txt`.
- Contexte et conventions du projet : [`CLAUDE.md`](CLAUDE.md) ; game design :
  [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md).

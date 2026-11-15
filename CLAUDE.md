# Cardistry — contexte pour Claude Code

Jeu de cartes **tour par tour, type roguelite**, en **Java**, joué dans le **terminal** avec une interface plein écran en ASCII (couleurs, sprites, animations).
Version personnelle de Louis, développée en parallèle de celle de David (dépôt d'origine davidou888/Cardistry) ; les deux versions seront comparées à la fin. Les décisions de game design sont prises par Louis. Le game design détaillé est dans [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — le lire avant toute feature de gameplay.

## État actuel du code

Prototype jouable, par défaut dans sa propre fenêtre en pixel art (carte, combat, cartes et icônes illustrées), sinon en plein écran terminal ou en mode texte : run de 3 niveaux sur une carte à embranchements (8 étages, boss au bout), deck construit (12 cartes au départ, pioche / défausse, récompense de carte après chaque combat), nouvelle main de 5 cartes par combat, récompense (objet ou trinket, 1 parmi 3) après chaque adversaire vaincu, adversaires à attaques annoncées (intents). Lancement : `./play` (voir « Commandes »).

- `cardistry.Main` — assemble tout, choisit l'affichage selon l'OS et les options (voir README.md)
- `hero/` — `Hero` (personnage jouable : stats de départ + hooks) et `HeroRegistry` (**registre** : Louis, Dave, Sassy, tous leurs chiffres). Choisi après le titre (`Input.chooseHero`), passé à `Run.setHero` ; `GameState.hero()` ; ses hooks sont appelés avant ceux des trinkets.
- `platform/` — `Os` (détection), `SaveDirectory` (dossier de sauvegarde par OS), `UnlockFile` (secrets débloqués, `unlocks.txt`, corrompu → vide)
- `run/` — `Run` (une partie : phases carte / combat / récompense, niveaux, codes, secrets débloqués dans le pool, Ctrl+Z en rejouant l'historique depuis la seed), `Secret`, `UnlockStore`, `UnlockTracker` (conditions de déblocage, écoute le bus). Spoilers : `docs/SECRETS.md`
- `run/map/` — `RunMap`, `MapNode`, `NodeType`, `MapGenerator` (carte d'un niveau, sous-flux de la seed), `MapView` (ce que l'UI affiche)
- `ui/MapLayout` — dessin de la carte en pixels (fenêtre) : nœuds décalés, chemins courbes en pointillés, sous-flux `carte-dessin-<niveau>` ; `ui/MapArt` — carte en lettres (terminal, mode texte)
- `ui/Glyphs` — repli ASCII pur (appliqué par `Painter` et `Console`)
- `core/` — `GameState` (dont l'inventaire, l'Ivresse et les malédictions), `Luck` (tirages de chance doublés par CLAUDE.md), `Curse` (malédiction d'un combat, avec intensité : Gueule de bois), `DrunkTier` (paliers d'Ivresse : Pompette / Bourré / Cuite, bonus de dégâts posé par `GameState`), `TemporaryEffect` (effets temporaires génériques : délai, durée en tours / charges, report entre combats, prochain sort ×N, annuler une attaque, sauter le prochain tour du joueur ou de l'adversaire), `CombatEngine` (règles, récompenses, aucun affichage), `Action` (jouer, piocher, utiliser un objet, fumer, rouler, annuler — `Undo` est traitée par `run.Run`, le moteur seul la refuse), `ActionResult`, `RewardChoice`, `PlayerDecisions` (décisions demandées en cours d'action : carte à garder, trinket à jeter ; implémentées par `ui.Input`), `EffectContext` (ce que les effets peuvent faire : dégâts bruts ou élémentaires, dégâts au joueur, soins bruts ou élémentaires, pioche, remplacement de carte, Ivresse, effets temporaires, adversaire null, carte morte, tirages de chance), `Rng`
- `core/event/` — `GameEvent` (records scellés), `EventBus` (en file : un événement publié en réaction est livré après l'événement en cours), `GameHooks` (reçoivent un `EffectContext`) + `HookDispatcher`. Le moteur transmet chaque événement aux hooks des trinkets équipés.
- `core/stats/` — `Stats`, `StatModifier` (plats + pourcentages qui s'additionnent + multiplicateurs finaux), `StatType`. Toutes branchées : PV max (PV de départ + plafond des soins), taille de main, cartes par pioche, emplacements de trinkets (5) et d'objets (3), dégâts par élément, efficacité des soins (+ soins par élément), réduction de dégâts, dégâts de l'adversaire, Choomies gagnés. Les autres stats sont déclarées mais pas encore lues par le moteur.
- `model/` — `Element` (deux triangles : feu > plante > eau > feu et géométrique > algébrique > spirituel > géométrique ; `GLITCH` « error: null » bat tout, lui-même compris ; `NEUTRAL` ne bat rien ; la nature s'affiche « plante » ; chaque élément a sa stat de dégâts et de soin, `StatType.allDamageMultipliers()` pour les bonus « tous dégâts »), `ElementRules` (**toutes** les tables de multiplicateurs), `Player`/`Enemy`, `enemy/` (`EnemyRegistry` = **registre des adversaires** avec scaling par niveau, `EnemyMove` + `MoveEffect` = attaques déclarées, `EnemyBehavior` = choix de l'intent, `EnemyRank`), `card/` (`Card` scellée, avec une rareté, dont `DeadCard` injouable et sans élément — « NO CARDS », « Exercice 3b » ; `CardPool` = registre + `startingDeck()` ; les tirages renvoient des copies)
- `items/` — `SetBonus` + `SetBonuses` (registre des sets, paliers cumulables), `Gauge` (jauge de trinket), `Rarity`, `Tag`, `Collectible` (scellée : `Item` consommable, `Trinket` passif = modificateurs de stats + hooks, construit par builder), `ItemRegistry` (**registre de tout le contenu**), `LootTable` (tirage pondéré par rareté, seedé ; secrets débloqués au poids 2 ; rareté doublée par CLAUDE.md), `NullPointerRoll`
- `ui/` — interfaces `Renderer`/`Input`, `GameLoop` (pilote un `run.Run`), `TitleChoice` (règles + codes de l'écran titre), `Format`, `ItemKeys` (W/E/R…, clavier QWERTZ), `QuitGameException` (quitter l'application : titre, fenêtre fermée) et `LeaveGameException` (abandonner la partie → retour au titre ; `Main.sessions` enchaîne les parties, une `Run` neuve à chaque fois)
  - `ui/lanterna/` — plein écran : `LanternaUi` (Renderer + Input + animations), `CombatScreen` (terminal), `CombatWindowScreen` (fenêtre : joueur à gauche, adversaire à droite, cartes illustrées, barre d'icônes aux couleurs de rareté, journal en panneau sur J), `MapScreen` (pixels dans la fenêtre, lettres en terminal), `MenuScreens` (titre, règles, fin, terminal trop petit), `ItemScreens` (récompense, remplacement, inventaire), `ShopScreen` (P partir, Échap = pause ; marchand à gauche des cartes dans une fenêtre ≥ 100×30, fumée animée au pixel près par `ShopScreen.smoke`, petit fumeur en texte sinon), `StoryScreen`, `Painter` (dessin), `ViewState` (sélection, journal, effets en cours). Minimum 80×24. Un écran qui dessine des images teste `p.hasImages()` et garde un rendu texte complet sinon (`--terminal`, `--no-images`). Animations de l'adversaire : `LanternaUi.enemyFrames` compte les frames PNG dans la fenêtre, celles du sprite ASCII sinon.
  - `ui/window/` — fenêtre par défaut : `CellWindow` (Swing, double buffer, redimensionnable, F10 CRT, F11, Ctrl ±) dessine elle-même un terminal virtuel Lanterna ; `GridRenderer` (grille → image, testable sans écran), `CellFonts` (Departure Mono + JetBrains Mono, OFL), `CellPalette`, `KeyMapper`, `ImageLayer` + `Images` (PNG alignés sur la grille, repli ASCII ; `ImageLayer.Mark` = dessin au pixel près en unités de 1/7 de colonne, posé par `Painter.fill` / `Painter.stamp`), `AppIcon` (icône provisoire). `LanternaUi` est le même dans la fenêtre et en `--terminal`.
  - `ui/audio/` — son : `AudioDirector` (écoute le bus → effets ; l'écran → la musique ; jamais appelé par la logique), `Audio` (+ `silent()`, muet automatique sans carte son et en test via `cardistry.audio=off`), `SoftwareAudio` (une sortie 44,1 kHz, un fil), `Mixer` (pur, testé : musique en boucle + fondu, 16 effets), `SoundTable` (`audio/sounds.txt`, **table éditable**), `Pcm` (WAV et OGG via VorbisSPI ; chaque fichier est normalisé au chargement à −18 dB RMS sur ses passages forts, sans saturer, donc `volume=` de la table ne sert qu'au mixage), `Volume` (échelle perceptive). Sons réels fournis par Louis (seule `music/victory.ogg` reste un placeholder) ; `tools/generate-sounds.sh` régénère des placeholders et écraserait les vrais sons.
  - `ui/Settings` + `ui/OptionsController` + `platform/SettingsFile` (`settings.txt`) : volumes et muet ; menu pause (Échap) et Options dans `LanternaUi`, commande `options` en mode texte
  - `ui/text/` — rendu texte simple (`--no-ui`, ou automatique si l'entrée n'est pas un vrai terminal)
- `ui/sprite/` — `Sprite` (format multi-frames + carte de couleurs, voir README), `Sprites` (chargement, catalogue de la visionneuse `--sprites`)
- `src/main/resources/sprites/<id>.txt` — sprites ASCII des adversaires (`inconnu.txt` par défaut ; terminal et mode texte) ; `banners/` — bannière du titre
- `src/main/resources/images/` — PNG de la fenêtre, chemins résolus par `ui.window.Images` : `enemies/<id>.<animation>.<n>.png` + `<id>.png` (64×64), `player/player.png` (64×64, placeholder ; `player/<id du personnage>.png` est pris s'il existe), `npc/<id>.idle.<n>.png` (64×64, le marchand du shop), `cards/<slug du nom>.png` (24×24), `items/<id>.png` (objets et trinkets, 16×16), `map/<type>.png` (16×16), `ui/` (icônes de type et d'intent 8×8, paquet de clopes, placeholders). Image absente → repli (icône de type teintée, placeholder de catégorie, sprite ASCII, lettre du nœud).
- `art/` — sources Aseprite (icônes : une frame taguée par icône, tag = id ; `art/enemies/<id>.aseprite` et `art/npc/<id>.aseprite` : un tag par animation), `palette.gpl`, planches ; `art/tools/generate.lua` ajoute les ébauches manquantes et **n'écrase jamais un tag existant** (Louis retouche les .aseprite à la main) ; `./gradlew art` = `generateArt` + `exportArt` (PNG commités dans `src/main/resources/images/`, ignorées sans Aseprite) + `planche` (`cardistry.tools.ArtSheet`, côté tests). Ébauches : `art/tools/items.txt`, `trinkets.txt` (grilles de lettres de palette), `cards.lua`, `enemies.lua`, `npcs.lua` (dessin par formes ; `standard(id, draw)` fabrique idle / attack / hurt / death). `cardistry.tools.AnimationCheck` (côté tests) : jeu auto-piloté dans la vraie fenêtre.
- Tests UI sur terminal virtuel (`LanternaUiTest`) ; ils écrivent chaque écran dans `build/screens/*.txt` (et le rendu fenêtre en `.png`) pour relire la mise en page. Les tests tournent en `java.awt.headless`.
- `core/Shop` + `core/shop/` — shop ouvert (articles, vendus, rerolls), `ShopPrices` (tous les prix), `ShopOffer`, `ShopAction` ; joué par `CombatEngine.shopAction`
- `story/` — événements des nœuds `?` : `Story` (texte + choix), `StoryChoice` (condition, conséquence), `StoryContext` (ce qu'une conséquence peut faire, fourni par le moteur), `StoryRegistry` (**registre**, 3 placeholders)

### Ajouter un objet ou un trinket
1. Une entrée dans `ItemRegistry` (nom **exactement** comme dans GAME_DESIGN.md). Trinket : `Trinket.builder(id, nom, rareté).description(…).tags(…)` puis `.percent(stat, x)` / `.flat(stat, x)` pour les stats, `.cardWeight(carte, facteur)` pour la fréquence d'une carte, `.multiply(stat, x)` pour un multiplicateur final, `.gauge(new Gauge(…))` pour une jauge affichée, `.boostsTag(tag, x)` pour amplifier un tag (Diplôme EPFL ; les hooks lisent leur puissance avec `ctx.potency(id)`), `.hooks(new GameHooks() { … })` pour réagir aux événements. Objet : `new Item(…, ctx -> …)` ; un effet qui dure passe par `ctx.addEffect(TemporaryEffect.builder(…)…build())`. Identifiants qualifiés `carte:` / `objet:` / `trinket:` : un objet peut porter le nom d'une carte.
2. Si l'effet a besoin d'une action que `EffectContext` n'offre pas, l'y ajouter (elle doit publier l'événement correspondant).
3. Un test par effet chiffré.

### Ajouter un personnage
Une entrée dans `HeroRegistry` (`new Hero(id, nom, capacité, description, modificateurs, hooks)`), ajoutée à `ALL`. Les capacités passent par des `StatModifier` et des `GameHooks`, comme un trinket ; `ctx.announce(…)` écrit une ligne dans le journal. Un test par effet chiffré (`HeroTest`).

### Ajouter une icône ou un sprite
1. Icône d'objet ou de trinket : une grille 16×16 « == id » dans `art/tools/items.txt` ou `trinkets.txt` ; illustration de carte : une entrée `{ id, draw }` dans `art/tools/cards.lua` (24×24) ; adversaire : `standard("<spriteId>", draw)` dans `art/tools/enemies.lua` (64×64, tourné vers la gauche). Dessins originaux, palette commune, aucun logo ni marque ; un trinket à prénom a une icône liée à son effet, pas un portrait.
2. `./gradlew art`, relire `art/planche.png` / `art/planche-ennemis.png`, commiter les `.aseprite` **et** les PNG.
3. Ne jamais régénérer un tag existant pour « l'améliorer » : Louis retouche les `.aseprite` à la main. Le test `toutLeContenuAUneIcone` échoue si un objet, un trinket, une carte ou un adversaire du registre n'a pas d'image.

### Dettes restantes
- Placeholders à dessiner par Louis : les trois personnages (`images/player/louis.png`, `dave.png`, `sassy.png` ; `player.png` en attendant), Choomah, Clarence, le boss (un sprite pour les trois niveaux, idle seulement).
- Les animations de mort des ébauches sont la pose « touché » écrasée vers le sol.
- En fenêtre 80×24, un adversaire sans PNG aurait son sprite ASCII coupé (7 lignes disponibles) ; aucun dans le registre aujourd'hui.
- Les états à adversaires fixés (tests) n'ont pas de deck : leur pioche tire dans le pool, sans fin (`GameState.usesDeck()`).
- `CardPool` (model) dépend de `core.Rng` ; `core` et `items` dépendent l'un de l'autre (inventaire dans `GameState`, effets via `EffectContext`).

## Architecture cible

```
build.gradle(.kts) + wrapper Gradle
src/main/java/cardistry/
  Main.java
  core/      GameState, Rng (seedé), EventBus/GameEvent, Stats + modificateurs
  model/     Element (enum), cartes, Player, Enemy
  items/     Item (consommable), Trinket (passif), Rarity, effets, synergies, registre
  run/       Run, carte à embranchements (nœuds), récompenses, Shop
  ui/        interface Renderer/Input, implémentation Lanterna, sprites ASCII, animations
src/main/resources/sprites/   sprites ASCII en .txt
src/test/java/...             JUnit 5
```

### Règles d'architecture
- **La logique n'affiche jamais rien.** Pas de `System.out` ni de `Thread.sleep` hors de `ui/`. La logique émet des événements (`DamageDealt`, `CardPlayed`, `ItemBought`…) ; l'UI les consomme pour afficher/animer. C'est ce qui permet de brancher Lanterna, de tester, et de faire réagir les trinkets.
- **Tout l'aléatoire passe par un `Rng` seedé unique** par run → runs reproductibles (debug, seeds partageables, easter eggs liés aux seeds).
- **Effets par hooks** : trinkets et objets réagissent aux événements (`onTurnStart`, `onCardPlayed`, `onDamageDealt`, `onHeal`, `onTerrainChanged`, `onEnemyKilled`, `onShopEnter`…) plutôt qu'avec des `if` disséminés dans le moteur.
- **Stats calculées, jamais écrasées** : stat finale = base + bonus plats, puis multiplicateurs, dans un ordre défini et documenté.
- Contenu (cartes, ennemis, objets, trinkets) déclaré dans des registres centralisés, faciles à étendre par quelqu'un qui ne connaît pas le moteur.

## Conventions
- Fork indépendante de davidou888/Cardistry. Ne jamais pousser, merger ni ouvrir de PR vers upstream.
- À la fin de chaque tâche, quand les tests passent, pousser sur origin sans demander.
- Le jeu doit tourner sur Linux, macOS et Windows : aucun chemin, raccourci clavier ou caractère spécifique à un OS sans repli.
- Java 21+ (le build compile en `release = 21`), Gradle avec wrapper, JUnit 5.
- UI terminal : **Lanterna 3.x** (vérifier la dernière version sur Maven Central).
- Noms de code en anglais, **textes affichés au joueur en français**.
- Petits commits ciblés, un sujet par commit. Ne pas réécrire tout l'historique git.
- Toute mécanique avec des chiffres (multiplicateurs, prix, raretés) a un test unitaire.
- En cas de doute de game design : **demander plutôt qu'inventer**, et noter la question dans `docs/GAME_DESIGN.md` (section « Questions ouvertes »).

## Commandes
- Build : `./gradlew build`
- Lancer : `./play` (Linux/macOS), `play.bat` (Windows) : fenêtre du jeu par défaut. Options : `--seed N`, `--reset-unlocks`, `--sprites`, `--fast`, `--terminal`, `--no-ui`, `--crt`, `--no-images`, `--mute`, `--no-music`, `--ascii`, `--no-color` (voir README.md). Pour tester sans toucher à la vraie sauvegarde : `XDG_DATA_HOME=/tmp/x ./play`. `./gradlew run` ne fournit pas de vrai terminal : le jeu y passe en mode texte.
- Mode texte : `./play --no-ui`
- CI : `.github/workflows/ci.yml` lance les tests sur Ubuntu, macOS et Windows (JDK 21).
- Paquets : `./gradlew jpackageImage` (app autonome), `./gradlew jpackage` (installateurs de l'OS courant ; JDK avec jmods requise). Release : tag `v*` → `.github/workflows/release.yml` (paquets des 3 OS → GitHub Release ; titre = message du tag annoté). Identifiant `io.github.megaray.cardistry`, éditeur « Cardistry ».
- Tests : `./gradlew test` ; captures de la fenêtre dans `build/screens/*.png` (`WindowScreenshotTest`) : les relire après tout changement de mise en page.
- Vérifier les animations dans la vraie fenêtre (jeu auto-piloté, trace sur la sortie) : `./gradlew animationCheck [-Penemies=id,id]`
- Art : `./gradlew art` (ébauches manquantes + export PNG + planches) ; détail : `generateArt`, `exportArt`, `planche`

# Cardistry — contexte pour Claude Code

Jeu de cartes **tour par tour, type roguelite**, en **Java**, joué dans le **terminal** avec une interface plein écran en ASCII (couleurs, sprites, animations).
Version personnelle de Louis, développée en parallèle de celle de David (dépôt d'origine davidou888/Cardistry) ; les deux versions seront comparées à la fin. Les décisions de game design sont prises par Louis. Le game design détaillé est dans [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — le lire avant toute feature de gameplay.

## État actuel du code

Prototype jouable en plein écran Lanterna : 3 combats (un par adversaire), nouvelle main de 5 cartes par combat, récompense (objet ou trinket, 1 parmi 3) après chaque adversaire vaincu. Lancement : `./play` (voir « Commandes »).

- `cardistry.Main` — assemble tout, choisit l'affichage selon l'OS et les options (voir README.md)
- `platform/` — `Os` (détection), `SaveDirectory` (dossier de sauvegarde par OS), `UnlockFile` (secrets débloqués, `unlocks.txt`, corrompu → vide)
- `run/` — `Run` (une partie : codes, secrets débloqués dans le pool, Ctrl+Z en rejouant l'historique depuis la seed), `Secret`, `UnlockStore`, `UnlockTracker` (conditions de déblocage, écoute le bus). Spoilers : `docs/SECRETS.md`
- `ui/Glyphs` — repli ASCII pur (appliqué par `Painter` et `Console`)
- `core/` — `GameState` (dont l'inventaire, l'Ivresse et les malédictions), `Luck` (tirages de chance doublés par CLAUDE.md), `Curse` (malédiction d'un combat, avec intensité : Gueule de bois), `TemporaryEffect` (effets temporaires génériques : délai, durée en tours / charges, report entre combats, prochain sort ×N, annuler une attaque, sauter le prochain tour du joueur ou de l'adversaire), `CombatEngine` (règles, récompenses, aucun affichage), `Action` (jouer, piocher, utiliser un objet, fumer, rouler, annuler — `Undo` est traitée par `run.Run`, le moteur seul la refuse), `ActionResult`, `RewardChoice`, `PlayerDecisions` (décisions demandées en cours d'action : carte à garder, trinket à jeter ; implémentées par `ui.Input`), `EffectContext` (ce que les effets peuvent faire : dégâts bruts ou élémentaires, dégâts au joueur, soins bruts ou élémentaires, pioche, remplacement de carte, Ivresse, effets temporaires, adversaire null, carte morte, tirages de chance), `Rng`
- `core/event/` — `GameEvent` (records scellés), `EventBus` (en file : un événement publié en réaction est livré après l'événement en cours), `GameHooks` (reçoivent un `EffectContext`) + `HookDispatcher`. Le moteur transmet chaque événement aux hooks des trinkets équipés.
- `core/stats/` — `Stats`, `StatModifier` (plats + pourcentages qui s'additionnent + multiplicateurs finaux), `StatType`. Toutes branchées : PV max (PV de départ + plafond des soins), taille de main, cartes par pioche, emplacements de trinkets (5) et d'objets (3), dégâts par élément, efficacité des soins (+ soins par élément), réduction de dégâts, dégâts de l'adversaire, Choomies gagnés. Les autres stats sont déclarées mais pas encore lues par le moteur.
- `model/` — `Element`, `ElementRules` (**toutes** les tables de multiplicateurs), `Player`/`Enemy`, `EnemyRoster`, `card/` (`Card` scellée, dont `DeadCard` « NO CARDS » injouable et sans élément ; `CardPool` = registre ; la pioche renvoie des copies)
- `items/` — `SetBonus` + `SetBonuses` (registre des sets, paliers cumulables), `Gauge` (jauge de trinket), `Rarity`, `Tag`, `Collectible` (scellée : `Item` consommable, `Trinket` passif = modificateurs de stats + hooks, construit par builder), `ItemRegistry` (**registre de tout le contenu**), `LootTable` (tirage pondéré par rareté, seedé ; secrets débloqués au poids 2 ; rareté doublée par CLAUDE.md), `NullPointerRoll`
- `ui/` — interfaces `Renderer`/`Input`, `GameLoop` (pilote un `run.Run`), `TitleChoice` (règles + codes de l'écran titre), `Format`, `ItemKeys` (W/E/R…, clavier QWERTZ), `QuitGameException`
  - `ui/lanterna/` — plein écran : `LanternaUi` (Renderer + Input + animations), `CombatScreen`, `MenuScreens` (titre, règles, fin, terminal trop petit), `ItemScreens` (récompense, remplacement, inventaire), `Painter` (dessin), `ViewState` (sélection, journal, effets en cours). Minimum 80×24.
  - `ui/text/` — rendu texte simple (`--no-ui`, ou automatique si l'entrée n'est pas un vrai terminal)
- `ui/sprite/` — `Sprite` (format multi-frames + carte de couleurs, voir README), `Sprites` (chargement, catalogue de la visionneuse `--sprites`)
- `src/main/resources/sprites/<id>.txt` — sprites des adversaires (`inconnu.txt` par défaut) ; `banners/` — bannière du titre
- Tests UI sur terminal virtuel (`LanternaUiTest`) ; ils écrivent chaque écran dans `build/screens/*.txt` pour relire la mise en page.
- `run/` reste à compléter : carte à embranchements, shop.

### Ajouter un objet ou un trinket
1. Une entrée dans `ItemRegistry` (nom **exactement** comme dans GAME_DESIGN.md). Trinket : `Trinket.builder(id, nom, rareté).description(…).tags(…)` puis `.percent(stat, x)` / `.flat(stat, x)` pour les stats, `.cardWeight(carte, facteur)` pour la fréquence d'une carte, `.multiply(stat, x)` pour un multiplicateur final, `.gauge(new Gauge(…))` pour une jauge affichée, `.boostsTag(tag, x)` pour amplifier un tag (Diplôme EPFL ; les hooks lisent leur puissance avec `ctx.potency(id)`), `.hooks(new GameHooks() { … })` pour réagir aux événements. Objet : `new Item(…, ctx -> …)` ; un effet qui dure passe par `ctx.addEffect(TemporaryEffect.builder(…)…build())`. Identifiants qualifiés `carte:` / `objet:` / `trinket:` : un objet peut porter le nom d'une carte.
2. Si l'effet a besoin d'une action que `EffectContext` n'offre pas, l'y ajouter (elle doit publier l'événement correspondant).
3. Un test par effet chiffré.

### Dettes restantes
- Pioche infinie avec remise (pas de vrai deck/défausse), gardée volontairement pour l'instant.
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
- Java 17+ (vérifier `java -version` sur la machine), Gradle avec wrapper, JUnit 5.
- UI terminal : **Lanterna 3.x** (vérifier la dernière version sur Maven Central).
- Noms de code en anglais, **textes affichés au joueur en français**.
- Petits commits ciblés, un sujet par commit. Ne pas réécrire tout l'historique git.
- Toute mécanique avec des chiffres (multiplicateurs, prix, raretés) a un test unitaire.
- En cas de doute de game design : **demander plutôt qu'inventer**, et noter la question dans `docs/GAME_DESIGN.md` (section « Questions ouvertes »).

## Commandes
- Build : `./gradlew build`
- Lancer : `./play` (Linux/macOS, plein écran terminal), `play.bat` (Windows, fenêtre Lanterna). Options : `--seed N`, `--reset-unlocks`, `--sprites`, `--fast`, `--no-ui`, `--window`, `--ascii`, `--no-color` (voir README.md). Pour tester sans toucher à la vraie sauvegarde : `XDG_DATA_HOME=/tmp/x ./play`. `./gradlew run` ne fournit pas de vrai terminal : le jeu y passe en mode texte.
- Mode texte : `./play --no-ui`
- CI : `.github/workflows/ci.yml` lance les tests sur Ubuntu, macOS et Windows (JDK 21).
- Tests : `./gradlew test`

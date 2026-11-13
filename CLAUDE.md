# Cardistry — contexte pour Claude Code

Jeu de cartes **tour par tour, type roguelite**, en **Java**, joué dans le **terminal** avec une interface plein écran en ASCII (couleurs, sprites, animations).
Version personnelle de Louis, développée en parallèle de celle de David (dépôt d'origine davidou888/Cardistry) ; les deux versions seront comparées à la fin. Les décisions de game design sont prises par Louis. Le game design détaillé est dans [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — le lire avant toute feature de gameplay.

## État actuel du code

Prototype jouable en plein écran Lanterna : 3 combats (un par adversaire), nouvelle main de 5 cartes par combat, récompense (objet ou trinket, 1 parmi 3) après chaque adversaire vaincu. Lancement : `./play` (voir « Commandes »).

- `cardistry.Main` — assemble tout ; options `--seed N`, `--fast`, `--no-ui`
- `core/` — `GameState` (dont l'inventaire), `CombatEngine` (règles, récompenses, aucun affichage), `Action` (jouer, piocher, utiliser un objet), `ActionResult`, `RewardChoice`, `EffectContext` (ce que les effets peuvent faire), `Rng`
- `core/event/` — `GameEvent` (records scellés), `EventBus` (en file : un événement publié en réaction est livré après l'événement en cours), `GameHooks` (reçoivent un `EffectContext`) + `HookDispatcher`. Le moteur transmet chaque événement aux hooks des trinkets équipés.
- `core/stats/` — `Stats`, `StatModifier` (plats + pourcentages qui s'additionnent), `StatType`. Branchées : PV max (PV de départ + plafond des soins), taille de main, emplacements de trinkets (5) et d'objets (3), multiplicateur de dégâts par élément, efficacité des soins. Les autres stats sont déclarées mais pas encore lues par le moteur.
- `model/` — `Element`, `ElementRules` (**toutes** les tables de multiplicateurs), `Player`/`Enemy`, `EnemyRoster`, `card/` (`Card` scellée, `CardPool` = registre ; la pioche renvoie des copies)
- `items/` — `Rarity`, `Tag`, `Collectible` (scellée : `Item` consommable, `Trinket` passif = modificateurs de stats + hooks, construit par builder), `ItemRegistry` (**registre de tout le contenu**), `LootTable` (tirage pondéré par rareté, seedé)
- `ui/` — interfaces `Renderer`/`Input`, `GameLoop`, `Format`, `ItemKeys` (A/Z/E…), `QuitGameException`
  - `ui/lanterna/` — plein écran : `LanternaUi` (Renderer + Input + animations), `CombatScreen`, `MenuScreens` (titre, règles, fin, terminal trop petit), `ItemScreens` (récompense, remplacement, inventaire), `Painter` (dessin), `ViewState` (sélection, journal, effets en cours). Minimum 80×24.
  - `ui/text/` — rendu texte simple (`--no-ui`, ou automatique si l'entrée n'est pas un vrai terminal)
- `src/main/resources/sprites/<nom-sans-accents>.txt` — sprites des adversaires (`inconnu.txt` par défaut) ; `banners/` — bannière du titre
- Tests UI sur terminal virtuel (`LanternaUiTest`) ; ils écrivent chaque écran dans `build/screens/*.txt` pour relire la mise en page.
- Pas encore créés : `run/`.

### Ajouter un objet ou un trinket
1. Une entrée dans `ItemRegistry` (nom **exactement** comme dans GAME_DESIGN.md). Trinket : `Trinket.builder(id, nom, rareté).description(…).tags(…)` puis `.percent(stat, x)` / `.flat(stat, x)` pour les stats, `.hooks(new GameHooks() { … })` pour réagir aux événements. Objet : `new Item(…, ctx -> …)`.
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
- Java 17+ (vérifier `java -version` sur la machine), Gradle avec wrapper, JUnit 5.
- UI terminal : **Lanterna 3.x** (vérifier la dernière version sur Maven Central).
- Noms de code en anglais, **textes affichés au joueur en français**.
- Petits commits ciblés, un sujet par commit. Ne pas réécrire tout l'historique git.
- Toute mécanique avec des chiffres (multiplicateurs, prix, raretés) a un test unitaire.
- En cas de doute de game design : **demander plutôt qu'inventer**, et noter la question dans `docs/GAME_DESIGN.md` (section « Questions ouvertes »).

## Commandes
- Build : `./gradlew build`
- Lancer en plein écran : `./play` (options : `./play --seed 42 --fast`). `./gradlew run` ne fournit pas de vrai terminal : le jeu y passe en mode texte.
- Mode texte : `./play --no-ui`
- Tests : `./gradlew test`

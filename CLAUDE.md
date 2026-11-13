# Cardistry — contexte pour Claude Code

Jeu de cartes **tour par tour, type roguelite**, en **Java**, joué dans le **terminal** avec une interface plein écran en ASCII (couleurs, sprites, animations).
Projet à deux (Louis + David). Le game design détaillé est dans [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — le lire avant toute feature de gameplay.

## État actuel du code

Prototype jouable (un combat contre 3 adversaires), migré sur Gradle. Lancement : `./gradlew run` (`--args="--seed 42 --fast"` pour rejouer une seed sans pauses).

- `cardistry.Main` — assemble tout ; options `--seed N`, `--fast`
- `core/` — `GameState`, `CombatEngine` (règles, aucun affichage), `Action`, `Rng`
- `core/event/` — `GameEvent` (records scellés), `EventBus`, `GameHooks` + `HookDispatcher` (prêts, aucun trinket encore)
- `core/stats/` — `Stats`, `StatModifier`, `StatType`. Branchées : taille de main, multiplicateur de dégâts par élément, efficacité des soins. Les autres stats sont déclarées mais pas encore lues par le moteur.
- `model/` — `Element`, `ElementRules` (**toutes** les tables de multiplicateurs), `Player`/`Enemy`, `EnemyRoster`, `card/` (`Card` scellée, `CardPool` = registre ; la pioche renvoie des copies)
- `ui/` — interfaces `Renderer`/`Input`, `GameLoop` ; `ui/text/` = rendu texte provisoire (en attendant Lanterna)
- Pas encore créés : `items/`, `run/`, Lanterna.

### Dettes restantes
- Pioche infinie avec remise (pas de vrai deck/défausse) ; soins sans plafond de PV. Voir « Questions ouvertes » dans `docs/GAME_DESIGN.md`.
- `CardPool` (model) dépend de `core.Rng`.

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
- Java 17+ (vérifier `java -version` sur la machine), Gradle avec wrapper, JUnit 5.
- UI terminal : **Lanterna 3.x** (vérifier la dernière version sur Maven Central).
- Noms de code en anglais, **textes affichés au joueur en français**.
- Petits commits ciblés, un sujet par commit. Ne pas réécrire tout l'historique git.
- Toute mécanique avec des chiffres (multiplicateurs, prix, raretés) a un test unitaire.
- En cas de doute de game design : **demander plutôt qu'inventer**, et noter la question dans `docs/GAME_DESIGN.md` (section « Questions ouvertes »).

## Commandes
- Build : `./gradlew build`
- Lancer : `./gradlew run` (options : `--args="--seed 42 --fast"`). Le futur mode plein écran Lanterna aura besoin d'un vrai terminal ; garder le rendu texte (`ui/text`) comme mode `--no-ui`.
- Tests : `./gradlew test`

# Cardistry — contexte pour Claude Code

Jeu de cartes **tour par tour, type roguelite**, en **Java**, joué dans le **terminal** avec une interface plein écran en ASCII (couleurs, sprites, animations).
Projet à deux (Louis + David). Le game design détaillé est dans [`docs/GAME_DESIGN.md`](docs/GAME_DESIGN.md) — le lire avant toute feature de gameplay.

## État actuel du code

Prototype jouable mais monolithique :
- `Carte.java` — `Carte` (abstraite), `Sortilege`, `Terrain`, `Heal`, `Deck`
- `Magic.java` — `main`, `Jeu` (boucle + état + affichage mélangés), `Personnage`, `Player`, `Adversaire`, `Helper` (couleurs ANSI, pause), `Ecran` (fond RGB, inutilisé)
- Pas d'outil de build, pas de packages, pas de tests. Compilation actuelle : `javac *.java && java Magic`.
- Les commentaires en bas de `Magic.java` contiennent les tables d'éléments (feu > nature > eau > feu).

### Bugs / dettes connus (à traiter pendant le refactor)
1. Comparaison de `String` avec `==` (`typeRelation`, `getMult`) → remplacer par un `enum Element`.
2. **Les tables en commentaire ne correspondent pas au code** :
   - Sortilège même type que l'adversaire : code ×0.5, table ×1. Sortilège battu par l'adversaire : code ×1, table ×0.5.
   - Terrain : la table parle des dégâts *reçus* par l'adversaire, le code s'en sert comme multiplicateur de l'*attaque* de l'adversaire.
   → Ne pas trancher seul : signaler et demander quelle version est la bonne.
3. `scanner.nextInt()` plante sur une saisie non numérique ; un `Scanner` est recréé à chaque boucle.
4. `Deck.piocher()` renvoie la même instance partagée → cassera dès qu'une carte pourra être améliorée/modifiée. Pioche infinie avec remise (pas de vrai deck/défausse).
5. L'intro annonce un « Dragon » mais l'adversaire s'appelle « Hydre ».
6. Pas de message de défaite quand le joueur meurt ; « Welcome to GAME NAME ».
7. Quand un adversaire meurt, le suivant attaque dans le même tour (intentionnel ? à confirmer).
8. Fautes : « Antlantis », « élélments », « éfficaces », « comemncer ».

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
- Lancer : `./gradlew run` (le jeu plein écran a besoin d'un vrai terminal ; prévoir un mode texte simple / `--no-ui` pour tester sans Lanterna)
- Tests : `./gradlew test`

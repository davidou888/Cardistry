package cardistry.core;

import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.EnemyRoster;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** L'état d'une partie. Lisible par tous, modifiable uniquement par le moteur (package core). */
public final class GameState {

    private final Player player;
    private final List<Enemy> enemies;
    private int currentEnemyIndex = 0;
    private TerrainCard terrain;
    private final List<Card> hand;
    private final Stats playerStats;
    private int turn = 0;

    public GameState(Player player, List<Enemy> enemies, TerrainCard terrain, List<Card> hand, Stats playerStats) {
        if (enemies.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins un adversaire");
        }
        this.player = player;
        this.enemies = List.copyOf(enemies);
        this.terrain = terrain;
        this.hand = new ArrayList<>(hand);
        this.playerStats = playerStats;
    }

    /** Nouvelle partie : adversaires par défaut, terrain de départ, main pleine. */
    public static GameState newGame(CardPool pool, Rng rng) {
        Stats stats = new Stats();
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < stats.getInt(StatType.HAND_SIZE); i++) {
            hand.add(pool.draw(rng));
        }
        return new GameState(new Player(), EnemyRoster.createDefault(), CardPool.startingTerrain(), hand, stats);
    }

    public Player player() {
        return player;
    }

    public List<Enemy> enemies() {
        return enemies;
    }

    public Enemy currentEnemy() {
        return enemies.get(currentEnemyIndex);
    }

    public TerrainCard terrain() {
        return terrain;
    }

    /** Vue non modifiable de la main. */
    public List<Card> hand() {
        return Collections.unmodifiableList(hand);
    }

    /** Taille maximale de la main (stat {@link StatType#HAND_SIZE}). */
    public int handSize() {
        return playerStats.getInt(StatType.HAND_SIZE);
    }

    /** Stats du joueur : c'est ici que trinkets et objets ajouteront leurs modificateurs. */
    public Stats playerStats() {
        return playerStats;
    }

    public int turn() {
        return turn;
    }

    /** Multiplicateur actuel de l'attaque de l'adversaire (dépend du terrain). */
    public double enemyAttackMultiplier() {
        return ElementRules.terrainAttackMultiplier(terrain.element(), currentEnemy().element());
    }

    public boolean isWon() {
        return enemies.stream().noneMatch(Enemy::isAlive);
    }

    public boolean isLost() {
        return !player.isAlive();
    }

    // --- Mutations réservées au moteur ---

    boolean hasNextEnemy() {
        return currentEnemyIndex < enemies.size() - 1;
    }

    void advanceEnemy() {
        currentEnemyIndex++;
    }

    void setTerrain(TerrainCard terrain) {
        this.terrain = terrain;
    }

    Card removeFromHand(int slot) {
        return hand.remove(slot);
    }

    void addToHand(Card card) {
        hand.add(card);
    }

    void nextTurn() {
        turn++;
    }
}

package cardistry.core;

import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.Trinket;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.EnemyRoster;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** L'état d'une partie. Lisible par tous, modifiable uniquement par le moteur (package core). */
public final class GameState {

    /** Cartes distribuées au début de chaque combat. */
    public static final int STARTING_HAND = 5;

    private final Player player;
    private final List<Enemy> enemies;
    private int currentEnemyIndex = 0;
    private TerrainCard terrain;
    private final List<Card> hand;
    private final Stats playerStats;
    private int turn = 0;
    private int combatTurn = 0;
    private final List<Trinket> trinkets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private int drunkenness = 0;
    private boolean hangoverThisCombat = false;
    private final java.util.Set<Curse> pendingCurses = java.util.EnumSet.noneOf(Curse.class);
    private final java.util.Set<Curse> activeCurses = java.util.EnumSet.noneOf(Curse.class);

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

    /** Nouvelle partie : adversaires par défaut, terrain de départ, main vide (distribuée au début du combat). */
    public static GameState newGame() {
        Stats stats = new Stats();
        return new GameState(new Player(stats.get(StatType.MAX_HP)), EnemyRoster.createDefault(),
                CardPool.startingTerrain(), List.of(), stats);
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

    /** PV max du joueur (stat {@link StatType#MAX_HP}) : les soins ne vont pas au-delà. */
    public double maxHp() {
        return playerStats.get(StatType.MAX_HP);
    }

    /** Stats du joueur : c'est ici que trinkets et objets ajouteront leurs modificateurs. */
    public Stats playerStats() {
        return playerStats;
    }

    // --- Inventaire ---

    /** Trinkets équipés (vue non modifiable). */
    public List<Trinket> trinkets() {
        return Collections.unmodifiableList(trinkets);
    }

    /** Objets en inventaire, dans l'ordre des emplacements (vue non modifiable). */
    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    public int trinketSlots() {
        return playerStats.getInt(StatType.TRINKET_SLOTS);
    }

    public int itemSlots() {
        return playerStats.getInt(StatType.ITEM_SLOTS);
    }

    public boolean hasTrinket(String id) {
        return trinkets.stream().anyMatch(t -> t.id().equals(id));
    }

    // --- Ivresse et malédictions ---

    /** Ivresse actuelle (remise à 0 à la fin de chaque combat). */
    public int drunkenness() {
        return drunkenness;
    }

    /** Ivresse à partir de laquelle on attrape une Gueule de bois (stat {@link StatType#HANGOVER_THRESHOLD}). */
    public int hangoverThreshold() {
        return playerStats.getInt(StatType.HANGOVER_THRESHOLD);
    }

    /** Malédictions actives pendant ce combat. */
    public java.util.Set<Curse> activeCurses() {
        return java.util.Collections.unmodifiableSet(activeCurses);
    }

    /** Malédictions qui s'activeront au prochain combat. */
    public java.util.Set<Curse> pendingCurses() {
        return java.util.Collections.unmodifiableSet(pendingCurses);
    }

    /** Nombre total de tours joués dans la partie. */
    public int turn() {
        return turn;
    }

    /** Tour dans le combat actuel, à partir de 1 (0 avant le premier tour du combat). */
    public int combatTurn() {
        return combatTurn;
    }

    /** Numéro du combat actuel, à partir de 1. */
    public int combatNumber() {
        return currentEnemyIndex + 1;
    }

    /** Multiplicateur total d'un sortilège contre l'adversaire actuel (élément × stats). */
    public double spellMultiplier(SpellCard spell) {
        return ElementRules.spellMultiplier(spell.element(), currentEnemy().element())
                * playerStats.get(StatType.damageMultiplier(spell.element()));
    }

    /** Multiplicateur total d'un soin sur le terrain actuel (terrain × efficacité × soin de l'élément). */
    public double healMultiplier(HealCard heal) {
        return ElementRules.healMultiplier(heal.element(), terrain.element())
                * playerStats.get(StatType.HEAL_EFFICIENCY)
                * playerStats.get(StatType.healMultiplier(heal.element()));
    }

    /** Multiplicateur actuel de l'attaque de l'adversaire (terrain × stat {@link StatType#ENEMY_DAMAGE}). */
    public double enemyAttackMultiplier() {
        return ElementRules.terrainAttackMultiplier(terrain.element(), currentEnemy().element())
                * playerStats.get(StatType.ENEMY_DAMAGE);
    }

    /** Dégâts que l'adversaire infligera à sa prochaine attaque (multiplicateur, puis réduction). */
    public double enemyAttackDamage() {
        return Math.max(0, currentEnemy().attack() * enemyAttackMultiplier()
                - playerStats.get(StatType.DAMAGE_REDUCTION));
    }

    /**
     * Poids d'une carte dans la pioche : produit des facteurs des trinkets équipés
     * (1 = fréquence normale, 2 = deux fois plus fréquente).
     */
    public double cardWeight(Card card) {
        double weight = 1.0;
        for (Trinket trinket : trinkets) {
            weight *= trinket.cardWeight(card);
        }
        return weight;
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

    Card replaceInHand(int slot, Card card) {
        return hand.set(slot, card);
    }

    void clearHand() {
        hand.clear();
    }

    void nextTurn() {
        turn++;
        combatTurn++;
    }

    void resetCombatTurn() {
        combatTurn = 0;
    }

    /** Équipe un trinket et applique ses modificateurs. */
    void equipTrinket(Trinket trinket) {
        if (trinkets.size() >= trinketSlots()) {
            throw new IllegalStateException("Plus d'emplacement de trinket");
        }
        trinkets.add(trinket);
        trinket.modifiers().forEach(playerStats::add);
    }

    /** Retire un trinket et ses modificateurs. */
    Trinket unequipTrinket(int slot) {
        Trinket trinket = trinkets.remove(slot);
        playerStats.removeBySource(trinket.modifierSource());
        return trinket;
    }

    void addItem(Item item) {
        if (items.size() >= itemSlots()) {
            throw new IllegalStateException("Plus d'emplacement d'objet");
        }
        items.add(item);
    }

    Item removeItem(int slot) {
        return items.remove(slot);
    }

    void setDrunkenness(int value) {
        drunkenness = value;
    }

    boolean hangoverThisCombat() {
        return hangoverThisCombat;
    }

    void setHangoverThisCombat(boolean value) {
        hangoverThisCombat = value;
    }

    void addPendingCurse(Curse curse) {
        pendingCurses.add(curse);
    }

    /** Active les malédictions en attente (modificateurs compris) et les renvoie. */
    java.util.List<Curse> activatePendingCurses() {
        java.util.List<Curse> activated = java.util.List.copyOf(pendingCurses);
        for (Curse curse : activated) {
            activeCurses.add(curse);
            curse.modifiers().forEach(playerStats::add);
        }
        pendingCurses.clear();
        return activated;
    }

    /** Termine les malédictions actives (modificateurs retirés) et les renvoie. */
    java.util.List<Curse> endActiveCurses() {
        java.util.List<Curse> ended = java.util.List.copyOf(activeCurses);
        for (Curse curse : ended) {
            playerStats.removeBySource(curse.modifierSource());
        }
        activeCurses.clear();
        return ended;
    }
}

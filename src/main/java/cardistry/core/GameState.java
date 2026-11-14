package cardistry.core;

import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.core.stats.StatModifier;
import cardistry.items.Item;
import cardistry.items.SetBonus;
import cardistry.items.SetBonuses;
import cardistry.items.Tag;
import cardistry.items.Trinket;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
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

    /** Cartes distribuées au début de chaque combat (valeur de base de la stat {@link StatType#STARTING_HAND}). */
    public static final int STARTING_HAND = 5;

    /** Palier de set actif. */
    public record ActiveSetTier(SetBonus set, SetBonus.Tier tier) {
    }

    /** Changement de palier depuis le dernier recalcul (pour les notifications). */
    record SetChange(ActiveSetTier tier, boolean activated) {
    }

    /** Clopes au début de chaque partie. */
    public static final int STARTING_CIGARETTES = 5;

    /** Choomies au début de chaque partie. */
    public static final int STARTING_CHOOMIES = 15;

    private final Player player;
    private final List<Enemy> enemies;
    private final boolean openEnded;
    private int currentEnemyIndex = 0;
    private boolean finalCombat = false;
    private boolean won = false;
    private int choomies = STARTING_CHOOMIES;
    private int level = 0;
    private int floor = 0;
    /** Deck de la run ; {@code null} = pas de deck, la pioche tire dans le pool (tests, scénarios). */
    private final List<Card> deck;
    private final List<Card> drawPile = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    private TerrainCard terrain;
    private final List<Card> hand;
    private final Stats playerStats;
    private int turn = 0;
    private int combatTurn = 0;
    private final List<Trinket> trinkets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private int drunkenness = 0;
    private boolean hangoverThisCombat = false;
    private final java.util.Map<Curse, Integer> pendingCurses = new java.util.EnumMap<>(Curse.class);
    private final java.util.Map<Curse, Integer> activeCurses = new java.util.EnumMap<>(Curse.class);
    private int cigarettes = STARTING_CIGARETTES;
    private final List<SetBonus> setBonuses = SetBonuses.all();
    private List<ActiveSetTier> activeSetTiers = List.of();
    private final List<SetChange> pendingSetChanges = new ArrayList<>();
    private final List<TemporaryEffect> effects = new ArrayList<>();
    private boolean undoUsedThisCombat = false;

    /**
     * Partie à adversaires fixés : ils sont affrontés dans l'ordre, une récompense entre chacun, et
     * la partie est gagnée quand le dernier meurt (tests, scénarios).
     */
    public GameState(Player player, List<Enemy> enemies, TerrainCard terrain, List<Card> hand, Stats playerStats) {
        this(player, enemies, false, terrain, hand, playerStats);
        if (enemies.isEmpty()) {
            throw new IllegalArgumentException("Il faut au moins un adversaire");
        }
    }

    private GameState(Player player, List<Enemy> enemies, boolean openEnded, TerrainCard terrain, List<Card> hand,
            Stats playerStats) {
        this(player, enemies, openEnded, terrain, hand, playerStats, null);
    }

    private GameState(Player player, List<Enemy> enemies, boolean openEnded, TerrainCard terrain, List<Card> hand,
            Stats playerStats, List<Card> deck) {
        this.deck = deck == null ? null : new ArrayList<>(deck);
        this.player = player;
        this.enemies = new ArrayList<>(enemies);
        this.openEnded = openEnded;
        this.terrain = terrain;
        this.hand = new ArrayList<>(hand);
        this.playerStats = playerStats;
    }

    /**
     * Partie ouverte : aucun adversaire au départ, chaque combat est lancé de l'extérieur
     * ({@code CombatEngine.startCombat}, par la carte de la run).
     */
    public static GameState openRun(Player player, TerrainCard terrain, Stats playerStats) {
        return new GameState(player, List.of(), true, terrain, List.of(), playerStats);
    }

    /** Partie ouverte avec un deck : chaque combat pioche dans une copie mélangée du deck. */
    public static GameState openRun(Player player, TerrainCard terrain, Stats playerStats, List<Card> deck) {
        return new GameState(player, List.of(), true, terrain, List.of(), playerStats, deck);
    }

    /** Nouvelle partie (carte de la run) : terrain de départ, deck de départ, aucun adversaire encore. */
    public static GameState newGame() {
        return newGame(CardPool.startingDeck());
    }

    /** Nouvelle partie avec le deck {@code deck} (tests). */
    public static GameState newGame(List<Card> deck) {
        Stats stats = new Stats();
        return openRun(new Player(stats.get(StatType.MAX_HP)), CardPool.startingTerrain(), stats, deck);
    }

    // --- Deck ---

    /** {@code true} si la partie a un deck (sinon la pioche tire dans le pool, sans fin). */
    public boolean usesDeck() {
        return deck != null;
    }

    /** Deck de la run (vue non modifiable ; vide sans deck). */
    public List<Card> deck() {
        return deck == null ? List.of() : Collections.unmodifiableList(deck);
    }

    /** Pioche du combat en cours (le dessus est le dernier élément). */
    public List<Card> drawPile() {
        return Collections.unmodifiableList(drawPile);
    }

    /** Défausse du combat en cours. */
    public List<Card> discardPile() {
        return Collections.unmodifiableList(discardPile);
    }

    void addToDeck(Card card) {
        deck.add(card);
    }

    Card removeFromDeck(int index) {
        return deck.remove(index);
    }

    /** Pioche neuve : copies du deck, mélangées ; défausse vidée. */
    void resetPiles(Rng rng) {
        drawPile.clear();
        discardPile.clear();
        for (Card card : deck) {
            drawPile.add(card.copy());
        }
        shuffle(drawPile, rng);
    }

    /** Défausse remélangée dans la pioche ; renvoie le nombre de cartes. */
    int reshuffleDiscard(Rng rng) {
        drawPile.addAll(discardPile);
        discardPile.clear();
        shuffle(drawPile, rng);
        return drawPile.size();
    }

    Card takeFromDrawPile() {
        return drawPile.remove(drawPile.size() - 1);
    }

    void discard(Card card) {
        if (!(card instanceof cardistry.model.card.DeadCard)) {
            discardPile.add(card);
        }
    }

    void clearPiles() {
        drawPile.clear();
        discardPile.clear();
    }

    /** Fisher-Yates avec le Rng seedé. */
    private static void shuffle(List<Card> cards, Rng rng) {
        for (int i = cards.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Card tmp = cards.get(i);
            cards.set(i, cards.get(j));
            cards.set(j, tmp);
        }
    }

    public Player player() {
        return player;
    }

    /** Adversaires affrontés ou à affronter (partie fixe), ou déjà rencontrés (partie ouverte). */
    public List<Enemy> enemies() {
        return Collections.unmodifiableList(enemies);
    }

    /** {@code true} si les combats sont lancés de l'extérieur (carte de la run). */
    public boolean isOpenEnded() {
        return openEnded;
    }

    /** {@code true} si un adversaire est en jeu (un combat a commencé). */
    public boolean hasEnemy() {
        return currentEnemyIndex < enemies.size();
    }

    public Enemy currentEnemy() {
        return enemies.get(currentEnemyIndex);
    }

    /** Niveau de la carte (1 à 3) ; 0 hors carte (partie à adversaires fixés). */
    public int level() {
        return level;
    }

    /** Étage atteint sur la carte du niveau (0 avant le premier nœud). */
    public int floor() {
        return floor;
    }

    void setLocation(int level, int floor) {
        this.level = level;
        this.floor = floor;
    }

    /** Choomies (monnaie de la run). */
    public int choomies() {
        return choomies;
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

    /** {@code true} si les soins peuvent dépasser les PV max (stat {@link StatType#UNCAPPED_HEALING}). */
    public boolean healingUncapped() {
        return playerStats.get(StatType.UNCAPPED_HEALING) > 0;
    }

    /** {@code true} si les tirages de chance sont faits deux fois (CLAUDE.md, stat {@link StatType#LUCKY_ROLLS}). */
    public boolean luckyRolls() {
        return playerStats.get(StatType.LUCKY_ROLLS) > 0;
    }

    /** {@code true} si Ctrl+Z (trinket) a déjà servi pendant ce combat : une fois par combat. */
    public boolean undoUsedThisCombat() {
        return undoUsedThisCombat;
    }

    void setUndoUsedThisCombat(boolean value) {
        undoUsedThisCombat = value;
    }

    /**
     * {@code true} pendant le dernier combat de la partie : le boss final (carte de la run), ou le
     * dernier adversaire d'une partie fixe.
     */
    public boolean isFinalCombat() {
        return openEnded ? finalCombat : !hasNextEnemy();
    }

    /** {@code true} si l'adversaire actuel est immunisé contre la mort instantanée (boss, dernier combat). */
    public boolean isBossFight() {
        return hasEnemy() && (currentEnemy().rank() == cardistry.model.enemy.EnemyRank.BOSS || isFinalCombat());
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

    // --- Sets ---

    /**
     * Puissance d'un trinket : produit des amplifications de ses tags par les <b>autres</b>
     * trinkets équipés (Diplôme EPFL : ×2 sur les trinkets EPFL). 1 par défaut.
     */
    public double potency(Trinket trinket) {
        double potency = 1.0;
        for (Trinket other : trinkets) {
            if (other != trinket) {
                for (Tag tag : trinket.tags()) {
                    potency *= other.boostFor(tag);
                }
            }
        }
        return potency;
    }

    /** Amplification du bonus de set d'un tag (produit des {@code boostFor} des trinkets équipés). */
    public double setPotency(Tag tag) {
        double potency = 1.0;
        for (Trinket trinket : trinkets) {
            potency *= trinket.boostFor(tag);
        }
        return potency;
    }

    /** Modificateur amplifié : plats et pourcentages ×p ; multiplicateur final f → 1 + (f − 1) × p. */
    private static StatModifier scaled(StatModifier m, double potency, String source) {
        double value = m.kind() == StatModifier.Kind.FINAL ? 1 + (m.value() - 1) * potency : m.value() * potency;
        return new StatModifier(m.stat(), m.kind(), value, source);
    }

    /** Réapplique les modificateurs de tous les trinkets avec leur puissance actuelle, puis les sets. */
    private void refreshInventoryModifiers() {
        for (Trinket trinket : trinkets) {
            playerStats.removeBySource(trinket.modifierSource());
            double potency = potency(trinket);
            for (StatModifier m : trinket.modifiers()) {
                playerStats.add(scaled(m, potency, trinket.modifierSource()));
            }
        }
        refreshSets();
    }

    /** Nombre de trinkets équipés portant {@code tag}. */
    public int tagCount(Tag tag) {
        return (int) trinkets.stream().filter(t -> t.tags().contains(tag)).count();
    }

    /** Tous les sets connus. */
    public List<SetBonus> setBonuses() {
        return setBonuses;
    }

    /** Paliers de set actifs (les paliers se cumulent). */
    public List<ActiveSetTier> activeSetTiers() {
        return activeSetTiers;
    }

    /** Recalcule les modificateurs de set et note les paliers qui s'activent ou se désactivent. */
    private void refreshSets() {
        for (ActiveSetTier active : activeSetTiers) {
            playerStats.removeBySource(active.set().source(active.tier()));
        }
        List<ActiveSetTier> now = new ArrayList<>();
        for (SetBonus set : setBonuses) {
            double potency = setPotency(set.tag());
            for (SetBonus.Tier tier : set.reached(tagCount(set.tag()))) {
                now.add(new ActiveSetTier(set, tier));
                for (StatModifier m : tier.modifiers()) {
                    playerStats.add(scaled(m, potency, set.source(tier)));
                }
            }
        }
        for (ActiveSetTier tier : now) {
            if (!activeSetTiers.contains(tier)) {
                pendingSetChanges.add(new SetChange(tier, true));
            }
        }
        for (ActiveSetTier tier : activeSetTiers) {
            if (!now.contains(tier)) {
                pendingSetChanges.add(new SetChange(tier, false));
            }
        }
        activeSetTiers = List.copyOf(now);
    }

    /** Changements de palier depuis le dernier appel. */
    List<SetChange> drainSetChanges() {
        List<SetChange> changes = List.copyOf(pendingSetChanges);
        pendingSetChanges.clear();
        return changes;
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
        return java.util.Collections.unmodifiableSet(activeCurses.keySet());
    }

    /** Intensité d'une malédiction active (0 si absente). */
    public int activeIntensity(Curse curse) {
        return activeCurses.getOrDefault(curse, 0);
    }

    /** Intensité d'une malédiction à venir (0 si absente). */
    public int pendingIntensity(Curse curse) {
        return pendingCurses.getOrDefault(curse, 0);
    }

    // --- Clopes ---

    /** Clopes dans le paquet. */
    public int cigarettes() {
        return cigarettes;
    }

    /** Taille du paquet (stat {@link StatType#CIGARETTE_MAX}). */
    public int cigaretteMax() {
        return playerStats.getInt(StatType.CIGARETTE_MAX);
    }

    /** Malédictions qui s'activeront au prochain combat. */
    public java.util.Set<Curse> pendingCurses() {
        return java.util.Collections.unmodifiableSet(pendingCurses.keySet());
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
        return Math.min(currentEnemyIndex, enemies.size() - 1) + 1;
    }

    /** Multiplicateur total d'un sortilège contre l'adversaire actuel (élément × stats). */
    public double spellMultiplier(SpellCard spell) {
        return ElementRules.spellMultiplier(spell.element(), currentEnemy().element())
                * playerStats.get(StatType.damageMultiplier(spell.element()))
                * nextSpellBoost().map(TemporaryEffect::spellMultiplier).orElse(1.0);
    }

    /** Effet actif qui multipliera le prochain sort (le plus ancien), s'il y en a un. */
    public java.util.Optional<TemporaryEffect> nextSpellBoost() {
        return effects.stream().filter(e -> e.isActive() && e.spellMultiplier() != 1.0).findFirst();
    }

    /** Effets temporaires en cours (en attente ou actifs), dans l'ordre d'arrivée. */
    public List<TemporaryEffect> effects() {
        return Collections.unmodifiableList(effects);
    }

    /** Multiplicateur total d'un soin sur le terrain actuel (terrain × efficacité × soin de l'élément). */
    public double healMultiplier(HealCard heal) {
        return healMultiplier(heal.element());
    }

    /** Multiplicateur d'un soin de l'élément {@code element} (carte ou objet « soin feu »). */
    public double healMultiplier(cardistry.model.Element element) {
        return ElementRules.healMultiplier(element, terrain.element())
                * playerStats.get(StatType.HEAL_EFFICIENCY)
                * playerStats.get(StatType.healMultiplier(element));
    }

    /** Multiplicateur actuel de l'attaque de l'adversaire (terrain × stat {@link StatType#ENEMY_DAMAGE}). */
    public double enemyAttackMultiplier() {
        return ElementRules.terrainAttackMultiplier(terrain.element(), currentEnemy().element())
                * playerStats.get(StatType.ENEMY_DAMAGE);
    }

    /**
     * Dégâts d'un coup de l'adversaire actuel : dégâts déclarés (plus ceux liés à ta main) × niveau ×
     * multiplicateur, moins la réduction de dégâts, jamais sous 0.
     */
    public double hitDamage(cardistry.model.enemy.MoveEffect.Hit hit) {
        return hitDamage(hit.damage() + hit.perMissingCard()
                * Math.max(0, cardistry.model.enemy.MoveEffect.Hit.FULL_HAND - hand.size()));
    }

    /** Dégâts d'un coup de {@code declared} dégâts (avant niveau, terrain et réduction). */
    public double hitDamage(double declared) {
        return Math.max(0, declared * currentEnemy().damageScale() * enemyAttackMultiplier()
                - playerStats.get(StatType.DAMAGE_REDUCTION));
    }

    /** Dégâts d'un coup de la prochaine attaque annoncée (0 si elle n'attaque pas). */
    public double enemyAttackDamage() {
        cardistry.model.enemy.EnemyMove intent = currentEnemy().peekIntent();
        return intent.effects().stream()
                .filter(e -> e instanceof cardistry.model.enemy.MoveEffect.Hit)
                .map(e -> (cardistry.model.enemy.MoveEffect.Hit) e)
                .findFirst().map(this::hitDamage).orElse(0.0);
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

    /** Le dernier combat est gagné, et le joueur est vivant. */
    public boolean isWon() {
        return won && player.isAlive();
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

    /** Partie ouverte : nouvel adversaire, qui devient l'adversaire actuel. */
    void addEncounter(Enemy enemy, boolean isFinal) {
        enemies.add(enemy);
        currentEnemyIndex = enemies.size() - 1;
        finalCombat = isFinal;
    }

    void setWon() {
        won = true;
    }

    void setChoomies(int value) {
        choomies = value;
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
        addTrinket(trinket);
    }

    /**
     * Remplace le trinket {@code slot} par {@code trinket}. Toujours possible, même si la perte du
     * trinket remplacé réduit les emplacements (Diplôme EPFL) : le moteur fait alors jeter les
     * trinkets en trop.
     */
    Trinket replaceTrinket(int slot, Trinket trinket) {
        Trinket old = unequipTrinket(slot);
        addTrinket(trinket);
        return old;
    }

    private void addTrinket(Trinket trinket) {
        trinkets.add(trinket);
        refreshInventoryModifiers();
    }

    /** Retire un trinket et ses modificateurs. */
    Trinket unequipTrinket(int slot) {
        Trinket trinket = trinkets.remove(slot);
        playerStats.removeBySource(trinket.modifierSource());
        playerStats.removeBySource(trinket.dynamicSource());
        refreshInventoryModifiers();
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

    void addEffect(TemporaryEffect effect) {
        effects.add(effect);
    }

    /** Active l'effet et applique ses modificateurs. */
    void activateEffect(TemporaryEffect effect) {
        effect.activate();
        effect.modifiers().forEach(playerStats::add);
    }

    /** Retire l'effet et ses modificateurs. */
    void removeEffect(TemporaryEffect effect) {
        effects.remove(effect);
        playerStats.removeBySource(effect.modifierSource());
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

    /** Ajoute une malédiction à venir (son intensité s'additionne si elle l'est déjà). */
    void addPendingCurse(Curse curse, int intensity) {
        pendingCurses.merge(curse, intensity, Integer::sum);
    }

    /** Fixe l'intensité d'une malédiction à venir (0 = retirée). */
    void setPendingIntensity(Curse curse, int intensity) {
        if (intensity <= 0) {
            pendingCurses.remove(curse);
        } else {
            pendingCurses.put(curse, intensity);
        }
    }

    /** Fixe l'intensité d'une malédiction active, modificateurs compris (0 = terminée). */
    void setActiveIntensity(Curse curse, int intensity) {
        playerStats.removeBySource(curse.modifierSource());
        if (intensity <= 0) {
            activeCurses.remove(curse);
        } else {
            activeCurses.put(curse, intensity);
            curse.modifiers(intensity).forEach(playerStats::add);
        }
    }

    /** Active les malédictions en attente (modificateurs compris) et les renvoie. */
    java.util.List<Curse> activatePendingCurses() {
        java.util.List<Curse> activated = java.util.List.copyOf(pendingCurses.keySet());
        for (Curse curse : activated) {
            setActiveIntensity(curse, activeIntensity(curse) + pendingCurses.get(curse));
        }
        pendingCurses.clear();
        return activated;
    }

    /** Termine les malédictions actives (modificateurs retirés) et les renvoie. */
    java.util.List<Curse> endActiveCurses() {
        java.util.List<Curse> ended = java.util.List.copyOf(activeCurses.keySet());
        for (Curse curse : ended) {
            setActiveIntensity(curse, 0);
        }
        return ended;
    }

    void setCigarettes(int value) {
        cigarettes = value;
    }
}

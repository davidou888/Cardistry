package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatEngineTest {

    private static final double DELTA = 1e-9;
    private static final int HAND_SIZE = (int) StatType.HAND_SIZE.defaultBase();

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Combat contrôlé : main, adversaires et terrain imposés. */
    private void setUp(TerrainCard terrain, List<Card> hand, Enemy... enemies) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(enemies), terrain, hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(42), bus);
    }

    private static Enemy hydra() {
        return new Enemy("Hydre", FIRE, 20, 5);
    }

    private static Enemy siren() {
        return new Enemy("Sirène", WATER, 30, 10);
    }

    private static TerrainCard neutralTerrainFor(Enemy enemy) {
        // Terrain ni du même élément, ni qui bat l'adversaire → ×1.
        for (var element : cardistry.model.Element.values()) {
            if (element != enemy.element() && !element.beats(enemy.element())) {
                return new TerrainCard("Neutre", element);
            }
        }
        throw new AssertionError();
    }

    // --- Démarrage et tours ---

    @Test
    void startPublieLaSeedEtLancePremierCombat() {
        Enemy hydra = hydra();
        setUp(new TerrainCard("EPFL", FIRE), List.of(new TerrainCard("Ancienne", FIRE)), hydra, siren());

        engine.start();

        assertEquals(new GameEvent.GameStarted(42), events.get(0));
        assertEquals(new GameEvent.CombatStarted(hydra, 1, 2), events.get(1));
        assertEquals(new GameEvent.HandDealt(state.hand()), events.get(2));
        assertEquals(3, events.size());
        assertEquals(GameState.STARTING_HAND, state.hand().size());
        assertFalse(state.hand().stream().anyMatch(c -> c.name().equals("Ancienne")));
        assertEquals(1, state.combatNumber());
    }

    @Test
    void startTurnIncrementeLeTour() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(), hydra());
        engine.startTurn();
        engine.startTurn();
        assertEquals(2, state.turn());
        assertEquals(2, state.combatTurn());
        assertEquals(List.of(new GameEvent.TurnStarted(1), new GameEvent.TurnStarted(2)), events);
    }

    @Test
    void nouvellePartieMainDeCinqEtDeterministe() {
        GameState a = startedGame(5);
        GameState b = startedGame(5);
        assertEquals(GameState.STARTING_HAND, a.hand().size());
        assertEquals(100, a.player().hp(), DELTA);
        assertEquals(100, a.maxHp(), DELTA);
        assertEquals(a.hand().stream().map(Card::name).toList(), b.hand().stream().map(Card::name).toList());
        assertEquals("Hydre", a.currentEnemy().name());
        assertEquals(1.25, a.enemyAttackMultiplier(), DELTA); // EPFL (feu) contre Hydre (feu)
    }

    private static GameState startedGame(long seed) {
        GameState game = GameState.newGame();
        assertTrue(game.hand().isEmpty());
        new CombatEngine(game, new CardPool(), new Rng(seed), new EventBus()).start();
        return game;
    }

    // --- Cartes ---

    @Test
    void sortilegeAvantageInflige150Pourcent() {
        Enemy hydra = hydra();
        SpellCard jet = new SpellCard("Jet d'eau", WATER, 10);
        setUp(neutralTerrainFor(hydra), List.of(jet), hydra);

        assertTrue(engine.submit(new Action.PlayCard(0)));

        assertEquals(5, hydra.hp(), DELTA);
        assertEquals(new GameEvent.CardPlayed(jet), events.get(0));
        assertEquals(new GameEvent.DamageDealt(hydra, 15, 1.5), events.get(1));
    }

    @Test
    void sortilegeDesavantageInflige50Pourcent() {
        Enemy siren = siren();
        setUp(neutralTerrainFor(siren), List.of(new SpellCard("Boule de feu", FIRE, 10)), siren);
        engine.submit(new Action.PlayCard(0));
        assertEquals(25, siren.hp(), DELTA);
    }

    @Test
    void sortilegeMemeElementInflige100Pourcent() {
        Enemy hydra = hydra();
        setUp(neutralTerrainFor(hydra), List.of(new SpellCard("Boule de feu", FIRE, 10)), hydra);
        engine.submit(new Action.PlayCard(0));
        assertEquals(10, hydra.hp(), DELTA);
    }

    @Test
    void soinBoosteSurTerrainDuMemeElement() {
        Enemy hydra = hydra();
        HealCard heal = new HealCard("Petit pétou", FIRE, 10);
        setUp(new TerrainCard("EPFL", FIRE), List.of(heal), hydra);

        state.player().takeDamage(50);

        engine.submit(new Action.PlayCard(0));

        assertTrue(events.contains(new GameEvent.Healed(state.player(), 15, 1.5)));
        // 50 + 15 de soin - 5 × 1.25 d'attaque (Hydre sur terrain feu)
        assertEquals(50 + 15 - 6.25, state.player().hp(), DELTA);
    }

    @Test
    void soinPlafonneAuxPvMax() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(new HealCard("Petit pétou", FIRE, 10)), hydra());
        state.player().takeDamage(4);

        engine.submit(new Action.PlayCard(0));

        assertEquals(new GameEvent.Healed(state.player(), 4, 1.5), events.get(1));
        assertEquals(100 - 6.25, state.player().hp(), DELTA);
    }

    @Test
    void pvMaxAmeliorablesParStat() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(new HealCard("Petit pétou", FIRE, 10)), hydra());
        state.playerStats().add(StatModifier.flat(StatType.MAX_HP, 20, "amulette"));

        engine.submit(new Action.PlayCard(0));

        assertEquals(120, state.maxHp(), DELTA);
        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
    }

    @Test
    void terrainChangeLAttaqueAdverseDesCeTour() {
        Enemy hydra = hydra();
        TerrainCard epfl = new TerrainCard("EPFL", FIRE);
        TerrainCard swamp = new TerrainCard("Marécage", WATER);
        setUp(epfl, List.of(swamp), hydra);

        engine.submit(new Action.PlayCard(0));

        assertSame(swamp, state.terrain());
        assertEquals(new GameEvent.TerrainChanged(epfl, swamp), events.get(1));
        assertEquals(new GameEvent.DamageDealt(state.player(), 3.75, 0.75), events.get(3));
        assertEquals(100 - 3.75, state.player().hp(), DELTA);
    }

    @Test
    void jouerUneCarteLaRetireEtDecaleLaMain() {
        Card a = new TerrainCard("A", FIRE);
        Card b = new TerrainCard("B", WATER);
        Card c = new TerrainCard("C", NATURE);
        setUp(new TerrainCard("EPFL", FIRE), List.of(a, b, c), hydra());

        engine.submit(new Action.PlayCard(0));

        assertEquals(List.of(b, c), state.hand());
    }

    // --- Pioche ---

    @Test
    void piocherAjouteUneCarteEtLAdversaireAttaque() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(), hydra());

        assertTrue(engine.submit(new Action.DrawCard()));

        assertEquals(1, state.hand().size());
        assertEquals(new GameEvent.CardDrawn(state.hand().get(0)), events.get(0));
        assertInstanceOf(GameEvent.EnemyAttacked.class, events.get(1));
        assertEquals(100 - 6.25, state.player().hp(), DELTA);
    }

    // --- Actions refusées : rien ne change, pas d'attaque ---

    @Test
    void piocherMainPleineEstRefuse() {
        List<Card> fullHand = new ArrayList<>();
        for (int i = 0; i < HAND_SIZE; i++) {
            fullHand.add(new TerrainCard("T" + i, FIRE));
        }
        setUp(new TerrainCard("EPFL", FIRE), fullHand, hydra());

        assertFalse(engine.submit(new Action.DrawCard()));

        assertEquals(List.of(new GameEvent.ActionRejected(Reason.HAND_FULL)), events);
        assertEquals(HAND_SIZE, state.hand().size());
        assertEquals(100, state.player().hp(), DELTA);
    }

    @Test
    void slotHorsDeLaMainEstRefuse() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(new TerrainCard("A", FIRE)), hydra());

        assertFalse(engine.submit(new Action.PlayCard(-1)));
        assertFalse(engine.submit(new Action.PlayCard(HAND_SIZE)));

        assertEquals(List.of(
                new GameEvent.ActionRejected(Reason.INVALID_CHOICE),
                new GameEvent.ActionRejected(Reason.INVALID_CHOICE)), events);
    }

    @Test
    void slotVideEstRefuse() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(new TerrainCard("A", FIRE)), hydra());

        assertFalse(engine.submit(new Action.PlayCard(1)));

        assertEquals(List.of(new GameEvent.ActionRejected(Reason.EMPTY_SLOT)), events);
        assertEquals(1, state.hand().size());
    }

    // --- Mort des adversaires, victoire, défaite ---

    @Test
    void adversaireTueCombatSuivantNouvelleMainSansAttaque() {
        Enemy hydra = hydra();
        Enemy siren = siren();
        SpellCard tsunami = new SpellCard("Tsunami", WATER, 15);
        setUp(new TerrainCard("EPFL", FIRE), List.of(tsunami), hydra, siren);
        engine.startTurn();
        events.clear();

        engine.submit(new Action.PlayCard(0));

        assertEquals(List.of(
                new GameEvent.CardPlayed(tsunami),
                new GameEvent.DamageDealt(hydra, 22.5, 1.5),
                new GameEvent.EnemyKilled(hydra),
                new GameEvent.CombatStarted(siren, 2, 2),
                new GameEvent.HandDealt(state.hand())), events);
        assertSame(siren, state.currentEnemy());
        assertEquals(2, state.combatNumber());
        assertEquals(0, state.combatTurn());
        assertEquals(GameState.STARTING_HAND, state.hand().size());
        assertEquals(100, state.player().hp(), DELTA);
        assertFalse(engine.isOver());
    }

    @Test
    void dernierAdversaireTueVictoire() {
        Enemy hydra = hydra();
        setUp(new TerrainCard("EPFL", FIRE), List.of(new SpellCard("Tsunami", WATER, 15)), hydra);

        engine.submit(new Action.PlayCard(0));

        assertEquals(new GameEvent.GameWon(), events.get(events.size() - 1));
        assertTrue(state.isWon());
        assertTrue(engine.isOver());
        assertThrows(IllegalStateException.class, () -> engine.submit(new Action.DrawCard()));
    }

    @Test
    void joueurAZeroPvDefaite() {
        Enemy brute = new Enemy("Brute", FIRE, 1000, 100);
        setUp(new TerrainCard("EPFL", FIRE), List.of(), brute);

        engine.submit(new Action.DrawCard());

        assertFalse(state.player().isAlive());
        assertEquals(new GameEvent.GameLost(), events.get(events.size() - 1));
        assertTrue(state.isLost());
        assertTrue(engine.isOver());
    }

    @Test
    void combatCompletContreLesTroisAdversaires() {
        // Pool à une seule carte : Boule de feu 40 → Hydre ×1 (40), Sirène ×0.5 (20), Goblin ×1.5 (60).
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = GameState.newGame();
        engine = new CombatEngine(state, new CardPool(List.of(new SpellCard("Boule", FIRE, 40))), new Rng(1), bus);

        engine.start();
        int turns = 0;
        while (!engine.isOver()) {
            engine.startTurn();
            assertTrue(engine.submit(new Action.PlayCard(0)));
            turns++;
        }

        assertTrue(state.isWon());
        assertEquals(1 + 2 + 1, turns); // Hydre 20 → 1 sort, Sirène 30 → 2, Goblin 40 → 1
        assertEquals(3, events.stream().filter(GameEvent.CombatStarted.class::isInstance).count());
        // Seule attaque : Sirène au tour où elle survit (×1 sur EPFL contre eau).
        assertEquals(100 - 10, state.player().hp(), DELTA);
    }

    // --- Stats du joueur ---

    @Test
    void multiplicateurDeDegatsParElement() {
        Enemy hydra = hydra();
        setUp(neutralTerrainFor(hydra), List.of(new SpellCard("Jet d'eau", WATER, 10)), hydra);
        state.playerStats().add(StatModifier.percent(StatType.WATER_DAMAGE, 0.25, "test"));
        state.playerStats().add(StatModifier.percent(StatType.FIRE_DAMAGE, 2, "autre élément"));

        engine.submit(new Action.PlayCard(0));

        // 10 × 1.5 (eau bat feu) × 1.25 (stat eau)
        assertEquals(new GameEvent.DamageDealt(hydra, 18.75, 1.875), events.get(1));
    }

    @Test
    void efficaciteDesSoins() {
        setUp(new TerrainCard("EPFL", FIRE), List.of(new HealCard("Potion", NATURE, 10)), hydra());
        state.player().takeDamage(50);
        state.playerStats().add(StatModifier.percent(StatType.HEAL_EFFICIENCY, 1, "test"));

        engine.submit(new Action.PlayCard(0));

        // 10 × 0.75 (feu bat nature) × 2
        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
    }

    @Test
    void tailleDeMainSuitLaStat() {
        List<Card> fullHand = new ArrayList<>();
        for (int i = 0; i < HAND_SIZE; i++) {
            fullHand.add(new TerrainCard("T" + i, FIRE));
        }
        setUp(new TerrainCard("EPFL", FIRE), fullHand, hydra());
        state.playerStats().add(StatModifier.flat(StatType.HAND_SIZE, 1, "sac"));

        assertEquals(HAND_SIZE + 1, state.handSize());
        assertTrue(engine.submit(new Action.DrawCard()));
        assertEquals(HAND_SIZE + 1, state.hand().size());
    }
}

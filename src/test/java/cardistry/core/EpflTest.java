package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** EPFL : tout doublé pendant 3 tours, PV ×2 à l'arrivée et ÷2 au départ, puis retour au Dojo. */
class EpflTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    private void setUp(double enemyAttack, Card... hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", Element.NEUTRAL, 1000, enemyAttack);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", Element.NEUTRAL, 50, 0)),
                new TerrainCard("Forêt", Element.NATURE, Rarity.COMMON), List.of(hand), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(2), bus, new LootTable(List::of));
        engine.startTurn();
    }

    private void pass() {
        engine.submit(new Action.DrawCard());
    }

    @Test
    void arriveeToutEstDoublePasLaTailleDeMain() {
        setUp(0, CardPool.epfl());
        state.player().takeDamage(50);
        int hand = state.handSize();
        engine.submit(new Action.PlayCard(0));
        assertEquals(100, state.player().hp(), "PV actuels ×2");
        assertEquals(200, state.maxHp(), 1e-9);
        assertEquals(2.0, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9);
        assertEquals(2.0, state.playerStats().get(StatType.FIRE_DAMAGE), 1e-9);
        assertEquals(2.0, state.playerStats().get(StatType.HEAL_EFFICIENCY), 1e-9);
        assertEquals(hand, state.handSize());
        assertEquals(CardPool.EPFL_TURNS, state.epflTurnsLeft());
    }

    @Test
    void apresTroisToursRetourAuDojoEtPvDivises() {
        setUp(0, CardPool.epfl());
        engine.submit(new Action.PlayCard(0)); // 100 → 200
        List<Integer> shown = new ArrayList<>();
        for (int turn = 1; turn <= 3; turn++) {
            engine.startTurn();
            assertEquals("EPFL", state.terrain().name(), "tour " + turn);
            shown.add(state.epflTurnsLeft());
            pass();
        }
        assertEquals(List.of(3, 2, 1), shown, "tours restants, tour en cours compris");
        engine.startTurn();
        assertEquals("Dojo", state.terrain().name());
        assertEquals(100, state.player().hp());
        assertEquals(100, state.maxHp(), 1e-9);
        assertEquals(1.1, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9, "bonus du Dojo");
    }

    @Test
    void changerDeTerrainAvantTroisToursAnnuleLeMinuteur() {
        setUp(0, CardPool.epfl(), new TerrainCard("Marécage", Element.WATER, Rarity.COMMON));
        engine.submit(new Action.PlayCard(0));
        engine.startTurn();
        engine.submit(new Action.PlayCard(0)); // EPFL → Marécage
        assertEquals(100, state.player().hp());
        assertEquals(0, state.epflTurnsLeft());
        assertEquals(1.0, state.playerStats().get(StatType.WATER_DAMAGE), 1e-9);
        for (int i = 0; i < 4; i++) {
            engine.startTurn();
            pass();
        }
        assertEquals("Marécage", state.terrain().name(), "pas de retour au Dojo");
    }

    @Test
    void combatTermineSurLEpflPvDivisesAvantLaFinDuCombat() {
        setUp(0, CardPool.epfl(), new SpellCard("Fin", Element.NEUTRAL, 1000, Rarity.COMMON));
        engine.submit(new Action.PlayCard(0)); // 200/200
        engine.startTurn();
        engine.submit(new Action.PlayCard(0)); // tue l'adversaire
        assertEquals(100, state.player().hp());
        assertEquals(100, state.maxHp(), 1e-9);
        int halved = indexOf(e -> e instanceof GameEvent.AbilityTriggered a && a.message().startsWith("Tu quittes l'EPFL"));
        int killed = indexOf(e -> e instanceof GameEvent.EnemyKilled);
        int next = indexOf(e -> e instanceof GameEvent.CombatStarted);
        assertTrue(killed < halved && halved < next, events.toString());
    }

    private int indexOf(java.util.function.Predicate<GameEvent> match) {
        for (int i = 0; i < events.size(); i++) {
            if (match.test(events.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void degatsSubisPendantLEpflProportionConservee() {
        setUp(0, CardPool.epfl());
        state.player().takeDamage(50); // 50/100
        engine.submit(new Action.PlayCard(0)); // 100/200
        state.player().takeDamage(40); // 60/200
        for (int i = 0; i < 3; i++) {
            engine.startTurn();
            pass();
        }
        engine.startTurn(); // retour au Dojo : 60/200 → 30/100
        assertEquals(30, state.player().hp());
        assertEquals(100, state.maxHp(), 1e-9);
    }

    @Test
    void laReductionDeDegatsEstDoublee() {
        setUp(10, CardPool.epfl());
        state.playerStats().add(cardistry.core.stats.StatModifier.flat(StatType.DEFENSE, 3, "test"));
        engine.submit(new Action.PlayCard(0)); // 200 PV ; coup de 10 − 2 × 3
        assertEquals(196, state.player().hp());
    }
}

package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DrunkennessTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Deux combats ; la main contient un sort qui tue le premier adversaire d'un coup. */
    private void setUp(int enemies) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        List<Enemy> list = new ArrayList<>();
        for (int i = 0; i < enemies; i++) {
            list.add(new Enemy("E" + i, FIRE, 20, 5));
        }
        state = new GameState(new Player(100), list, new TerrainCard("EPFL", FIRE),
                List.of(new SpellCard("Tsunami", WATER, 100)), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private static Item drink() {
        return new Item("biere", "Bière", "x", "", Rarity.COMMON, Set.of(Tag.DRINK), ctx -> { });
    }

    private void drinkTimes(int n) {
        for (int i = 0; i < n; i++) {
            engine.give(drink());
            assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.UseItem(0)));
        }
    }

    private long count(Class<? extends GameEvent> type) {
        return events.stream().filter(type::isInstance).count();
    }

    // --- Paliers ---

    private double fireDamage() {
        return state.playerStats().get(StatType.FIRE_DAMAGE);
    }

    @Test
    void paliersPompetteBourreCuite() {
        setUp(2);
        assertEquals(DrunkTier.SOBER, state.drunkTier());
        assertEquals(1.0, fireDamage(), 1e-9);
        drinkTimes(1);
        assertEquals(DrunkTier.TIPSY, state.drunkTier());
        assertEquals(1.10, fireDamage(), 1e-9);
        drinkTimes(1);
        assertEquals(DrunkTier.DRUNK, state.drunkTier());
        assertEquals(1.25, fireDamage(), 1e-9);
        drinkTimes(1);
        assertEquals(DrunkTier.WASTED, state.drunkTier());
        assertEquals(1.50, fireDamage(), 1e-9);
        drinkTimes(2);
        assertEquals(1.50, fireDamage(), 1e-9, "la Cuite ne monte pas plus haut");
        assertEquals(1.50, state.playerStats().get(StatType.WATER_DAMAGE), 1e-9);
        assertEquals(1.50, state.playerStats().get(StatType.NATURE_DAMAGE), 1e-9);
    }

    @Test
    void leBonusDIvresseTombeALaFinDuCombat() {
        setUp(2);
        drinkTimes(2);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0)); // tue : fin du combat
        assertEquals(DrunkTier.SOBER, state.drunkTier());
        assertEquals(1.0, fireDamage(), 1e-9);
    }

    @Test
    void avecUnSeuilPlusHautLaCuiteArrivePlusTard() {
        setUp(2);
        state.playerStats().add(StatModifier.flat(StatType.HANGOVER_THRESHOLD, 2, "test"));
        drinkTimes(4);
        assertEquals(DrunkTier.DRUNK, state.drunkTier(), "Bourré de 2 à 4 avec un seuil à 5");
        assertTrue(state.pendingCurses().isEmpty());
        drinkTimes(1);
        assertEquals(DrunkTier.WASTED, state.drunkTier());
        assertFalse(state.pendingCurses().isEmpty());
    }

    @Test
    void uneBoissonDonneUnPointDIvresse() {
        setUp(2);
        drinkTimes(1);
        assertEquals(1, state.drunkenness());
        assertTrue(events.contains(new GameEvent.DrunkennessChanged(1, 3)));
    }

    @Test
    void unObjetQuiNEstPasUneBoissonNeSaoulePas() {
        setUp(2);
        engine.give(new Item("mate", "Maté", "x", "", Rarity.COMMON, Set.of(Tag.HEAL), ctx -> { }));
        engine.submit(new Action.UseItem(0));
        assertEquals(0, state.drunkenness());
    }

    @Test
    void auSeuilGueuleDeBoisAuProchainCombat() {
        setUp(2);
        drinkTimes(2);
        assertTrue(state.pendingCurses().isEmpty());

        drinkTimes(1);
        assertEquals(Set.of(Curse.HANGOVER), state.pendingCurses());
        assertTrue(events.contains(new GameEvent.CurseGained(Curse.HANGOVER)));
        assertEquals(7, state.handSize(), "rien ne change pendant le combat en cours");
    }

    @Test
    void laGueuleDeBoisDureLeCombatSuivantPuisDisparait() {
        setUp(3);
        drinkTimes(3);

        engine.submit(new Action.PlayCard(0)); // tue E0
        assertEquals(0, state.drunkenness(), "l'Ivresse retombe à la fin du combat");
        assertEquals(Set.of(Curse.HANGOVER), state.activeCurses());
        assertEquals(6, state.handSize());
        assertEquals(GameState.STARTING_HAND, state.hand().size());
        int started = events.indexOf(new GameEvent.CurseStarted(Curse.HANGOVER));
        assertTrue(started > events.indexOf(new GameEvent.CombatStarted(state.currentEnemy(), 2, 3)));
        assertTrue(started < events.size() - 1, "avant la distribution");

        state.currentEnemy().takeDamage(100);
        engine.submit(new Action.DrawCard()); // tue E1
        assertTrue(state.activeCurses().isEmpty());
        assertTrue(events.contains(new GameEvent.CurseEnded(Curse.HANGOVER)));
        assertEquals(7, state.handSize());
    }

    @Test
    void uneSeuleGueuleDeBoisParCombatMaisLIvresseContinue() {
        setUp(2);
        drinkTimes(6);
        assertEquals(6, state.drunkenness());
        assertEquals(1, count(GameEvent.CurseGained.class));
    }

    @Test
    void seuilModifiable() {
        setUp(2);
        state.playerStats().add(StatModifier.flat(StatType.HANGOVER_THRESHOLD, 2, "foie"));
        drinkTimes(4);
        assertTrue(state.pendingCurses().isEmpty());
        drinkTimes(1);
        assertEquals(Set.of(Curse.HANGOVER), state.pendingCurses());
    }

    @Test
    void pasDeGueuleDeBoisPendantLeDernierCombat() {
        setUp(1);
        drinkTimes(3);
        assertTrue(state.pendingCurses().isEmpty());
        assertEquals(0, count(GameEvent.CurseGained.class));
    }

    @Test
    void lesEffetsPeuventAjouterDeLIvresse() {
        setUp(2);
        engine.give(new Item("x", "x", "x", "", Rarity.COMMON, Set.of(Tag.META), ctx -> ctx.addDrunkenness(2)));
        engine.submit(new Action.UseItem(0));
        assertEquals(2, state.drunkenness());
        assertFalse(state.pendingCurses().contains(Curse.HANGOVER));
    }
}

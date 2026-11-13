package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.Curse;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Googz, Honoré, Ross et leurs combinaisons. */
class CigaretteTrinketTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Adversaires feu 100 PV / 10 d'attaque sur le terrain donné ; seed donnée. */
    private void setUp(TerrainCard terrain, List<Card> hand, long seed) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 100, 10), new Enemy("B", FIRE, 100, 10),
                new Enemy("C", FIRE, 100, 10)), terrain, hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
    }

    private void setUp() {
        setUp(new TerrainCard("Forêt", NATURE), List.of(), 1);
    }

    private void equip(String id) {
        engine.equip(ItemRegistry.trinket(id));
    }

    /** Première seed dont le premier tirage est (ou n'est pas) sous 20 %. */
    private static long seedWhereFirstRoll(boolean below) {
        for (long seed = 0; ; seed++) {
            if ((new Rng(seed).nextDouble() < 0.2) == below) {
                return seed;
            }
        }
    }

    private static final TerrainCard EPFL = new TerrainCard("EPFL", FIRE);

    // --- Googz ---

    @Test
    void googzTransformeEnPetitPetouALaPlaceDesCinqPv() {
        setUp(EPFL, List.of(), seedWhereFirstRoll(true));
        state.player().takeDamage(50);
        equip("googz");
        engine.submit(new Action.Smoke());
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(true)));
        assertEquals(50 + 22.5, state.player().hp(), DELTA); // soin feu sur EPFL : 15 × 1.5
    }

    @Test
    void googzSinonCinqPvPerdus() {
        setUp(EPFL, List.of(), seedWhereFirstRoll(false));
        equip("googz");
        engine.submit(new Action.Smoke());
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(false)));
        assertEquals(95, state.player().hp(), DELTA);
    }

    @Test
    void googzVingtPourcentEnMoyenne() {
        setUp();
        equip("googz");
        for (int i = 0; i < 2000; i++) {
            engine.submit(new Action.Smoke());
            engine.submit(new Action.RollCigarette()); // regarnit le paquet
            state.player().heal(1000);                 // PV hors sujet
        }
        long transformed = events.stream().filter(e -> e.equals(new GameEvent.CigaretteSmoked(true))).count();
        assertEquals(0.2, transformed / 2000.0, 0.03);
    }

    @Test
    void googzReduitQuandMemeLaGueuleDeBois() {
        setUp(EPFL, List.of(), seedWhereFirstRoll(true));
        equip("googz");
        for (int i = 0; i < 3; i++) {
            engine.give(ItemRegistry.item("spritz"));
            engine.submit(new Action.UseItem(0));
        }
        assertEquals(1, state.pendingIntensity(Curse.HANGOVER));
        // Transformée ou non, la clope réduit la Gueule de bois.
        engine.submit(new Action.Smoke());
        assertEquals(0, state.pendingIntensity(Curse.HANGOVER));
    }

    @Test
    void googzEtGrinder() {
        setUp(EPFL, List.of(), seedWhereFirstRoll(true));
        state.player().takeDamage(60);
        equip("googz");
        equip("grinder");
        engine.submit(new Action.Smoke());
        // 15 × 1.5 (terrain feu) × 1.5 (Grinder) = 33.75
        assertEquals(40 + 33.75, state.player().hp(), DELTA);
    }

    @Test
    void googzEtRoss() {
        setUp(EPFL, List.of(), seedWhereFirstRoll(true));
        equip("googz");
        equip("ross");
        engine.submit(new Action.Smoke());
        // 15 × 1.5 (terrain feu) × 2 (Ross), sans plafond
        assertEquals(100 + 45, state.player().hp(), DELTA);
    }

    // --- Honoré ---

    @Test
    void honoreUneClopeTousLesCinqToursJoues() {
        setUp();
        equip("honore");
        Gauge gauge = state.trinkets().get(0).gauge().orElseThrow();
        for (int i = 0; i < 4; i++) {
            engine.startTurn();
        }
        assertEquals(4, gauge.value());
        assertEquals(5, state.cigarettes());

        engine.startTurn();
        assertEquals(6, state.cigarettes());
        assertEquals(0, gauge.value());
    }

    @Test
    void honoreNeComptePasLesToursSautes() {
        setUp();
        equip("honore");
        Gauge gauge = state.trinkets().get(0).gauge().orElseThrow();
        engine.give(ItemRegistry.item("raclette"));
        engine.submit(new Action.UseItem(0));
        assertFalse(engine.startTurn());   // tour sauté
        assertEquals(0, gauge.value());
        assertTrue(engine.startTurn());
        assertEquals(1, gauge.value());
    }

    @Test
    void honorePersisteEntreLesCombats() {
        setUp();
        equip("honore");
        Gauge gauge = state.trinkets().get(0).gauge().orElseThrow();
        engine.startTurn();
        engine.startTurn();
        state.currentEnemy().takeDamage(1000);
        engine.submit(new Action.DrawCard());
        assertEquals(2, state.combatNumber());
        assertEquals(2, gauge.value());
    }

    @Test
    void honorePaquetPleinLaClopeAttendUnePlace() {
        setUp();
        equip("honore");
        Gauge gauge = state.trinkets().get(0).gauge().orElseThrow();
        engine.give(new Item("paquet", "paquet", "x", "", Rarity.COMMON, java.util.Set.of(Tag.META),
                ctx -> ctx.addCigarettes(100)));
        engine.submit(new Action.UseItem(0));
        for (int i = 0; i < 6; i++) {
            engine.startTurn();
        }
        assertEquals(20, state.cigarettes());
        assertTrue(gauge.isFull(), "la jauge reste à 5 : clope prête");

        engine.submit(new Action.Smoke());   // une place se libère : la clope d'Honoré arrive
        assertEquals(20, state.cigarettes());
        assertEquals(0, gauge.value());
    }

    // --- Ross ---

    @Test
    void rossDoubleLesSoinsEtDepasseLeMax() {
        setUp(EPFL, List.of(new HealCard("Petit pétou", FIRE, 10)), 1);
        equip("ross");
        engine.submit(new Action.PlayCard(0));
        // 10 × 1.5 (terrain feu) × 2 = 30, sans plafond ; puis l'adversaire attaque (10 × 1.25)
        assertEquals(new GameEvent.Healed(state.player(), 30, 3.0), events.get(1));
        assertEquals(100 + 30 - 12.5, state.player().hp(), DELTA);
        assertEquals(100, state.maxHp(), DELTA);
    }

    @Test
    void rossDoubleAussiLesSoinsDeTrinketsEtDObjets() {
        setUp();
        equip("ross");
        equip("monde-snack");
        engine.give(ItemRegistry.item("mate"));
        engine.submit(new Action.UseItem(0));
        assertEquals(120, state.player().hp(), DELTA);   // Maté 10 × 2
        state.currentEnemy().takeDamage(1000);
        engine.submit(new Action.DrawCard());
        assertEquals(140, state.player().hp(), DELTA);   // Monde Snack 10 × 2
    }

    @Test
    void rossEtGrinderOrdreDeCalcul() {
        setUp(EPFL, List.of(new HealCard("Petit pétou", FIRE, 10)), 1);
        state.player().takeDamage(90);
        equip("ross");
        equip("grinder");
        state.playerStats().add(StatModifier.percent(StatType.HEAL_EFFICIENCY, 0.5, "autre bonus"));
        engine.submit(new Action.PlayCard(0));
        // 10 × 1.5 (terrain) × [(1 + 0.5) × 2] (efficacité, Ross en final) × 1.5 (Grinder) = 67.5
        // (un ×2 additif aurait donné 10 × 1.5 × 2.5 × 1.5 = 56.25)
        assertEquals(new GameEvent.Healed(state.player(), 67.5, 6.75), events.get(1));
    }

    @Test
    void rossEtPetouAncestral() {
        setUp();
        equip("ross");
        equip("petou-ancestral");
        for (int i = 0; i < 10; i++) {
            engine.startTurn();
        }
        // 10 tours × 20 PV, moins 5 attaques des tours sautés (10) et aucune action jouée
        assertEquals(100 + 10 * 20 - 5 * 10, state.player().hp(), DELTA);
    }
}

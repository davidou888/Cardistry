package cardistry.core;

import static cardistry.core.stats.StatModifier.flat;
import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.ItemRegistry;
import cardistry.items.LootTable;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CigaretteTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Trois adversaires feu de 20 PV / 10 d'attaque ; terrain Forêt (attaque ×1). */
    private void setUp() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 20, 10), new Enemy("B", FIRE, 20, 10),
                new Enemy("C", FIRE, 20, 10)), new TerrainCard("Forêt", NATURE), List.of(), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private void smoke() {
        engine.submit(new Action.Smoke());
    }

    private void drinkTimes(int n) {
        for (int i = 0; i < n; i++) {
            engine.give(ItemRegistry.item("spritz"));
            engine.submit(new Action.UseItem(0));
        }
    }

    private void killCurrentEnemy() {
        state.currentEnemy().takeDamage(1000);
        engine.submit(new Action.DrawCard());
    }

    // --- Paquet ---

    @Test
    void cinqClopesAuDepartVingtMax() {
        setUp();
        assertEquals(5, state.cigarettes());
        assertEquals(20, state.cigaretteMax());
    }

    @Test
    void fumerEstGratuitEtCouteCinqPv() {
        setUp();
        assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.Smoke()));
        assertEquals(4, state.cigarettes());
        assertEquals(95, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(false)));
        assertTrue(events.contains(new GameEvent.CigarettesChanged(4, 20)));
        assertFalse(events.stream().anyMatch(GameEvent.EnemyAttacked.class::isInstance));
    }

    @Test
    void plusDeClopeRefuse() {
        setUp();
        for (int i = 0; i < 5; i++) {
            smoke();
        }
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.Smoke()));
        assertEquals(new GameEvent.ActionRejected(Reason.NO_CIGARETTE), events.get(events.size() - 1));
    }

    @Test
    void rouleRemplaceLActionDuTour() {
        setUp();
        assertEquals(ActionResult.TURN_ENDED, engine.submit(new Action.RollCigarette()));
        assertEquals(6, state.cigarettes());
        assertEquals(90, state.player().hp(), DELTA); // l'adversaire attaque
        assertTrue(events.contains(new GameEvent.CigaretteRolled()));
    }

    @Test
    void roulerPaquetPleinRefuse() {
        setUp();
        state.setCigarettes(20);
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.RollCigarette()));
        assertEquals(new GameEvent.ActionRejected(Reason.PACK_FULL), events.get(events.size() - 1));
        assertEquals(100, state.player().hp(), DELTA);
    }

    @Test
    void lesClopesSeGardentEntreLesCombats() {
        setUp();
        smoke();
        killCurrentEnemy();
        assertEquals(2, state.combatNumber());
        assertEquals(4, state.cigarettes());
    }

    @Test
    void lesEffetsAjoutentDesClopesSansDepasserLePaquet() {
        setUp();
        state.setCigarettes(19);
        engine.give(new cardistry.items.Item("x", "x", "x", "", cardistry.items.Rarity.COMMON,
                Set.of(cardistry.items.Tag.META), ctx -> assertEquals(1, ctx.addCigarettes(3))));
        engine.submit(new Action.UseItem(0));
        assertEquals(20, state.cigarettes());
    }

    // --- Dégâts ---

    @Test
    void fumerIgnoreLePolycopie() {
        setUp();
        engine.equip(ItemRegistry.trinket("polycopie-d-analyse-i"));
        smoke();
        assertEquals(95, state.player().hp(), DELTA);
    }

    @Test
    void uneClopeQuiTue() {
        setUp();
        state.player().takeDamage(97);
        assertEquals(ActionResult.TURN_ENDED, engine.submit(new Action.Smoke()));
        assertTrue(state.isLost());
        assertTrue(events.contains(new GameEvent.GameLost()));
    }

    @Test
    void fumerSansGueuleDeBoisNeFaitQuePerdreDesPv() {
        setUp();
        smoke();
        assertFalse(events.stream().anyMatch(GameEvent.CurseReduced.class::isInstance));
    }

    // --- Gueule de bois ---

    @Test
    void clopeSurGueuleDeBoisAVenir() {
        setUp();
        drinkTimes(3);
        assertEquals(1, state.pendingIntensity(Curse.HANGOVER));

        smoke();
        assertTrue(state.pendingCurses().isEmpty());
        assertTrue(events.contains(new GameEvent.CurseReduced(Curse.HANGOVER, 0, true)));

        killCurrentEnemy();
        assertTrue(state.activeCurses().isEmpty());
        assertEquals(7, state.handSize());
    }

    @Test
    void clopeSurGueuleDeBoisActive() {
        setUp();
        drinkTimes(3);
        killCurrentEnemy();
        assertEquals(6, state.handSize());

        smoke();
        assertTrue(state.activeCurses().isEmpty());
        assertEquals(7, state.handSize());
        assertTrue(events.contains(new GameEvent.CurseEnded(Curse.HANGOVER)));
    }

    @Test
    void laClopeReduitDAbordLaGueuleDeBoisActive() {
        setUp();
        drinkTimes(3);
        killCurrentEnemy();       // combat 2 : Gueule de bois active
        drinkTimes(3);            // + une à venir pour le combat 3
        assertEquals(1, state.activeIntensity(Curse.HANGOVER));
        assertEquals(1, state.pendingIntensity(Curse.HANGOVER));

        smoke();
        assertEquals(0, state.activeIntensity(Curse.HANGOVER));
        assertEquals(1, state.pendingIntensity(Curse.HANGOVER));
    }

    @Test
    void intensiteDeuxDeuxCartesDeMoins() {
        setUp();
        state.addPendingCurse(Curse.HANGOVER, 2);
        killCurrentEnemy();
        assertEquals(5, state.handSize());
        assertEquals("−2 taille de main", Curse.HANGOVER.description(2));

        smoke();
        assertEquals(1, state.activeIntensity(Curse.HANGOVER));
        assertEquals(6, state.handSize());
        assertTrue(events.contains(new GameEvent.CurseReduced(Curse.HANGOVER, 1, false)));
    }

    // --- Transformation (stat générique, utilisée par Googz) ---

    @Test
    void transformationEnPetitPetou() {
        setUp();
        state.player().takeDamage(80);
        state.playerStats().add(flat(StatType.CIGARETTE_TRANSFORM_CHANCE, 1, "test"));
        drinkTimes(1);            // Spritz : +30 → 50 PV, Ivresse 1
        smoke();
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(true)));
        // Petit pétou : soin feu 15 sur Forêt (le feu bat la nature : ×1) ; −1 Ivresse ; pas de −5 PV.
        assertEquals(50 + 15, state.player().hp(), DELTA);
        assertEquals(0, state.drunkenness());
        assertEquals(1, events.stream().filter(GameEvent.ItemUsed.class::isInstance).count(),
                "la clope transformée n'est pas un objet utilisé (seul le Spritz l'est)");
    }
}

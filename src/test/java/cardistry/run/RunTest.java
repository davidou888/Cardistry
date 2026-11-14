package cardistry.run;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.core.GameState;
import cardistry.core.PlayerDecisions;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.ItemRegistry;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Une partie : codes, secrets débloqués dans le pool, Ctrl+Z. */
class RunTest {

    private final List<GameEvent> events = new ArrayList<>();
    private final EventBus bus = new EventBus();

    RunTest() {
        bus.subscribe(events::add);
    }

    /** Deux adversaires solides, pool varié : la partie dure. */
    private static GameState sturdy() {
        return new GameState(new Player(100), List.of(new Enemy("A", FIRE, 200, 7), new Enemy("B", WATER, 200, 7)),
                new TerrainCard("EPFL", FIRE), List.of(), new Stats());
    }

    private static CardPool pool() {
        return new CardPool(List.of(new SpellCard("Boule de feu", FIRE, 10), new SpellCard("Tsunami", WATER, 15),
                new HealCard("Potion", FIRE, 10), new TerrainCard("Forêt", cardistry.model.Element.NATURE)));
    }

    private Run run(long seed, Secret... codes) {
        Run run = new Run(seed, Set.of(), bus, RunTest::sturdy, RunTest::pool);
        for (Secret code : codes) {
            run.addCode(code);
        }
        run.start();
        return run;
    }

    /** Ce qui doit être identique après un rejeu. */
    private record Snapshot(int turn, double hp, double enemyHp, List<String> hand, String terrain, int cigarettes) {
        static Snapshot of(GameState s) {
            return new Snapshot(s.turn(), s.player().hp(), s.currentEnemy().hp(),
                    s.hand().stream().map(Card::name).toList(), s.terrain().name(), s.cigarettes());
        }
    }

    // --- Codes ---

    @Test
    void codeEquipeLeSecretPourCettePartie() {
        Run run = run(1, Secret.CTRL_Z);
        assertTrue(run.state().hasTrinket(ItemRegistry.CTRL_Z));
        assertEquals(List.of(Secret.CTRL_Z), run.codes());

        Run next = run(1);
        assertFalse(next.state().hasTrinket(ItemRegistry.CTRL_Z), "rien à la partie suivante");
    }

    @Test
    void codeApresLeDebutRefuse() {
        Run run = run(1);
        assertThrows(IllegalStateException.class, () -> run.addCode(Secret.CLAUDE_MD));
    }

    @Test
    void codeNeDebloqueRien() {
        UnlockStore store = new UnlockStore.InMemory();
        Run run = new Run(1, store.load(), bus, RunTest::sturdy, RunTest::pool);
        run.addCode(Secret.CLAUDE_MD);
        UnlockTracker tracker = new UnlockTracker(run::state, store, bus);
        bus.subscribe(tracker);
        run.start();
        assertTrue(run.state().hasTrinket(ItemRegistry.CLAUDE_MD));
        assertTrue(run.state().luckyRolls());
        assertTrue(store.load().isEmpty());
        assertTrue(tracker.unlockedThisRun().isEmpty());
    }

    // --- Ctrl+Z ---

    @Test
    void ctrlZRevientAuDebutDuTourPrecedent() {
        Run run = run(3, Secret.CTRL_Z);
        assertTrue(run.startTurn());
        Snapshot turn1 = Snapshot.of(run.state());
        run.submit(new Action.Smoke()); // action gratuite : annulée aussi
        run.submit(new Action.PlayCard(0));
        assertTrue(run.startTurn());
        assertFalse(Snapshot.of(run.state()).equals(turn1));

        assertTrue(run.canUndo());
        assertEquals(ActionResult.FREE_ACTION, run.submit(new Action.Undo()));
        assertEquals(turn1, Snapshot.of(run.state()));
        assertTrue(events.contains(new GameEvent.TurnUndone(1)));
    }

    @Test
    void rejouerLaMemeActionRedonneLeMemeEtat() {
        Run reference = run(5);
        reference.startTurn();
        reference.submit(new Action.PlayCard(1));
        reference.startTurn();
        Snapshot expected = Snapshot.of(reference.state());

        Run run = run(5, Secret.CTRL_Z);
        run.startTurn();
        run.submit(new Action.PlayCard(1));
        run.startTurn();
        run.submit(new Action.Undo());
        run.submit(new Action.PlayCard(1));
        run.startTurn();
        assertEquals(expected, Snapshot.of(run.state()));
    }

    @Test
    void ctrlZUneFoisParCombat() {
        Run run = run(2, Secret.CTRL_Z);
        run.startTurn();
        run.submit(new Action.PlayCard(0));
        run.startTurn();
        run.submit(new Action.Undo());
        run.submit(new Action.PlayCard(0));
        run.startTurn();
        assertFalse(run.canUndo());
        events.clear();
        assertEquals(ActionResult.REJECTED, run.submit(new Action.Undo()));
        assertTrue(events.contains(new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.UNDO_UNAVAILABLE)));
    }

    @Test
    void ctrlZImpossibleAuPremierTourDuCombat() {
        Run run = run(2, Secret.CTRL_Z);
        run.startTurn();
        assertFalse(run.canUndo());
        assertEquals(ActionResult.REJECTED, run.submit(new Action.Undo()));
    }

    @Test
    void ctrlZImpossibleSansLeTrinket() {
        Run run = run(2);
        run.startTurn();
        run.submit(new Action.PlayCard(0));
        run.startTurn();
        assertFalse(run.canUndo());
        assertEquals(ActionResult.REJECTED, run.submit(new Action.Undo()));
    }

    @Test
    void ctrlZNeRevientPasAuCombatPrecedent() {
        // Premier adversaire tué en un sort : le tour 2 est le premier du combat 2.
        Run run = new Run(1, Set.of(), bus,
                () -> new GameState(new Player(100), List.of(new Enemy("A", FIRE, 1, 1), new Enemy("B", FIRE, 200, 1)),
                        new TerrainCard("EPFL", FIRE), List.of(), new Stats()),
                () -> new CardPool(List.of(new SpellCard("Jet d'eau", WATER, 10))));
        run.addCode(Secret.CTRL_Z);
        run.start();
        run.startTurn();
        run.submit(new Action.PlayCard(0));
        while (run.hasPendingReward()) {
            run.resolveReward(new cardistry.core.RewardChoice.Skip());
        }
        assertEquals(2, run.state().combatNumber());
        assertTrue(run.startTurn());
        assertFalse(run.canUndo());
        run.submit(new Action.PlayCard(0));
        run.startTurn();
        assertTrue(run.canUndo(), "nouveau combat : Ctrl+Z disponible");
    }

    @Test
    void ctrlZRejoueLesDecisionsDuJoueur() {
        // Pioche au choix (3 cartes) : les choix faits avant le tour annulé sont rejoués.
        java.util.function.Supplier<GameState> withChoices = () -> {
            GameState s = sturdy();
            s.playerStats().add(StatModifier.flat(StatType.DRAW_CHOICES, 2, "test"));
            return s;
        };
        int[] answers = {2, 1, 0};
        int[] asked = {0};
        PlayerDecisions decisions = new PlayerDecisions() {
            @Override
            public int chooseDrawnCard(GameState state, List<Card> options) {
                return answers[asked[0]++ % answers.length];
            }

            @Override
            public int chooseTrinketToDiscard(GameState state) {
                return 0;
            }
        };
        Run run = new Run(8, Set.of(), bus, withChoices, RunTest::pool);
        run.addCode(Secret.CTRL_Z);
        run.setDecisions(decisions);
        run.start();
        run.startTurn();
        run.submit(new Action.DrawCard());
        run.startTurn();
        run.submit(new Action.DrawCard());
        run.startTurn();
        Snapshot turn3 = Snapshot.of(run.state());
        run.submit(new Action.DrawCard());
        run.startTurn();

        int askedBefore = asked[0];
        run.submit(new Action.Undo());
        assertEquals(askedBefore, asked[0], "le rejeu ne redemande rien au joueur");
        assertEquals(turn3, Snapshot.of(run.state()));
    }

    // --- Secrets débloqués ---

    @Test
    void lesDeblocagesDeLaPartieNeChangentPasSonPool() {
        UnlockStore store = new UnlockStore.InMemory();
        Run run = new Run(1, store.load(), bus);
        store.save(EnumSet.of(Secret.CTRL_Z));
        assertTrue(run.unlockedSecrets().isEmpty());
        assertEquals(Set.of(Secret.CTRL_Z), new Run(1, store.load(), bus).unlockedSecrets());
    }
}

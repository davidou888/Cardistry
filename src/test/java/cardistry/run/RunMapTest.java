package cardistry.run;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.enemy.EnemyRank;
import cardistry.run.map.MapNode;
import cardistry.run.map.NodeType;
import cardistry.run.map.RunMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Une partie sur la carte : 3 niveaux, nœuds, boss, déterminisme. */
class RunMapTest {

    private final List<GameEvent> events = new ArrayList<>();

    /** Partie où chaque sort tue (pool d'une seule carte surpuissante). */
    private Run strongRun(long seed) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        Run run = new Run(seed, Set.of(), bus, GameState::newGame,
                () -> new CardPool(List.of(new SpellCard("Météore", FIRE, 10_000))));
        run.start();
        return run;
    }

    /** Joue jusqu'à la fin en prenant toujours le nœud {@code pick} (modulo les choix) ; renvoie les nœuds. */
    private static List<MapNode> playToEnd(Run run, int pick) {
        List<MapNode> path = new ArrayList<>();
        int guard = 0;
        while (!run.isOver() && guard++ < 10_000) {
            switch (run.phase()) {
                case MAP -> {
                    int index = pick % run.choices().size();
                    path.add(run.choices().get(index));
                    run.chooseNode(index);
                }
                case COMBAT -> {
                    if (run.startTurn()) {
                        run.submit(new Action.PlayCard(0));
                    }
                }
                case REWARD -> run.resolveReward(new RewardChoice.Skip());
                case OVER -> {
                }
            }
        }
        return path;
    }

    @Test
    void departSurLaCarte() {
        Run run = strongRun(1);
        assertEquals(Run.Phase.MAP, run.phase());
        assertEquals(1, run.state().level());
        assertEquals(0, run.state().floor());
        assertEquals(run.map().orElseThrow().floor(1), run.choices());
        assertTrue(events.contains(new GameEvent.LevelStarted(1)));
        assertThrows(IllegalStateException.class, run::startTurn);
    }

    @Test
    void unCombatParNoeudDeCombat() {
        Run run = strongRun(1);
        run.chooseNode(0);
        assertEquals(Run.Phase.COMBAT, run.phase());
        assertEquals(1, run.state().floor());
        assertEquals(EnemyRank.COMMON, run.state().currentEnemy().rank());
        assertTrue(run.startTurn());
        assertEquals(ActionResult.TURN_ENDED, run.submit(new Action.PlayCard(0)));
        assertTrue(run.phase() == Run.Phase.REWARD || run.phase() == Run.Phase.MAP);
        if (run.phase() == Run.Phase.REWARD) {
            run.resolveReward(new RewardChoice.Skip());
        }
        assertEquals(Run.Phase.MAP, run.phase());
        assertEquals(run.map().orElseThrow().choicesAfter(run.position()), run.choices());
        assertEquals(2, run.choices().get(0).floor());
    }

    @Test
    void troisNiveauxPuisVictoire() {
        Run run = strongRun(3);
        List<MapNode> path = playToEnd(run, 0);
        assertTrue(run.state().isWon());
        assertEquals(3, run.state().level());
        assertEquals(RunMap.FLOORS * 3, path.size());
        assertEquals(3, path.stream().filter(n -> n.type() == NodeType.BOSS).count());
        assertTrue(events.contains(new GameEvent.LevelStarted(2)));
        assertTrue(events.contains(new GameEvent.LevelStarted(3)));
        assertEquals(new GameEvent.GameWon(), events.stream().filter(e -> e instanceof GameEvent.GameWon).findFirst()
                .orElseThrow());
        List<String> bosses = events.stream().filter(e -> e instanceof GameEvent.CombatStarted)
                .map(e -> ((GameEvent.CombatStarted) e).enemy())
                .filter(e -> e.rank() == EnemyRank.BOSS).map(e -> e.id()).toList();
        assertEquals(List.of("boss-1", "boss-2", "boss-3"), bosses);
    }

    @Test
    void elitesEtNiveauDesAdversaires() {
        // Sur plusieurs seeds et chemins : élites sur les nœuds Élite, niveau appliqué.
        for (long seed = 0; seed < 20; seed++) {
            events.clear();
            Run run = strongRun(seed);
            playToEnd(run, (int) seed);
            List<GameEvent> list = List.copyOf(events);
            int level = 0;
            String kind = null;
            for (GameEvent event : list) {
                if (event instanceof GameEvent.LevelStarted l) {
                    level = l.level();
                } else if (event instanceof GameEvent.NodeEntered n) {
                    kind = n.kind();
                } else if (event instanceof GameEvent.CombatStarted c) {
                    EnemyRank expected = switch (kind) {
                        case "Élite" -> EnemyRank.ELITE;
                        case "Boss" -> EnemyRank.BOSS;
                        default -> EnemyRank.COMMON;
                    };
                    assertEquals(expected, c.enemy().rank(), kind);
                    assertEquals(cardistry.model.enemy.EnemyRegistry.DAMAGE_SCALE[level - 1], c.enemy().damageScale());
                }
            }
        }
    }

    @Test
    void memeSeedMemesChoixMemePartie() {
        Run a = strongRun(9);
        List<MapNode> pathA = playToEnd(a, 1);
        List<String> enemiesA = enemies();
        events.clear();
        Run b = strongRun(9);
        List<MapNode> pathB = playToEnd(b, 1);
        assertEquals(pathA, pathB);
        assertEquals(enemiesA, enemies());
        assertEquals(a.state().choomies(), b.state().choomies());
    }

    private List<String> enemies() {
        return events.stream().filter(e -> e instanceof GameEvent.CombatStarted)
                .map(e -> ((GameEvent.CombatStarted) e).enemy().id()).toList();
    }

    @Test
    void ctrlZRejoueLaCarte() {
        EventBus bus = new EventBus();
        Run run = new Run(5, Set.of(), bus, GameState::newGame,
                () -> new CardPool(List.of(new cardistry.model.card.TerrainCard("Forêt", cardistry.model.Element.NATURE))));
        run.addCode(Secret.CTRL_Z);
        run.start();
        run.chooseNode(1);
        MapNode position = run.position();
        String enemy = run.state().currentEnemy().id();
        run.startTurn();
        run.submit(new Action.PlayCard(0));
        run.startTurn();
        double hp = run.state().player().hp();
        assertTrue(run.canUndo());
        assertEquals(ActionResult.FREE_ACTION, run.submit(new Action.Undo()));
        assertEquals(position, run.position());
        assertEquals(enemy, run.state().currentEnemy().id());
        assertEquals(1, run.state().turn());
        assertEquals(Run.Phase.COMBAT, run.phase());
        assertNotNull(run.map().orElseThrow());
        assertTrue(run.state().player().hp() >= hp);
        assertSame(run.choices().size(), run.map().orElseThrow().choicesAfter(position).size());
    }
}

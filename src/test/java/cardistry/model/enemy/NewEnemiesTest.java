package cardistry.model.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.DeadCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Adversaires des nouveaux éléments, le Gobelin et l'Engeance des cookies. */
class NewEnemiesTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /** Combat sur un terrain neutre sans effet (ses malus sont testés à part). */
    private void setUp(String id, long seed) {
        enemy = EnemyRegistry.create(id, 1);
        state = new GameState(new Player(500), List.of(enemy), new TerrainCard("Neutre", Element.NEUTRAL, Rarity.COMMON),
                List.of(), new Stats()); // main vide : le joueur pioche sans jamais remplir sa main
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
    }

    /** {@code n} tours où le joueur pioche ; renvoie les PV perdus à chaque tour. */
    private List<Integer> turns(int n) {
        List<Integer> lost = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int before = state.player().hp();
            if (engine.startTurn()) {
                engine.submit(new Action.DrawCard());
            }
            lost.add(before - state.player().hp());
        }
        return lost;
    }

    @Test
    void fichesDesNouveauxAdversaires() {
        record Expected(String id, String name, Element element, EnemyRank rank, int hp, String terrain) {
        }
        for (Expected e : List.of(
                new Expected(EnemyRegistry.GOUROU, "Gourou du Sat", Element.SPIRIT, EnemyRank.COMMON, 24,
                        "Salle de méditation"),
                new Expected(EnemyRegistry.NG, "NG", Element.ALGEBRAIC, EnemyRank.COMMON, 26, "Salle d'exercices"),
                new Expected(EnemyRegistry.HYPERCUBE, "Hypercube misogyne", Element.GEOMETRIC, EnemyRank.COMMON, 25,
                        "Cabinet psy scintillant"),
                new Expected(EnemyRegistry.GOBELIN, "Gobelin", Element.NATURE, EnemyRank.COMMON, 22,
                        "Clairière d'adolescent de base"),
                new Expected(EnemyRegistry.ENGEANCE, "Engeance des cookies", Element.GLITCH, EnemyRank.ELITE, 60,
                        "Le canapé"))) {
            Enemy created = EnemyRegistry.create(e.id(), 1);
            assertEquals(e.name(), created.name());
            assertEquals(e.element(), created.element(), e.id());
            assertEquals(e.rank(), created.rank(), e.id());
            assertEquals(e.hp(), created.hp(), e.id());
            assertEquals(e.terrain(), created.terrain().orElseThrow().name(), e.id());
        }
        assertTrue(EnemyRegistry.COMMONS.containsAll(List.of(EnemyRegistry.GOUROU, EnemyRegistry.NG,
                EnemyRegistry.HYPERCUBE, EnemyRegistry.GOBELIN)));
        assertTrue(EnemyRegistry.ELITES.contains(EnemyRegistry.ENGEANCE));
    }

    @Test
    void gourouRegardFixeHypnotiseSonCoupSuivantDouble() {
        setUp(EnemyRegistry.GOUROU, 1);
        assertEquals(List.of(4, 4, 0, 8, 4), turns(5), "encens 4, 4, regard fixe, puis ×2, puis normal");
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.StatusApplied s
                && s.type() == StatusType.HYPNOTIZED && s.target() == StatusTarget.PLAYER));
    }

    @Test
    void ngDemonstrationPuisContreExemple() {
        setUp(EnemyRegistry.NG, 1);
        assertEquals(List.of(5, 5, 0), turns(3));
        assertTrue(state.hand().stream().anyMatch(c -> c instanceof DeadCard d && d.name().equals("Exercice 3b")));
        assertEquals("Démonstration", EnemyRegistry.create(EnemyRegistry.NG, 1).peekIntent().name());
    }

    @Test
    void hypercubeTunnelTopologiquePuisCelibatForce() {
        setUp(EnemyRegistry.HYPERCUBE, 1);
        assertEquals(List.of(5, 5, 8), turns(3), "Célibat forcé : 2 × 4");
        assertEquals("Tunnel topologique", EnemyRegistry.create(EnemyRegistry.HYPERCUBE, 1).peekIntent().name());
    }

    @Test
    void gobelinSixSeveeenSixOuSept() {
        Set<Integer> seen = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            setUp(EnemyRegistry.GOBELIN, seed);
            seen.addAll(turns(2));
        }
        assertEquals(Set.of(6, 7), seen);
        assertEquals("67", EnemyRegistry.create(EnemyRegistry.GOBELIN, 1).peekIntent().name());
        assertEquals(EnemyRegistry.SIX_SEVEN, EnemyRegistry.create(EnemyRegistry.GOBELIN, 1).peekIntent().id());
    }

    @Test
    void engeancePiegePatissierPuisGreenOut() {
        setUp(EnemyRegistry.ENGEANCE, 1);
        assertEquals(List.of(9, 9), turns(2));
        turns(1); // Green out : tu t'endors
        assertTrue(state.statuses(StatusTarget.PLAYER).has(StatusType.FONS));
        assertFalse(engine.startTurn(), "tour sauté");
    }

    @Test
    void engeanceSousLaMoitieMeditationTriptyqueChangeDElementUneFois() {
        Set<Element> reached = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) {
            setUp(EnemyRegistry.ENGEANCE, seed);
            enemy.takeDamage(31); // 29/60
            turns(1);
            Element after = enemy.element();
            assertNotEquals(Element.GLITCH, after);
            assertNotEquals(Element.NEUTRAL, after);
            reached.add(after);
            turns(4);
            assertEquals(after, enemy.element(), "une seule fois");
            assertEquals(1, events.stream().filter(e -> e instanceof GameEvent.EnemyElementChanged).count());
            events.clear();
        }
        assertTrue(reached.size() >= 4, "élément tiré au hasard : " + reached);
    }

    @Test
    void terrainsDesNouveauxAdversaires() {
        assertTrue(EnemyTerrains.of(EnemyRegistry.GOUROU).effectText().orElseThrow().contains("−15 %"));
        assertTrue(EnemyTerrains.of(EnemyRegistry.NG).effectText().orElseThrow().contains("−1 taille de main"));
        assertTrue(EnemyTerrains.of(EnemyRegistry.HYPERCUBE).effectText().orElseThrow().contains("+15 %"));
        assertEquals(Element.GEOMETRIC, EnemyTerrains.of(EnemyRegistry.HYPERCUBE).element());
        assertTrue(EnemyTerrains.of(EnemyRegistry.ENGEANCE).bonuses().isEmpty());
    }
}

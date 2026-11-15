package cardistry.core.status;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Système de statuts générique, testé avec l'Intox. */
class StatusTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /** Adversaire neutre de {@code hp} PV qui frappe de {@code attack} ; main : un sort de 1 dégât. */
    private void setUp(double hp, double attack, Stats stats) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", Element.NEUTRAL, hp, attack);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 50, 1)),
                new TerrainCard("Dojo", Element.NEUTRAL, Rarity.COMMON),
                List.of(new SpellCard("Pichenette", Element.NEUTRAL, 1, Rarity.COMMON)), stats);
        engine = new CombatEngine(state, new CardPool(), new Rng(3), bus, new LootTable(List::of));
    }

    private void setUp(double hp, double attack) {
        setUp(hp, attack, new Stats());
    }

    /** Utilise un objet qui inflige {@code type} à {@code target} avec la chance donnée. */
    private boolean inflict(StatusTarget target, StatusType type, double chance) {
        boolean[] landed = new boolean[1];
        engine.give(new Item("test", "Test", "", "", Rarity.COMMON, Set.of(),
                ctx -> landed[0] = ctx.applyStatus(target, type, chance)));
        engine.submit(new Action.UseItem(state.items().size() - 1));
        return landed[0];
    }

    private void drawPass() {
        engine.submit(new Action.DrawCard());
    }

    @Test
    void intoxSurLAdversaireMoinsCinqAuDebutDeSonTourPendantTroisTours() {
        setUp(100, 0);
        engine.startTurn();
        assertTrue(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        assertEquals(3, state.statuses(StatusTarget.ENEMY).get(StatusType.INTOX).orElseThrow().turnsLeft());
        assertEquals(100, enemy.hp(), "rien avant son tour");
        for (int turn = 1; turn <= 3; turn++) {
            drawPass();
            assertEquals(100 - 5 * turn, enemy.hp());
            engine.startTurn();
        }
        assertFalse(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
        drawPass();
        assertEquals(85, enemy.hp(), "trois tours seulement");
        assertTrue(events.contains(new GameEvent.StatusEnded(StatusTarget.ENEMY, StatusType.INTOX)));
        assertTrue(events.contains(new GameEvent.DamageDealt(enemy, 5, 1.0, GameEvent.DamageDealt.Source.STATUS)));
    }

    @Test
    void intoxNeSeReappliquePasTantQuIlEstActif() {
        setUp(100, 0);
        engine.startTurn();
        assertTrue(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        drawPass(); // 1er tour d'Intox : il en reste 2
        engine.startTurn();
        assertFalse(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        assertEquals(2, state.statuses(StatusTarget.ENEMY).get(StatusType.INTOX).orElseThrow().turnsLeft());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.StatusRefused r
                && r.type() == StatusType.INTOX));
    }

    @Test
    void intoxSurLeJoueurAuDebutDeSonTourEtDegatsBruts() {
        Stats stats = new Stats();
        stats.add(StatModifier.flat(StatType.DAMAGE_REDUCTION, 3, "test")); // ne réduit pas un statut
        setUp(100, 0, stats);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        assertEquals(100, state.player().hp());
        drawPass();
        engine.startTurn();
        assertEquals(95, state.player().hp());
    }

    @Test
    void lIntoxPeutTuerLAdversaireEtLeCombatSuivantCommence() {
        setUp(5, 0);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.INTOX, 1);
        drawPass();
        assertFalse(enemy.isAlive());
        assertTrue(events.contains(new GameEvent.EnemyKilled(enemy)));
        assertTrue(state.statuses(StatusTarget.ENEMY).isEmpty(), "statuts retirés en fin de combat");
    }

    @Test
    void lIntoxPeutTuerLeJoueur() {
        setUp(100, 0);
        state.player().takeDamage(97);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        drawPass();
        engine.startTurn();
        assertTrue(state.isLost());
        assertTrue(events.contains(new GameEvent.GameLost()));
    }

    @Test
    void lesStatutsDuJoueurSArretentALaFinDuCombat() {
        setUp(1, 0);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        engine.submit(new Action.PlayCard(0)); // tue l'adversaire (1 PV)
        assertTrue(state.statuses(StatusTarget.PLAYER).isEmpty());
        assertTrue(events.contains(new GameEvent.StatusEnded(StatusTarget.PLAYER, StatusType.INTOX)));
    }

    @Test
    void chanceDuJoueurDoubleeParClaudeMdMaisPasCelleSubie() {
        // Avec CLAUDE.md, un statut infligé à l'adversaire tente deux fois ; subi par le joueur, une fois.
        int landedOnEnemy = 0;
        int landedOnPlayer = 0;
        for (int seed = 0; seed < 400; seed++) {
            Stats stats = new Stats();
            stats.add(StatModifier.flat(StatType.LUCKY_ROLLS, 1, "claude"));
            EventBus bus = new EventBus();
            Enemy target = new Enemy("Cible", Element.NEUTRAL, 100, 0);
            state = new GameState(new Player(100), List.of(target), new TerrainCard("Dojo", Element.NEUTRAL,
                    Rarity.COMMON), List.of(), stats);
            engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
            engine.startTurn();
            landedOnEnemy += inflict(StatusTarget.ENEMY, StatusType.INTOX, 0.5) ? 1 : 0;
            landedOnPlayer += inflict(StatusTarget.PLAYER, StatusType.INTOX, 0.5) ? 1 : 0;
        }
        assertTrue(landedOnEnemy > 260 && landedOnEnemy < 340, "≈ 75 % : " + landedOnEnemy);
        assertTrue(landedOnPlayer > 160 && landedOnPlayer < 240, "≈ 50 % : " + landedOnPlayer);
    }
}

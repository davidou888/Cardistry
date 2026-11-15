package cardistry.model.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pouvoirs de cartes : texte + effet après l'effet principal, dégâts calculés, non multipliés. */
class CardPowerTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    private void setUp(Element enemyElement, Stats stats, Card... hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", enemyElement, 100, 0);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", Element.FIRE, 50, 0)),
                new TerrainCard("Dojo", Element.NEUTRAL, Rarity.COMMON), List.of(hand), stats);
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
        engine.startTurn();
    }

    private void play(int slot) {
        engine.submit(new Action.PlayCard(slot));
    }

    @Test
    void copyConserveLePouvoirLaRegleEtLeNonMultiplie() {
        SpellCard card = new SpellCard("Test", Element.FIRE, 10, Rarity.RARE)
                .withPower("brûle", ctx -> { })
                .withDamageRule("1-40", ctx -> 7)
                .unmultiplied();
        SpellCard copy = card.copy();
        assertNotSame(card, copy);
        assertEquals("brûle", copy.power().orElseThrow().text());
        assertSame(card.power().orElseThrow(), copy.power().orElseThrow());
        assertTrue(copy.hasDamageRule());
        assertEquals("1-40", copy.damageLabel().orElseThrow());
        assertTrue(copy.isUnmultiplied());
        assertEquals(Rarity.RARE, copy.rarity());
        HealCard heal = new HealCard("Soin", Element.WATER, 10, Rarity.COMMON).withPower("purge", ctx -> { });
        assertEquals("purge", heal.copy().power().orElseThrow().text());
        TerrainCard terrain = new TerrainCard("T", Element.WATER, Rarity.COMMON).withPower("x", ctx -> { });
        assertTrue(terrain.copy().power().isPresent());
        assertFalse(new SpellCard("Nu", Element.FIRE, 1, Rarity.COMMON).copy().power().isPresent());
    }

    @Test
    void lePouvoirSeDeclencheApresLesDegats() {
        setUp(Element.NEUTRAL, new Stats(), new SpellCard("Boule", Element.FIRE, 10, Rarity.COMMON)
                .withPower("Intox", ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.INTOX, 1)));
        play(0);
        int damage = indexOf(GameEvent.DamageDealt.class);
        int power = indexOf(GameEvent.CardPowerTriggered.class);
        int status = indexOf(GameEvent.StatusApplied.class);
        assertTrue(damage >= 0 && damage < power && power < status, events.toString());
        assertEquals(100 - 10 - 5, enemy.hp(), "10 du sort, puis 5 d'Intox au début de son tour");
        assertTrue(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
    }

    private int indexOf(Class<? extends GameEvent> type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.isInstance(events.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void laRegleDeDegatsLitLesPvAvantLeCoup() {
        int[] seen = new int[1];
        setUp(Element.NEUTRAL, new Stats(), new SpellCard("Règle", Element.NEUTRAL, 10, Rarity.COMMON)
                .withDamageRule("?", ctx -> {
                    seen[0] = ctx.enemyHp();
                    return 25;
                }));
        play(0);
        assertEquals(100, seen[0]);
        assertEquals(75, enemy.hp());
    }

    @Test
    void desDegatsNonMultipliesIgnorentElementStatsEtHypnose() {
        Stats stats = new Stats();
        stats.add(StatModifier.percent(StatType.WATER_DAMAGE, 1.0, "test"));
        setUp(Element.FIRE, stats, new SpellCard("Brut", Element.WATER, 35, Rarity.LEGENDARY).unmultiplied());
        state.statuses(StatusTarget.ENEMY).apply(StatusType.HYPNOTIZED, 1, StatusTarget.PLAYER);
        play(0);
        assertEquals(65, enemy.hp());
    }

    @Test
    void unSortSansDegatsNeFrappePasMaisDeclencheSonPouvoir() {
        setUp(Element.NEUTRAL, new Stats(), new SpellCard("Somnifère", Element.SPIRIT, 0, Rarity.UNCOMMON)
                .withPower("Fons", ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.FONS, 1, 1)));
        play(0);
        assertEquals(-1, indexOf(GameEvent.DamageDealt.class));
        assertTrue(indexOf(GameEvent.EnemyTurnSkipped.class) > indexOf(GameEvent.CardPowerTriggered.class),
                "endormi pour son tour");
    }

    @Test
    void soinEtTerrainDeclenchentAussiLeurPouvoir() {
        setUp(Element.NEUTRAL, new Stats(),
                new HealCard("Soupe", Element.NEUTRAL, 10, Rarity.COMMON)
                        .withPower("retire l'Intox", ctx -> ctx.removeStatus(StatusTarget.PLAYER, StatusType.INTOX)),
                new TerrainCard("Lieu", Element.NATURE, Rarity.COMMON)
                        .withPower("Intox", ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.INTOX, 1)));
        state.statuses(StatusTarget.PLAYER).apply(StatusType.INTOX, 3, StatusTarget.ENEMY);
        play(0);
        assertFalse(state.statuses(StatusTarget.PLAYER).has(StatusType.INTOX));
        engine.startTurn();
        play(0);
        assertTrue(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
    }

    @Test
    void purgeRetireLesStatutsNegatifsDuJoueurMaisPasLHypnose() {
        setUp(Element.NEUTRAL, new Stats(), new HealCard("Berceuse", Element.SPIRIT, 0, Rarity.UNCOMMON)
                .withPower("purge", ctx -> assertEquals(4, ctx.purgeNegativeStatuses())));
        var player = state.statuses(StatusTarget.PLAYER);
        for (StatusType type : List.of(StatusType.INTOX, StatusType.FONS, StatusType.BONDAGE, StatusType.BURN,
                StatusType.HYPNOTIZED)) {
            player.apply(type, 2, StatusTarget.ENEMY);
        }
        play(0);
        assertEquals(List.of(StatusType.HYPNOTIZED), player.all().stream().map(s -> s.type()).toList());
    }

    @Test
    void bonusDeDegatsPourLeCombatCumulableEtRetireEnFin() {
        setUp(Element.NEUTRAL, new Stats(),
                new HealCard("Roger", Element.FIRE, 0, Rarity.COMMON).withPower("+10 %", ctx -> ctx.addCombatDamageBonus(0.1)),
                new HealCard("Roger", Element.FIRE, 0, Rarity.COMMON).withPower("+10 %", ctx -> ctx.addCombatDamageBonus(0.1)),
                new SpellCard("Fin", Element.NEUTRAL, 1000, Rarity.COMMON));
        play(0);
        engine.startTurn();
        play(0);
        assertEquals(1.2, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9);
        assertEquals(1.2, state.playerStats().get(StatType.FIRE_DAMAGE), 1e-9);
        engine.startTurn();
        play(0); // tue l'adversaire : fin du combat
        assertEquals(1.0, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9);
    }

    @Test
    void etagesParcourus() {
        setUp(Element.NEUTRAL, new Stats());
        assertEquals(0, state.floorsTraveled());
        engine.enterNode(1, "Combat");
        engine.enterNode(2, "Combat");
        engine.startLevel(2);
        engine.enterNode(1, "Combat");
        assertEquals(3, state.floorsTraveled());
    }
}

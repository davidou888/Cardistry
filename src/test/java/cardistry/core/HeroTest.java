package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.hero.Hero;
import cardistry.hero.HeroRegistry;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Capacités des trois personnages. */
class HeroTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    private void setUp(Hero hero, long seed, Element terrain, List<Card> hand, Enemy... enemies) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(enemies), new TerrainCard("Terrain", terrain, cardistry.items.Rarity.COMMON), hand, new Stats())
                .withHero(hero);
        engine = new CombatEngine(state, new CardPool(hand), new Rng(seed), bus, new LootTable(List::of)); // main = ce sort
        engine.start();
    }

    private static Enemy enemy(Element element, double hp) {
        return new Enemy("Mannequin", element, hp, 5);
    }

    private static SpellCard spell(Element element, double damage) {
        return new SpellCard("Sort", element, damage, cardistry.items.Rarity.COMMON);
    }

    private double damageToEnemy() {
        return events.stream().filter(e -> e instanceof GameEvent.DamageDealt d && d.target() instanceof Enemy)
                .mapToDouble(e -> ((GameEvent.DamageDealt) e).amount()).sum();
    }

    private double healed() {
        return events.stream().filter(GameEvent.Healed.class::isInstance)
                .mapToDouble(e -> ((GameEvent.Healed) e).amount()).sum();
    }

    @Test
    void troisPersonnagesDansLOrdre() {
        assertEquals(List.of("Louis", "Dave", "Sassy"), HeroRegistry.all().stream().map(Hero::name).toList());
        assertEquals(HeroRegistry.DAVE, HeroRegistry.hero("dave"));
    }

    // --- Louis : MAN ---

    @Test
    void louisPremierSortDuCombatPlus50PourCent() {
        setUp(HeroRegistry.LOUIS, 1, WATER, List.of(spell(FIRE, 10), spell(FIRE, 10)), enemy(FIRE, 500));
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(15, damageToEnemy(), DELTA, "10 × 1.5");

        events.clear();
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(10, damageToEnemy(), DELTA, "le deuxième sort est normal");
    }

    @Test
    void louisLeBonusRevientAChaqueCombat() {
        setUp(HeroRegistry.LOUIS, 1, WATER, List.of(spell(FIRE, 10)), enemy(FIRE, 15), enemy(FIRE, 500));
        engine.startTurn();
        engine.submit(new Action.PlayCard(0)); // 15 dégâts : le premier adversaire meurt
        assertEquals(2, state.combatNumber());
        assertTrue(state.nextSpellBoost().isPresent(), "MAN est de retour");
        assertEquals(HeroRegistry.MAN_FIRST_SPELL, state.nextSpellBoost().get().spellMultiplier(), DELTA);
    }

    @Test
    void louisGueuleDeBoisDoublee() {
        setUp(HeroRegistry.LOUIS, 1, WATER, List.of(spell(FIRE, 100)), enemy(FIRE, 20), enemy(FIRE, 500));
        int handSize = state.handSize();
        for (int i = 0; i < 3; i++) {
            engine.give(new Item("biere", "Bière", "x", "", Rarity.COMMON, Set.of(Tag.DRINK), ctx -> { }));
            engine.submit(new Action.UseItem(0));
        }
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));

        assertEquals(1, state.activeIntensity(Curse.HANGOVER));
        assertEquals(handSize - 2, state.handSize(), "−2 de taille de main par point d'intensité");
        assertEquals("−2 taille de main", state.describeCurse(Curse.HANGOVER, 1));
    }

    @Test
    void sansLouisGueuleDeBoisNormale() {
        setUp(HeroRegistry.DAVE, 1, WATER, List.of(spell(FIRE, 100)), enemy(FIRE, 20), enemy(FIRE, 500));
        int handSize = state.handSize();
        for (int i = 0; i < 3; i++) {
            engine.give(new Item("biere", "Bière", "x", "", Rarity.COMMON, Set.of(Tag.DRINK), ctx -> { }));
            engine.submit(new Action.UseItem(0));
        }
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(handSize - 1, state.handSize());
        assertEquals("−1 taille de main", state.describeCurse(Curse.HANGOVER, 1));
    }

    // --- Dave : Full skill Mr. Le Juge ---

    @Test
    void daveSortDuTerrainQuiBatLAdversaireSoigneDesPvEnleves() {
        // Boule de feu sur un terrain feu contre un adversaire plante : 10 × 1.5 = 15 dégâts, 15 PV rendus.
        setUp(HeroRegistry.DAVE, 1, FIRE, List.of(spell(FIRE, 10)), enemy(NATURE, 500));
        state.player().takeDamage(50);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(15, damageToEnemy(), DELTA);
        assertEquals(15, healed(), DELTA);
    }

    @Test
    void daveSoigneSeulementLesPvReellementEnleves() {
        setUp(HeroRegistry.DAVE, 1, FIRE, List.of(spell(FIRE, 100)), enemy(NATURE, 8));
        state.player().takeDamage(50);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(8, healed(), DELTA, "l'adversaire n'avait plus que 8 PV");
    }

    @Test
    void daveRienSiLeTerrainNEstPasDeLElementDuSort() {
        setUp(HeroRegistry.DAVE, 1, WATER, List.of(spell(FIRE, 10)), enemy(NATURE, 500));
        state.player().takeDamage(50);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(0, healed(), DELTA);
    }

    @Test
    void daveRienSiLeSortNeBatPasLAdversaire() {
        setUp(HeroRegistry.DAVE, 1, FIRE, List.of(spell(FIRE, 10)), enemy(FIRE, 500));
        state.player().takeDamage(50);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(0, healed(), DELTA);
    }

    @Test
    void lesAutresPersonnagesNeSeSoignentPasAvecUnSort() {
        setUp(HeroRegistry.LOUIS, 1, FIRE, List.of(spell(FIRE, 10)), enemy(NATURE, 500));
        state.player().takeDamage(50);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(0, healed(), DELTA);
    }

    // --- Sassy : wadayatalkinabit ---

    @Test
    void sassyA130Pv() {
        GameState game = GameState.newGame(HeroRegistry.SASSY);
        assertEquals(130, game.maxHp(), DELTA);
        assertEquals(130, game.player().hp(), DELTA);
        assertEquals(100, GameState.newGame(HeroRegistry.LOUIS).maxHp(), DELTA);
        assertEquals(100, GameState.newGame(HeroRegistry.DAVE).maxHp(), DELTA);
    }

    @Test
    void sassyAlienInflige4DegatsParTourOuFumeUnJoint() {
        int misses = 0;
        int seeds = 400;
        for (long seed = 0; seed < seeds; seed++) {
            events.clear();
            setUp(HeroRegistry.SASSY, seed, WATER, List.of(spell(FIRE, 10)), enemy(FIRE, 500));
            engine.startTurn();
            boolean joint = events.stream().anyMatch(e -> e instanceof GameEvent.AbilityTriggered a
                    && a.message().contains("joint"));
            if (joint) {
                misses++;
                assertEquals(0, damageToEnemy(), DELTA, "seed " + seed);
            } else {
                assertEquals(HeroRegistry.ALIEN_DAMAGE, damageToEnemy(), DELTA, "seed " + seed);
                assertEquals(500 - 4, state.currentEnemy().hp(), DELTA);
            }
        }
        // 10 % de 400 = 40 attendus.
        assertTrue(misses > 20 && misses < 65, "ratés : " + misses + "/" + seeds);
    }

    @Test
    void sassyAlienSAjouteAuSortDuJoueur() {
        long seed = 0;
        while (new Rng(seed).nextDouble() < HeroRegistry.ALIEN_MISS_CHANCE) {
            seed++;
        }
        setUp(HeroRegistry.SASSY, seed, WATER, List.of(spell(FIRE, 10)), enemy(FIRE, 500));
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(4 + 10, damageToEnemy(), DELTA);
    }

    @Test
    void sassyAlienPeutAcheverLAdversaire() {
        long seed = 0;
        while (new Rng(seed).nextDouble() < HeroRegistry.ALIEN_MISS_CHANCE) {
            seed++;
        }
        setUp(HeroRegistry.SASSY, seed, WATER, List.of(spell(FIRE, 10)), enemy(FIRE, 3));
        assertFalse(engine.startTurn(), "l'adversaire meurt au début du tour : rien à jouer");
        assertTrue(engine.isOver() && state.isWon());
    }

    @Test
    void sansPersonnageAucuneCapacite() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(enemy(NATURE, 500)), new TerrainCard("Terrain", FIRE, cardistry.items.Rarity.COMMON),
                List.of(spell(FIRE, 10)), new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(spell(FIRE, 10))), new Rng(1), bus, new LootTable(List::of));
        engine.start();
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(15, damageToEnemy(), DELTA);
        assertEquals(0, healed(), DELTA);
        assertEquals("Toi", state.playerName());
    }
}

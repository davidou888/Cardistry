package cardistry.core;

import static cardistry.core.stats.StatModifier.flat;
import static cardistry.core.stats.StatModifier.percent;
import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.DamageDealt.Source;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Trinket;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Mécaniques ajoutées pour le contenu de l'étape 2. */
class EngineExtensionsTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy hydra;

    private void setUp(List<Card> hand, CardPool pool) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        hydra = new Enemy("Hydre", FIRE, 100, 10);
        state = new GameState(new Player(100), List.of(hydra), new TerrainCard("Forêt", NATURE), hand, new Stats());
        engine = new CombatEngine(state, pool, new Rng(4), bus, new LootTable(List::of));
    }

    private void setUp(List<Card> hand) {
        setUp(hand, new CardPool());
    }

    private void use(Consumer<EffectContext> effect) {
        engine.give(new Item("test", "test", "x", "", Rarity.COMMON, Set.of(), effect));
        engine.submit(new Action.UseItem(state.items().size() - 1));
    }

    // --- Attaque adverse ---

    @Test
    void reductionDeDegatsParAttaque() {
        setUp(List.of());
        state.playerStats().add(flat(StatType.DAMAGE_REDUCTION, 2, "test"));
        assertEquals(8, state.enemyAttackDamage(), DELTA); // Forêt : Hydre ×1, 10 − 2

        engine.submit(new Action.DrawCard());

        assertEquals(92, state.player().hp(), DELTA);
    }

    @Test
    void reductionJamaisSousZero() {
        setUp(List.of());
        state.playerStats().add(flat(StatType.DAMAGE_REDUCTION, 50, "test"));
        engine.submit(new Action.DrawCard());
        assertEquals(100, state.player().hp(), DELTA);
    }

    @Test
    void statDeDegatsAdverses() {
        setUp(List.of());
        state.playerStats().add(percent(StatType.ENEMY_DAMAGE, 0.25, "test"));
        engine.submit(new Action.DrawCard());
        assertEquals(new GameEvent.DamageDealt(state.player(), 12.5, 1.25, Source.ENEMY_ATTACK),
                events.get(events.size() - 1));
    }

    // --- Soins ---

    @Test
    void soinParElementSAjouteALEfficacite() {
        HealCard pétou = new HealCard("Petit pétou", FIRE, 10);
        setUp(List.of(pétou));
        state.player().takeDamage(50);
        state.playerStats().add(percent(StatType.FIRE_HEAL, 0.5, "grinder"));

        engine.submit(new Action.PlayCard(0));

        // Forêt (nature) : le feu bat la nature → soin feu ×1 ; × 1.5 (soin feu)
        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
    }

    @Test
    void soinParElementNeTouchePasLesAutres() {
        setUp(List.of(new HealCard("Potion", NATURE, 10)));
        state.player().takeDamage(50);
        state.playerStats().add(percent(StatType.FIRE_HEAL, 0.5, "grinder"));

        engine.submit(new Action.PlayCard(0));

        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1)); // Forêt : nature ×1.5
    }

    // --- Pioche ---

    @Test
    void cartesParPiocheLimiteesParLaMain() {
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hand.add(new TerrainCard("T" + i, FIRE));
        }
        setUp(hand);
        state.playerStats().add(flat(StatType.CARDS_PER_DRAW, 1, "photocopieuse"));

        engine.submit(new Action.DrawCard());
        assertEquals(7, state.hand().size());
        assertEquals(2, events.stream().filter(GameEvent.CardDrawn.class::isInstance).count());

        // Avec 6 cartes, une seule place.
        engine.submit(new Action.PlayCard(0));
        assertEquals(6, state.hand().size());
        events.clear();
        engine.submit(new Action.DrawCard());
        assertEquals(1, events.stream().filter(GameEvent.CardDrawn.class::isInstance).count());
    }

    @Test
    void poidsDePiocheModifiesParUnTrinket() {
        Card common = new SpellCard("Boule", FIRE, 10);
        Card rare = new HealCard("Petit pétou", FIRE, 15);
        CardPool pool = new CardPool(List.of(common, common, common, rare));
        setUp(List.of(), pool);
        engine.equip(Trinket.builder("g", "G", Rarity.COMMON).cardWeight("Petit pétou", 2).build());

        int pétous = 0;
        int draws = 20_000;
        Rng rng = new Rng(8);
        for (int i = 0; i < draws; i++) {
            if (pool.draw(rng, state::cardWeight).name().equals("Petit pétou")) {
                pétous++;
            }
        }
        // Sans trinket : 1/4. Avec un poids de 2 : 2/5.
        assertEquals(0.4, pétous / (double) draws, 0.015);
        assertEquals(2.0, state.cardWeight(rare), DELTA);
        assertEquals(1.0, state.cardWeight(common), DELTA);
    }

    @Test
    void piocheUniformeInchangeeSansTrinket() {
        CardPool pool = new CardPool();
        Rng a = new Rng(5);
        Rng b = new Rng(5);
        for (int i = 0; i < 50; i++) {
            assertEquals(pool.draw(a).name(), pool.draw(b, card -> 1.0).name());
        }
    }

    // --- Effets ---

    @Test
    void degatsElementairesCommeUnSort() {
        setUp(List.of());
        state.playerStats().add(percent(StatType.WATER_DAMAGE, 0.25, "test"));

        use(ctx -> ctx.elementalDamage(20, WATER));

        // Eau bat feu ×1.5, stat eau ×1.25
        assertEquals(new GameEvent.DamageDealt(hydra, 37.5, 1.875, Source.EFFECT), events.get(1));
        assertEquals(62.5, hydra.hp(), DELTA);
    }

    @Test
    void degatsBrutsDUnEffet() {
        setUp(List.of());
        use(ctx -> ctx.damageEnemy(5));
        assertEquals(new GameEvent.DamageDealt(hydra, 5, 1.0, Source.EFFECT), events.get(1));
    }

    @Test
    void remplacerUneCarteDeLaMain() {
        Card old = new TerrainCard("Vieille", FIRE);
        setUp(List.of(new TerrainCard("A", FIRE), old), new CardPool(List.of(new SpellCard("Neuve", WATER, 10))));

        use(ctx -> ctx.replaceCardInHand(1));

        assertEquals("Neuve", state.hand().get(1).name());
        GameEvent.CardReplaced replaced = (GameEvent.CardReplaced) events.get(1);
        assertEquals(old, replaced.removed());
        assertNotSame(state.hand().get(1), old);
        assertTrue(state.hand().get(0).name().equals("A"));
    }
}

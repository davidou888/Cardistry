package cardistry.items;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.DoublePredicate;
import org.junit.jupiter.api.Test;

/** Trinkets Secret : NullPointerException, Ctrl+Z (voir run.RunTest), CLAUDE.md. */
class SecretTrinketTest {

    private static final double DELTA = 1e-9;
    private static final TerrainCard EPFL = new TerrainCard("EPFL", FIRE, cardistry.items.Rarity.COMMON);

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private EventBus bus;

    /** {@code enemies} adversaires feu 100 PV ; main donnée ; rien n'est distribué (pas de start). */
    private void setUp(int enemies, List<Card> hand, long seed) {
        bus = new EventBus();
        bus.subscribe(events::add);
        List<Enemy> list = new ArrayList<>();
        for (int i = 0; i < enemies; i++) {
            list.add(new Enemy("E" + i, FIRE, 100, 10));
        }
        state = new GameState(new Player(100), list, EPFL, hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
    }

    private static List<Card> hand() {
        return List.of(new SpellCard("Boule de feu", FIRE, 10, cardistry.items.Rarity.COMMON), new HealCard("Potion", FIRE, 10, cardistry.items.Rarity.COMMON),
                new SpellCard("Jet d'eau", cardistry.model.Element.WATER, 10, cardistry.items.Rarity.COMMON));
    }

    /** Première seed dont les deux premiers {@code nextDouble} vérifient les conditions. */
    private static long seedWhere(DoublePredicate first, DoublePredicate second) {
        for (long seed = 0; ; seed++) {
            Rng rng = new Rng(seed);
            if (first.test(rng.nextDouble()) && second.test(rng.nextDouble())) {
                return seed;
            }
        }
    }

    private static boolean nullRoll(double r) {
        return r < NullPointerRoll.ENEMY_NULL_CHANCE;
    }

    private static boolean deadRoll(double r) {
        return r >= NullPointerRoll.ENEMY_NULL_CHANCE
                && r < NullPointerRoll.ENEMY_NULL_CHANCE + NullPointerRoll.DEAD_CARD_CHANCE;
    }

    private static boolean nothingRoll(double r) {
        return r >= NullPointerRoll.ENEMY_NULL_CHANCE + NullPointerRoll.DEAD_CARD_CHANCE;
    }

    private static boolean any(double r) {
        return true;
    }

    /** Début de combat : la main est distribuée (déclenche NullPointerException). */
    private void dealHand() {
        bus.publish(new GameEvent.HandDealt(state.hand()));
    }

    // --- NullPointerException : le tirage ---

    @Test
    void tirageNullPointerExceptionSeuils() {
        assertEquals(NullPointerRoll.ENEMY_NULL, NullPointerRoll.of(0.0, false));
        assertEquals(NullPointerRoll.ENEMY_NULL, NullPointerRoll.of(0.0499, false));
        assertEquals(NullPointerRoll.DEAD_CARD, NullPointerRoll.of(0.05, false));
        assertEquals(NullPointerRoll.DEAD_CARD, NullPointerRoll.of(0.1499, false));
        assertEquals(NullPointerRoll.NOTHING, NullPointerRoll.of(0.151, false));
        assertEquals(NullPointerRoll.NOTHING, NullPointerRoll.of(0.99, false));
    }

    @Test
    void jamaisNullSurLeBoss() {
        assertEquals(NullPointerRoll.NOTHING, NullPointerRoll.of(0.0, true));
        assertEquals(NullPointerRoll.NOTHING, NullPointerRoll.of(0.0499, true));
        assertEquals(NullPointerRoll.DEAD_CARD, NullPointerRoll.of(0.1, true));
    }

    @Test
    void ordreDeFaveur() {
        assertTrue(NullPointerRoll.compareFavor(NullPointerRoll.ENEMY_NULL, NullPointerRoll.NOTHING) > 0);
        assertTrue(NullPointerRoll.compareFavor(NullPointerRoll.NOTHING, NullPointerRoll.DEAD_CARD) > 0);
        assertTrue(NullPointerRoll.compareFavor(NullPointerRoll.DEAD_CARD, NullPointerRoll.ENEMY_NULL) < 0);
    }

    // --- NullPointerException : en jeu ---

    @Test
    void adversaireNullMeurtSurLeCoup() {
        setUp(2, hand(), seedWhere(SecretTrinketTest::nullRoll, SecretTrinketTest::any));
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertFalse(state.currentEnemy().isAlive());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.EnemyNulled));
        assertEquals(3, state.hand().size());

        engine.startTurn(); // la mort est résolue au début du tour
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.EnemyKilled));
        assertEquals(2, state.combatNumber());
    }

    @Test
    void pasDeNullSurLeDernierAdversaire() {
        setUp(1, hand(), seedWhere(SecretTrinketTest::nullRoll, SecretTrinketTest::any));
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertTrue(state.currentEnemy().isAlive());
        assertEquals(100, state.currentEnemy().hp(), DELTA);
        assertTrue(state.hand().stream().noneMatch(c -> c instanceof DeadCard));
    }

    @Test
    void uneCarteDevientNoCards() {
        setUp(2, hand(), seedWhere(SecretTrinketTest::deadRoll, SecretTrinketTest::any));
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertTrue(state.currentEnemy().isAlive());
        assertEquals(1, state.hand().stream().filter(c -> c instanceof DeadCard).count());
        assertEquals(3, state.hand().size());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.CardVoided v && v.dead().name().equals("NO CARDS")));
    }

    @Test
    void sinonRienNeSePasse() {
        setUp(2, hand(), seedWhere(SecretTrinketTest::nothingRoll, SecretTrinketTest::any));
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertTrue(state.currentEnemy().isAlive());
        assertTrue(state.hand().stream().noneMatch(c -> c instanceof DeadCard));
    }

    @Test
    void noCardsEstInjouable() {
        setUp(2, List.of(new DeadCard(), new SpellCard("Boule de feu", FIRE, 10, cardistry.items.Rarity.COMMON)), 1);
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.PlayCard(0)));
        assertTrue(events.contains(new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.UNPLAYABLE_CARD)));
        assertEquals(2, state.hand().size());
        assertEquals(100, state.currentEnemy().hp(), DELTA);
    }

    @Test
    void noCardsNEstPasRemplaceeParGeorge() {
        setUp(2, List.of(new DeadCard()), 1);
        engine.equip(ItemRegistry.trinket("george"));
        for (int i = 0; i < 5; i++) {
            engine.startTurn();
            engine.submit(new Action.DrawCard());
        }
        assertInstanceOf(DeadCard.class, state.hand().get(0));
        assertTrue(events.stream().noneMatch(e -> e instanceof GameEvent.CardReplaced r && r.removed() instanceof DeadCard));
    }

    @Test
    void noCardsDisparaitAuCombatSuivant() {
        bus = new EventBus();
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 1, 1), new Enemy("B", FIRE, 100, 1)),
                EPFL, List.of(), new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(new SpellCard("Boule de feu", FIRE, 10, cardistry.items.Rarity.COMMON))), new Rng(1),
                bus, new LootTable(List::of));
        engine.start();
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        // Une main avec une carte morte, puis on gagne le combat.
        int slot = 0;
        bus.publish(new GameEvent.HandDealt(state.hand())); // tirage quelconque
        engine.startTurn();
        while (state.hand().get(slot) instanceof DeadCard) {
            slot++;
        }
        engine.submit(new Action.PlayCard(slot));
        assertEquals(2, state.combatNumber());
        // Nouvelle main : 5 cartes neuves (au plus une carte morte, du tirage de ce combat).
        assertEquals(5, state.hand().size());
        assertTrue(state.hand().stream().filter(c -> c instanceof DeadCard).count() <= 1);
    }

    // --- CLAUDE.md ---

    @Test
    void claudeGardeLeTirageNullPointerExceptionFavorable() {
        // Premier tirage : carte morte ; second : rien → CLAUDE.md garde « rien ».
        long seed = seedWhere(SecretTrinketTest::deadRoll, SecretTrinketTest::nothingRoll);
        setUp(2, hand(), seed);
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertEquals(1, state.hand().stream().filter(c -> c instanceof DeadCard).count(), "sans CLAUDE.md");

        setUp(2, hand(), seed);
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        dealHand();
        assertTrue(state.hand().stream().noneMatch(c -> c instanceof DeadCard), "avec CLAUDE.md");
        assertTrue(state.currentEnemy().isAlive());
    }

    @Test
    void claudePeutRendreLAdversaireNull() {
        long seed = seedWhere(SecretTrinketTest::nothingRoll, SecretTrinketTest::nullRoll);
        setUp(2, hand(), seed);
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        dealHand();
        assertTrue(state.currentEnemy().isAlive(), "sans CLAUDE.md");

        setUp(2, hand(), seed);
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        dealHand();
        assertFalse(state.currentEnemy().isAlive(), "avec CLAUDE.md");
    }

    @Test
    void claudeCarteMorteSeulementSiLesDeuxTiragesLeSont() {
        long seed = seedWhere(SecretTrinketTest::deadRoll, SecretTrinketTest::deadRoll);
        setUp(2, hand(), seed);
        engine.equip(ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        dealHand();
        assertEquals(1, state.hand().stream().filter(c -> c instanceof DeadCard).count());
    }

    @Test
    void claudeDoubleLaChanceDeGoogz() {
        // Premier tirage raté (≥ 20 %), second réussi.
        long seed = seedWhere(r -> r >= 0.2, r -> r < 0.2);
        setUp(2, List.of(), seed);
        state.player().takeDamage(50);
        engine.equip(ItemRegistry.trinket("googz"));
        engine.submit(new Action.Smoke());
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(false)), "sans CLAUDE.md");

        events.clear();
        setUp(2, List.of(), seed);
        state.player().takeDamage(50);
        engine.equip(ItemRegistry.trinket("googz"));
        engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        engine.submit(new Action.Smoke());
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(true)), "avec CLAUDE.md");
        assertEquals(50 + 23, state.player().hp(), DELTA); // Petit pétou : soin feu 15 × 1.5 sur EPFL
    }

    @Test
    void claudeSansGoogzNeTransformeRien() {
        setUp(2, List.of(), seedWhere(r -> r < 0.2, r -> r < 0.2));
        engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        double before = state.player().hp();
        state.player().takeDamage(20);
        engine.submit(new Action.Smoke());
        assertTrue(events.contains(new GameEvent.CigaretteSmoked(false)));
        assertEquals(before - 20 + 5, state.player().hp(), DELTA);
    }

    // --- CLAUDE.md : rareté des récompenses ---

    private static Item item(String id, Rarity rarity) {
        return new Item(id, id, "", "", rarity, Set.of(), ctx -> { });
    }

    /** Première seed dont les deux premiers tirages pondérés (commun 60 / légendaire 3) donnent ces raretés. */
    private static long seedWhereRarities(boolean firstLegendary, boolean secondLegendary) {
        int total = Rarity.COMMON.dropWeight() + Rarity.LEGENDARY.dropWeight();
        for (long seed = 0; ; seed++) {
            Rng rng = new Rng(seed);
            boolean first = rng.nextInt(total) >= Rarity.COMMON.dropWeight();
            boolean second = rng.nextInt(total) >= Rarity.COMMON.dropWeight();
            if (first == firstLegendary && second == secondLegendary) {
                return seed;
            }
        }
    }

    private Collectible offerOne(long seed, boolean claude) {
        setUp(2, List.of(), seed);
        if (claude) {
            engine.equip(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD));
        }
        LootTable table = new LootTable(() -> List.of(item("commun", Rarity.COMMON), item("legendaire", Rarity.LEGENDARY)));
        return table.offer(state, new Rng(seed), 1).get(0);
    }

    @Test
    void claudeGardeLaRareteLaPlusHaute() {
        long seed = seedWhereRarities(false, true);
        assertEquals("commun", offerOne(seed, false).id());
        assertEquals("legendaire", offerOne(seed, true).id());
    }

    @Test
    void claudeNeBaissePasLaRarete() {
        long seed = seedWhereRarities(true, false);
        assertEquals("legendaire", offerOne(seed, false).id());
        assertEquals("legendaire", offerOne(seed, true).id());
    }

    @Test
    void claudeSurLaRareteStatistiquement() {
        int total = Rarity.COMMON.dropWeight() + Rarity.LEGENDARY.dropWeight();
        double single = (double) Rarity.LEGENDARY.dropWeight() / total;
        int legendary = 0;
        int trials = 4000;
        for (long seed = 0; seed < trials; seed++) {
            if (offerOne(seed, true).rarity() == Rarity.LEGENDARY) {
                legendary++;
            }
        }
        double expected = 1 - (1 - single) * (1 - single);
        double rate = (double) legendary / trials;
        assertTrue(Math.abs(rate - expected) < 0.02, "taux " + rate + ", attendu " + expected);
    }

    // --- Secrets dans le pool ---

    @Test
    void secretVerrouilleJamaisTire() {
        Trinket secret = ItemRegistry.trinket(ItemRegistry.CTRL_Z);
        LootTable table = new LootTable(() -> List.of(secret));
        assertEquals(0, table.weight(secret));
        setUp(2, List.of(), 1);
        assertTrue(table.offer(state, new Rng(1), 3).isEmpty());
    }

    @Test
    void secretDebloquePeseDeux() {
        Trinket secret = ItemRegistry.trinket(ItemRegistry.CTRL_Z);
        LootTable table = new LootTable(() -> List.of(secret, item("commun", Rarity.COMMON)), Set.of(ItemRegistry.CTRL_Z));
        assertEquals(LootTable.UNLOCKED_SECRET_WEIGHT, table.weight(secret));
        assertEquals(2, LootTable.UNLOCKED_SECRET_WEIGHT);
        setUp(2, List.of(), 1);
        assertEquals(2, table.offer(state, new Rng(1), 3).size());
    }

    @Test
    void secretDebloqueTireAvecSonPoids() {
        Trinket secret = ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION);
        LootTable table = new LootTable(() -> List.of(secret, item("commun", Rarity.COMMON)),
                Set.of(ItemRegistry.NULL_POINTER_EXCEPTION));
        setUp(2, List.of(), 1);
        int hits = 0;
        int trials = 31_000;
        Rng rng = new Rng(9);
        for (int i = 0; i < trials; i++) {
            if (table.offer(state, rng, 1).get(0) == secret) {
                hits++;
            }
        }
        double rate = (double) hits / trials;
        assertTrue(Math.abs(rate - 2.0 / 62) < 0.005, "taux " + rate); // 2 / (2 + 60)
    }

    @Test
    void secretDejaEquipeNonPropose() {
        Trinket secret = ItemRegistry.trinket(ItemRegistry.CLAUDE_MD);
        LootTable table = new LootTable(() -> List.of(ItemRegistry.trinket(ItemRegistry.CLAUDE_MD)),
                Set.of(ItemRegistry.CLAUDE_MD));
        setUp(2, List.of(), 1);
        engine.equip(secret);
        assertTrue(table.offer(state, new Rng(1), 3).isEmpty());
    }

    @Test
    void lesTroisSecretsSontDansLeRegistre() {
        for (String id : List.of(ItemRegistry.NULL_POINTER_EXCEPTION, ItemRegistry.CTRL_Z, ItemRegistry.CLAUDE_MD)) {
            Trinket trinket = ItemRegistry.trinket(id);
            assertEquals(Rarity.SECRET, trinket.rarity());
            assertTrue(trinket.tags().contains(Tag.META));
        }
        assertEquals("NullPointerException", ItemRegistry.trinket(ItemRegistry.NULL_POINTER_EXCEPTION).name());
        assertEquals("Ctrl+Z", ItemRegistry.trinket(ItemRegistry.CTRL_Z).name());
        assertEquals("CLAUDE.md", ItemRegistry.trinket(ItemRegistry.CLAUDE_MD).name());
    }
}

package cardistry.core;

import static cardistry.core.stats.StatModifier.flat;
import static cardistry.model.Element.ALGEBRAIC;
import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import cardistry.model.enemy.MoveEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Les 6 stats en combat : formule branchée dans le moteur (sorts, coups, soins, statuts). */
class StatsCombatTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /**
     * Terrain Forêt (plante, sans bonus) ; adversaire de 100 PV de l'élément {@code element}, avec
     * {@code stats} (FOR, INT, ESP, DEF, RES, CHA), qui frappe de {@code hit} à chaque tour.
     */
    private void setUp(Element element, int[] stats, double hit, List<Card> hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = Enemy.builder("cible", "Cible", element, EnemyRank.COMMON).hp(100)
                .stats(stats[0], stats[1], stats[2], stats[3], stats[4], stats[5])
                .behavior(EnemyBehavior.always(EnemyMove.of("coup", "Coup", "Coup", MoveEffect.Hit.of(hit))))
                .build();
        state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Forêt", NATURE, Rarity.COMMON),
                new ArrayList<>(hand), new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(new TerrainCard("Pioche", FIRE, Rarity.COMMON))),
                new Rng(3), bus, new LootTable(List::of));
    }

    private void player(StatType stat, double value) {
        state.playerStats().add(flat(stat, value, "test"));
    }

    private void use(Consumer<EffectContext> effect) {
        engine.give(new Item("test", "test", "x", "", Rarity.COMMON, Set.of(), effect));
        engine.submit(new Action.UseItem(state.items().size() - 1));
    }

    private static int[] stats(int... values) {
        return values;
    }

    private static final int[] NONE = {0, 0, 0, 0, 0, 0};

    // --- Sorts ---

    @Test
    void sortAvecForceEtDefDeLAdversaire() {
        // (10 + 3) × 1,5 (feu bat plante) − 2 = 17,5 → 18
        setUp(NATURE, stats(0, 0, 0, 2, 0, 0), 0, List.of(new SpellCard("Feu", FIRE, 10, Rarity.COMMON)));
        player(StatType.STRENGTH, 3);
        assertEquals(18, state.spellDamage((SpellCard) state.hand().get(0)));
        engine.submit(new Action.PlayCard(0));
        assertEquals(82, enemy.hp());
    }

    @Test
    void sortAlgebriqueAvecIntelligence() {
        setUp(NEUTRAL, NONE, 0, List.of(new SpellCard("Algèbre", ALGEBRAIC, 10, Rarity.COMMON)));
        player(StatType.INTELLIGENCE, 2);
        player(StatType.STRENGTH, 5); // ne compte pas pour un sort algébrique
        engine.submit(new Action.PlayCard(0));
        assertEquals(88, enemy.hp());
    }

    @Test
    void sortNonMultiplieIgnoreStatsEtDef() {
        setUp(NEUTRAL, stats(0, 0, 0, 5, 0, 0), 0, List.of(CardPool.professeurDOr()));
        player(StatType.MIND, 4);
        player(StatType.LUCK, 40);
        engine.submit(new Action.PlayCard(0));
        assertEquals(100 - CardPool.PROFESSEUR_D_OR_DAMAGE, enemy.hp());
        assertFalse(events.stream().anyMatch(e -> e instanceof GameEvent.CriticalHit));
    }

    @Test
    void unSortAZeroDegatResteAZeroMalgreLaForce() {
        setUp(NEUTRAL, NONE, 0, List.of(new SpellCard("Rien", NEUTRAL, 0, Rarity.COMMON)));
        player(StatType.STRENGTH, 5);
        engine.submit(new Action.PlayCard(0));
        assertEquals(100, enemy.hp());
    }

    @Test
    void coupCritiqueDuJoueur() {
        // CHA 34 : 102 % → critique sûr. 10 × 1,5 = 15
        setUp(NEUTRAL, NONE, 0, List.of(new SpellCard("Coup", NEUTRAL, 10, Rarity.COMMON)));
        player(StatType.LUCK, 34);
        engine.submit(new Action.PlayCard(0));
        assertEquals(85, enemy.hp());
        assertTrue(events.contains(new GameEvent.CriticalHit(enemy)));
    }

    @Test
    void sansChanceAucunCritique() {
        setUp(NEUTRAL, NONE, 0, List.of(new SpellCard("Coup", NEUTRAL, 10, Rarity.COMMON)));
        engine.submit(new Action.PlayCard(0));
        assertEquals(90, enemy.hp());
        assertFalse(events.stream().anyMatch(e -> e instanceof GameEvent.CriticalHit));
    }

    @Test
    void degatsElementairesDUnObjetSubissentLaDefSansStatOffensive() {
        setUp(NEUTRAL, stats(0, 0, 0, 3, 0, 0), 0, List.of());
        player(StatType.STRENGTH, 5);
        use(ctx -> ctx.elementalDamage(10, NEUTRAL));
        assertEquals(93, enemy.hp());
    }

    @Test
    void degatsBrutsIgnorentLaDef() {
        setUp(NEUTRAL, stats(0, 0, 0, 3, 0, 0), 0, List.of());
        use(ctx -> ctx.damageEnemy(10));
        assertEquals(90, enemy.hp());
    }

    // --- Coups adverses ---

    @Test
    void coupAdverseAvecSaForceEtTaDef() {
        // Adversaire neutre : terrain sans effet. 5 + 2 − 1 = 6
        setUp(NEUTRAL, stats(2, 0, 0, 0, 0, 0), 5, List.of());
        player(StatType.DEFENSE, 1);
        assertEquals(6, state.enemyAttackDamage(), 1e-9);
        engine.submit(new Action.RollCigarette());
        assertEquals(94, state.player().hp());
    }

    @Test
    void coupCritiqueDeLAdversaire() {
        setUp(NEUTRAL, stats(0, 0, 0, 0, 0, 40), 10, List.of());
        engine.submit(new Action.RollCigarette());
        assertEquals(85, state.player().hp());
        assertTrue(events.contains(new GameEvent.CriticalHit(state.player())));
    }

    // --- Soins ---

    @Test
    void carteDeSoinAvecEsprit() {
        // Soin neutre sur la Forêt : ×1. 10 + 2 = 12
        setUp(NEUTRAL, NONE, 0, List.of(new HealCard("Soin", NEUTRAL, 10, Rarity.COMMON)));
        state.player().takeDamage(50);
        player(StatType.MIND, 2);
        assertEquals(12, state.healAmount((HealCard) state.hand().get(0)), 1e-9);
        engine.submit(new Action.PlayCard(0));
        assertEquals(62, state.player().hp());
    }

    @Test
    void objetDeSoinAvecEspritMaisPasLesAutresSoins() {
        setUp(NEUTRAL, NONE, 0, List.of());
        state.player().takeDamage(50);
        player(StatType.MIND, 3);
        use(ctx -> ctx.itemHeal(10));
        assertEquals(63, state.player().hp());
        use(ctx -> ctx.healPlayer(10)); // soin d'un trinket, de Dave… : sans ESP
        assertEquals(73, state.player().hp());
    }

    @Test
    void soinDeLAdversaireAvecSonEsprit() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = Enemy.builder("cible", "Cible", NEUTRAL, EnemyRank.COMMON).hp(100).stats(0, 0, 2, 0, 0, 0)
                .behavior(EnemyBehavior.always(EnemyMove.of("soin", "Soin", "Soin", new MoveEffect.HealSelf(12))))
                .build();
        state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Forêt", NATURE, Rarity.COMMON),
                new ArrayList<>(), new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(new TerrainCard("Pioche", FIRE, Rarity.COMMON))),
                new Rng(3), bus, new LootTable(List::of));
        enemy.takeDamage(50);
        engine.submit(new Action.RollCigarette());
        assertEquals(64, enemy.hp());
    }

    // --- Statuts ---

    @Test
    void laResistanceBloqueMemeUnStatutACoupSur() {
        // 100 % − 20 × 5 % = 0
        setUp(NEUTRAL, stats(0, 0, 0, 0, 20, 0), 0, List.of());
        use(ctx -> assertFalse(ctx.applyStatus(StatusTarget.ENEMY, StatusType.INTOX, 1)));
        assertFalse(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
    }

    @Test
    void lIntelligenceRendUnStatutSurEtCinqResRaccourcitLaDuree() {
        // 0 % + 34 × 3 % − 5 × 5 % > 100 % ; RES 5 : 3 tours → 2
        setUp(NEUTRAL, stats(0, 0, 0, 0, 5, 0), 0, List.of());
        player(StatType.INTELLIGENCE, 34);
        use(ctx -> assertTrue(ctx.applyStatus(StatusTarget.ENEMY, StatusType.HYPNOTIZED, 0, 3)));
        assertEquals(2, state.statuses(StatusTarget.ENEMY).get(StatusType.HYPNOTIZED).orElseThrow().turnsLeft());
    }

    @Test
    void unStatutQueTuTePosesToiMemeIgnoreTaResistance() {
        setUp(NEUTRAL, NONE, 0, List.of());
        player(StatType.RESISTANCE, 20);
        use(ctx -> assertTrue(ctx.applyStatus(StatusTarget.PLAYER, StatusType.FONS, 1, 2)));
        assertEquals(2, state.statuses(StatusTarget.PLAYER).get(StatusType.FONS).orElseThrow().turnsLeft());
    }

    @Test
    void taResistanceProtegeDesStatutsAdverses() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = Enemy.builder("cible", "Cible", NEUTRAL, EnemyRank.COMMON).hp(100)
                .behavior(EnemyBehavior.always(EnemyMove.of("gaz", "Gaz", "Gaz",
                        new MoveEffect.Inflict("intox", 1.0, 2, false))))
                .build();
        state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Forêt", NATURE, Rarity.COMMON),
                new ArrayList<>(), new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(new TerrainCard("Pioche", FIRE, Rarity.COMMON))),
                new Rng(3), bus, new LootTable(List::of));
        player(StatType.RESISTANCE, 20);
        engine.submit(new Action.RollCigarette());
        assertFalse(state.statuses(StatusTarget.PLAYER).has(StatusType.INTOX));
    }
}

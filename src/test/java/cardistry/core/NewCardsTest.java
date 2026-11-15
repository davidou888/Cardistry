package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Nouvelles cartes du plan V2 (§8), lot par lot. */
class NewCardsTest {

    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /** Adversaire neutre de {@code hp} PV, terrain neutre sans effet, main = {@code hand}. */
    private void setUp(double hp, long seed, Stats stats, Card... hand) {
        enemy = new Enemy("Cible", Element.NEUTRAL, hp, 0);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", Element.NEUTRAL, 10, 0)),
                new TerrainCard("Neutre", Element.NEUTRAL, Rarity.COMMON), List.of(hand), stats);
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), new EventBus(), new LootTable(List::of));
        engine.startTurn();
    }

    private void setUp(double hp, Card... hand) {
        setUp(hp, 1, new Stats(), hand);
    }

    private void play() {
        engine.submit(new Action.PlayCard(0));
    }

    private static Card card(String name) {
        return new CardPool().cards().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow().copy();
    }

    // --- Lot 1 ---

    @Test
    void cayouuuEtPainPita() {
        setUp(100, card("CAYOUUU"), card("Pain pita"));
        play();
        assertEquals(88, enemy.hp());
        engine.startTurn();
        play();
        assertEquals(78, enemy.hp());
        assertEquals(Element.GEOMETRIC, card("Pain pita").element());
        assertEquals(Rarity.COMMON, card("CAYOUUU").rarity());
        assertEquals(Rarity.COMMON, card("Pain pita").rarity());
    }

    @Test
    void matrixDoubleSiLesPvFinissentParZeroOuUnAvantLeCoup() {
        for (double hp : List.of(50.0, 31.0, 100.0, 1001.0)) {
            setUp(hp, CardPool.matrix());
            play();
            assertEquals(hp - 20, enemy.hp(), "PV " + hp);
        }
        for (double hp : List.of(42.0, 39.0, 55.0)) {
            setUp(hp, CardPool.matrix());
            play();
            assertEquals(hp - 10, enemy.hp(), "PV " + hp);
        }
        setUp(30, CardPool.matrix()); // 30 → 10 : vérifié avant le coup, pas après
        play();
        assertEquals(10, enemy.hp());
        assertEquals(Element.ALGEBRAIC, CardPool.matrix().element());
        assertEquals(Rarity.COMMON, CardPool.matrix().rarity());
    }

    @Test
    void moindreCarreDegatsDeUnAQuaranteEnTaFaveur() {
        int min = Integer.MAX_VALUE;
        int max = 0;
        double sum = 0;
        double sumLucky = 0;
        int runs = 600;
        for (long seed = 0; seed < runs; seed++) {
            setUp(1000, seed, new Stats(), CardPool.moindreCarre());
            play();
            int dealt = 1000 - enemy.hp();
            min = Math.min(min, dealt);
            max = Math.max(max, dealt);
            sum += dealt;
            Stats lucky = new Stats();
            lucky.add(StatModifier.flat(StatType.LUCKY_ROLLS, 1, "claude"));
            setUp(1000, seed, lucky, CardPool.moindreCarre());
            play();
            sumLucky += 1000 - enemy.hp();
        }
        assertEquals(1, min);
        assertEquals(40, max);
        assertEquals(20.5, sum / runs, 1.5);
        assertEquals(27.4, sumLucky / runs, 1.5, "CLAUDE.md : le meilleur de deux tirages");
        assertEquals(Rarity.UNCOMMON, CardPool.moindreCarre().rarity());
    }

    @Test
    void distPlayerDixPlusLesEtagesParcourus() {
        setUp(100, CardPool.distPlayer());
        engine.enterNode(1, "Combat");
        engine.enterNode(2, "Combat");
        engine.enterNode(3, "Combat");
        play();
        assertEquals(100 - 13, enemy.hp());
        assertEquals(Element.GEOMETRIC, CardPool.distPlayer().element());
        assertEquals(Rarity.UNCOMMON, CardPool.distPlayer().rarity());
    }

    @Test
    void lesDegatsCalculesRestentMultiplies() {
        Stats stats = new Stats();
        stats.add(StatModifier.percent(StatType.ALGEBRAIC_DAMAGE, 0.5, "test"));
        setUp(42, 1, stats, CardPool.matrix());
        play();
        assertEquals(42 - 15, enemy.hp());
    }
}

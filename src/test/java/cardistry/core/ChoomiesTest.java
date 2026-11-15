package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.LootTable;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import cardistry.model.enemy.EnemyRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Choomies gagnés en battant un adversaire, selon son rang. */
class ChoomiesTest {

    /** Choomies gagnés en tuant {@code enemy} d'un sort (seed {@code seed}). */
    private static int gain(Enemy enemy, long seed, Stats stats) {
        GameState state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 10, 1)),
                new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), List.of(new SpellCard("Gros", FIRE, 1000, cardistry.items.Rarity.COMMON)), stats);
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(seed), new EventBus(),
                new LootTable(List::of));
        engine.submit(new Action.PlayCard(0));
        return state.choomies() - GameState.STARTING_CHOOMIES;
    }

    private static Enemy ranked(EnemyRank rank) {
        return Enemy.builder("x", "X", FIRE, rank).hp(10)
                .behavior(EnemyBehavior.always(EnemyMove.basicAttack(1))).build();
    }

    @Test
    void depart() {
        assertEquals(15, GameState.STARTING_CHOOMIES);
        assertEquals(15, GameState.newGame().choomies());
    }

    @Test
    void gainsSelonLeRang() {
        for (long seed = 0; seed < 200; seed++) {
            int common = gain(ranked(EnemyRank.COMMON), seed, new Stats());
            int elite = gain(ranked(EnemyRank.ELITE), seed, new Stats());
            int boss = gain(ranked(EnemyRank.BOSS), seed, new Stats());
            assertTrue(common >= 12 && common <= 18, "commun " + common);
            assertTrue(elite >= 30 && elite <= 40, "élite " + elite);
            assertTrue(boss >= 80 && boss <= 100, "boss " + boss);
        }
    }

    @Test
    void clarenceDonneSaValeur() {
        assertEquals(75, gain(EnemyRegistry.create(EnemyRegistry.CLARENCE, 1), 3, new Stats()));
    }

    @Test
    void statDeGainMultiplie() {
        Stats stats = new Stats();
        stats.add(StatModifier.percent(StatType.CHOOMIES_GAIN, 0.5, "test"));
        assertEquals(113, gain(EnemyRegistry.create(EnemyRegistry.CLARENCE, 1), 3, stats)); // 75 × 1.5 arrondi
    }
}

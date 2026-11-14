package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.TerrainCard;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CLAUDE.md : les tirages de chance en faveur du joueur sont faits deux fois, on garde le meilleur. */
class LuckTest {

    private static GameState state(boolean lucky) {
        Stats stats = new Stats();
        if (lucky) {
            stats.add(StatModifier.flat(StatType.LUCKY_ROLLS, 1, "test"));
        }
        return new GameState(new Player(100), List.of(new Enemy("A", Element.FIRE, 10, 1)),
                new TerrainCard("EPFL", Element.FIRE), List.of(), stats);
    }

    @Test
    void sansClaudeUnSeulTirage() {
        for (long seed = 0; seed < 200; seed++) {
            Rng rng = new Rng(seed);
            Rng reference = new Rng(seed);
            double kept = Luck.favorable(state(false), rng::nextDouble, Comparator.naturalOrder());
            assertEquals(reference.nextDouble(), kept);
            assertEquals(reference.nextDouble(), rng.nextDouble(), "un seul tirage consommé");
        }
    }

    @Test
    void avecClaudeDeuxTiragesEtLeMeilleur() {
        for (long seed = 0; seed < 200; seed++) {
            Rng rng = new Rng(seed);
            Rng reference = new Rng(seed);
            double kept = Luck.favorable(state(true), rng::nextDouble, Comparator.naturalOrder());
            assertEquals(Math.max(reference.nextDouble(), reference.nextDouble()), kept);
        }
    }

    @Test
    void egaliteGardeLePremier() {
        int[] calls = {0};
        String kept = Luck.favorable(state(true), () -> "tirage " + calls[0]++, (a, b) -> 0);
        assertEquals("tirage 0", kept);
        assertEquals(2, calls[0]);
    }

    @Test
    void chanceAvecClaudeReussitSiUnDesDeuxTiragesReussit() {
        for (long seed = 0; seed < 500; seed++) {
            Rng reference = new Rng(seed);
            boolean first = reference.nextDouble() < 0.2;
            boolean second = reference.nextDouble() < 0.2;
            assertEquals(first, Luck.chance(state(false), new Rng(seed), 0.2));
            assertEquals(first || second, Luck.chance(state(true), new Rng(seed), 0.2));
        }
    }

    @Test
    void chanceAvecClaudeSurBeaucoupDeTirages() {
        Rng rng = new Rng(42);
        int hits = 0;
        int trials = 20_000;
        for (int i = 0; i < trials; i++) {
            if (Luck.chance(state(true), rng, 0.2)) {
                hits++;
            }
        }
        // 1 − 0,8² = 36 %
        double rate = (double) hits / trials;
        assertTrue(rate > 0.34 && rate < 0.38, "taux " + rate);
        assertFalse(state(false).luckyRolls());
        assertTrue(state(true).luckyRolls());
    }
}

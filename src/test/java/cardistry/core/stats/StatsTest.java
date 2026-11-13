package cardistry.core.stats;

import static cardistry.core.stats.StatModifier.flat;
import static cardistry.core.stats.StatModifier.percent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatsTest {

    private static final double DELTA = 1e-9;

    private final Stats stats = new Stats();

    @ParameterizedTest
    @EnumSource(StatType.class)
    void sansModificateurOnObtientLaBase(StatType stat) {
        assertEquals(stat.defaultBase(), stats.get(stat), DELTA);
    }

    @Test
    void valeursDeBase() {
        assertEquals(100, stats.get(StatType.MAX_HP), DELTA);
        assertEquals(7, stats.getInt(StatType.HAND_SIZE));
        assertEquals(1.0, stats.get(StatType.FIRE_DAMAGE), DELTA);
        assertEquals(1.0, stats.get(StatType.HEAL_EFFICIENCY), DELTA);
    }

    @Test
    void bonusPlatsSAdditionnent() {
        stats.add(flat(StatType.HAND_SIZE, 1, "a"));
        stats.add(flat(StatType.HAND_SIZE, 2, "b"));
        assertEquals(10, stats.get(StatType.HAND_SIZE), DELTA);
    }

    @Test
    void pourcentagesSAdditionnent() {
        stats.add(percent(StatType.FIRE_DAMAGE, 0.25, "briquet"));
        stats.add(percent(StatType.FIRE_DAMAGE, 0.25, "briquet2"));
        assertEquals(1.5, stats.get(StatType.FIRE_DAMAGE), DELTA);
    }

    @Test
    void pourcentagesNegatifsEtPlancherAZero() {
        stats.add(percent(StatType.FIRE_DAMAGE, 0.5, "a"));
        stats.add(percent(StatType.FIRE_DAMAGE, -0.2, "b"));
        assertEquals(1.3, stats.get(StatType.FIRE_DAMAGE), DELTA);

        stats.add(percent(StatType.FIRE_DAMAGE, -5, "c"));
        assertEquals(0, stats.get(StatType.FIRE_DAMAGE), DELTA);
    }

    @Test
    void ordreBasePuisPlatsPuisMultiplicateurs() {
        // L'ordre d'ajout ne compte pas : (100 + 20) × 1.5, jamais 100 × 1.5 + 20.
        stats.add(percent(StatType.MAX_HP, 0.5, "a"));
        stats.add(flat(StatType.MAX_HP, 20, "b"));
        assertEquals(180, stats.get(StatType.MAX_HP), DELTA);
    }

    @Test
    void unModificateurNAffecteQueSaStat() {
        stats.add(percent(StatType.FIRE_DAMAGE, 1, "a"));
        assertEquals(1.0, stats.get(StatType.WATER_DAMAGE), DELTA);
    }

    @Test
    void retirerParSource() {
        stats.add(flat(StatType.HAND_SIZE, 2, "sac"));
        stats.add(percent(StatType.FIRE_DAMAGE, 0.25, "sac"));
        stats.add(flat(StatType.HAND_SIZE, 1, "autre"));

        stats.removeBySource("sac");

        assertEquals(8, stats.get(StatType.HAND_SIZE), DELTA);
        assertEquals(1.0, stats.get(StatType.FIRE_DAMAGE), DELTA);
        assertEquals(1, stats.modifiers().size());
    }

    @Test
    void laBaseEstModifiableEtLesModificateursSAppliquentDessus() {
        stats.setBase(StatType.MAX_HP, 80);
        stats.add(flat(StatType.MAX_HP, 10, "a"));
        assertEquals(80, stats.base(StatType.MAX_HP), DELTA);
        assertEquals(90, stats.get(StatType.MAX_HP), DELTA);
    }

    @Test
    void getIntArrondiALEntierInferieur() {
        stats.add(percent(StatType.HAND_SIZE, 0.1, "a")); // 7.7
        assertEquals(7, stats.getInt(StatType.HAND_SIZE));
    }
}

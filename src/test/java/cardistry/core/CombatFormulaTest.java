package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Element;
import org.junit.jupiter.api.Test;

class CombatFormulaTest {

    @Test
    void exempleDuPlan() {
        // Boule de feu (10), FOR 3, ×1,5 contre plante, DEF 2 : (10 + 3) × 1,5 − 2 = 17,5 → 18.
        assertEquals(18, CombatFormula.damage(10, 3, 1.5, false, 2, 0));
    }

    @Test
    void critiqueMultiplieAvantLaDef() {
        // (10 + 0) × 1 × 1,5 − 2 = 13
        assertEquals(13, CombatFormula.damage(10, 0, 1.0, true, 2, 0));
    }

    @Test
    void minimumUnSiLeCoupADesDegats() {
        assertEquals(1, CombatFormula.damage(2, 0, 1.0, false, 10, 0));
    }

    @Test
    void unSortAZeroResteAZero() {
        assertEquals(0, CombatFormula.damage(0, 5, 1.5, true, 0, 0));
    }

    @Test
    void defIgnoree() {
        assertEquals(12, CombatFormula.damage(12, 0, 1.0, false, 4, CombatFormula.ALL_DEFENSE));
        // CAYOUUU : ignore 2 points
        assertEquals(10, CombatFormula.damage(12, 0, 1.0, false, 4, 2));
        assertEquals(12, CombatFormula.damage(12, 0, 1.0, false, 1, 2));
    }

    @Test
    void defNegativeAjouteDesDegatsEtNEstJamaisIgnoree() {
        assertEquals(12, CombatFormula.damage(10, 0, 1.0, false, -2, 0));
        assertEquals(12, CombatFormula.damage(10, 0, 1.0, false, -2, CombatFormula.ALL_DEFENSE));
    }

    @Test
    void statOffensiveSelonLElement() {
        assertEquals(StatType.STRENGTH, CombatFormula.offensiveStat(Element.FIRE));
        assertEquals(StatType.STRENGTH, CombatFormula.offensiveStat(Element.WATER));
        assertEquals(StatType.STRENGTH, CombatFormula.offensiveStat(Element.NATURE));
        assertEquals(StatType.STRENGTH, CombatFormula.offensiveStat(Element.NEUTRAL));
        assertEquals(StatType.INTELLIGENCE, CombatFormula.offensiveStat(Element.ALGEBRAIC));
        assertEquals(StatType.INTELLIGENCE, CombatFormula.offensiveStat(Element.GEOMETRIC));
        assertEquals(StatType.MIND, CombatFormula.offensiveStat(Element.SPIRIT));
        assertNull(CombatFormula.offensiveStat(Element.GLITCH));
    }

    @Test
    void glitchPrendLaMeilleureStatOffensive() {
        Stats stats = new Stats();
        stats.add(StatModifier.flat(StatType.STRENGTH, 1, "test"));
        stats.add(StatModifier.flat(StatType.INTELLIGENCE, 4, "test"));
        stats.add(StatModifier.flat(StatType.MIND, 2, "test"));
        assertEquals(4, CombatFormula.offense(Element.GLITCH, stats));
        assertEquals(1, CombatFormula.offense(Element.FIRE, stats));
        assertEquals(2, CombatFormula.offense(Element.SPIRIT, stats));
    }

    @Test
    void soinAvecEsprit() {
        // (10 + 2) × 1,5 = 18
        assertEquals(18, CombatFormula.heal(10, 2, 1.5), 1e-9);
        assertEquals(0, CombatFormula.heal(0, 5, 1.0), 1e-9);
    }

    @Test
    void chanceDeCritique() {
        assertEquals(0.09, CombatFormula.critChance(3), 1e-9);
        assertEquals(0, CombatFormula.critChance(-2), 1e-9);
    }

    @Test
    void chanceDeStatut() {
        // 10 % + 3 × 3 % − 1 × 5 % = 14 %
        assertEquals(0.14, CombatFormula.statusChance(0.10, 3, 1), 1e-9);
        // Pet de Mamouz (100 %) contre RES 4 : 80 %
        assertEquals(0.80, CombatFormula.statusChance(1.0, 0, 4), 1e-9);
        assertEquals(1.0, CombatFormula.statusChance(1.0, 4, 0), 1e-9);
        assertEquals(0.0, CombatFormula.statusChance(0.10, 0, 5), 1e-9);
    }

    @Test
    void dureeDesStatutsAvecBeaucoupDeRes() {
        assertEquals(3, CombatFormula.statusTurns(3, 4));
        assertEquals(2, CombatFormula.statusTurns(3, 5));
        assertEquals(1, CombatFormula.statusTurns(1, 7));
    }
}

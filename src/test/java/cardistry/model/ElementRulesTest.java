package cardistry.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tables complètes : toute modification d'un multiplicateur doit casser un de ces tests. */
class ElementRulesTest {

    @ParameterizedTest(name = "sort {0} contre adversaire {1} → ×{2}")
    @CsvSource({
        "FIRE,   FIRE,   1.0",
        "FIRE,   WATER,  0.5",
        "FIRE,   NATURE, 1.5",
        "WATER,  FIRE,   1.5",
        "WATER,  WATER,  1.0",
        "WATER,  NATURE, 0.5",
        "NATURE, FIRE,   0.5",
        "NATURE, WATER,  1.5",
        "NATURE, NATURE, 1.0",
    })
    void spellMultiplier(Element spell, Element enemy, double expected) {
        assertEquals(expected, ElementRules.spellMultiplier(spell, enemy));
    }

    @ParameterizedTest(name = "soin {0} sur terrain {1} → ×{2}")
    @CsvSource({
        "FIRE,   FIRE,   1.5",
        "FIRE,   WATER,  0.75",
        "FIRE,   NATURE, 1.0",
        "WATER,  FIRE,   1.0",
        "WATER,  WATER,  1.5",
        "WATER,  NATURE, 0.75",
        "NATURE, FIRE,   0.75",
        "NATURE, WATER,  1.0",
        "NATURE, NATURE, 1.5",
    })
    void healMultiplier(Element heal, Element terrain, double expected) {
        assertEquals(expected, ElementRules.healMultiplier(heal, terrain));
    }

    @ParameterizedTest(name = "terrain {0}, adversaire {1} → attaque ×{2}")
    @CsvSource({
        "FIRE,   FIRE,   1.25",
        "FIRE,   WATER,  1.0",
        "FIRE,   NATURE, 0.75",
        "WATER,  FIRE,   0.75",
        "WATER,  WATER,  1.25",
        "WATER,  NATURE, 1.0",
        "NATURE, FIRE,   1.0",
        "NATURE, WATER,  0.75",
        "NATURE, NATURE, 1.25",
    })
    void terrainAttackMultiplier(Element terrain, Element enemy, double expected) {
        assertEquals(expected, ElementRules.terrainAttackMultiplier(terrain, enemy));
    }
}

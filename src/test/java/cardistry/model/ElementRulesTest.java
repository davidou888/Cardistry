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
        "FIRE,   NEUTRAL, 1.0",
        "WATER,  NEUTRAL, 1.0",
        "NATURE, NEUTRAL, 1.0",
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
        "FIRE,   NEUTRAL, 1.0",
        "WATER,  NEUTRAL, 1.0",
        "NATURE, NEUTRAL, 1.0",
    })
    void terrainAttackMultiplier(Element terrain, Element enemy, double expected) {
        assertEquals(expected, ElementRules.terrainAttackMultiplier(terrain, enemy));
    }

    @org.junit.jupiter.api.Test
    void neutreNeBatRienEtNEstBattuQueParGlitch() {
        for (Element element : Element.values()) {
            org.junit.jupiter.api.Assertions.assertFalse(Element.NEUTRAL.beats(element));
            org.junit.jupiter.api.Assertions.assertEquals(element == Element.GLITCH, element.beats(Element.NEUTRAL),
                    element + " bat neutre ?");
        }
        assertEquals(java.util.List.of(Element.FIRE, Element.WATER, Element.NATURE, Element.ALGEBRAIC,
                Element.SPIRIT, Element.GEOMETRIC, Element.GLITCH), Element.PLAYABLE);
        assertEquals("plante", Element.NATURE.displayName());
    }

    @ParameterizedTest
    @CsvSource({
        // GLITCH bat tout, lui-même compris
        "GLITCH, FIRE,    1.5",
        "GLITCH, NEUTRAL, 1.5",
        "GLITCH, GLITCH,  1.5",
        "FIRE,   GLITCH,  0.5",
        "SPIRIT, GLITCH,  0.5",
        // second triangle
        "GEOMETRIC, ALGEBRAIC, 1.5",
        "ALGEBRAIC, SPIRIT,    1.5",
        "SPIRIT,    GEOMETRIC, 1.5",
        "ALGEBRAIC, GEOMETRIC, 0.5",
        "FIRE,      SPIRIT,    1.0",
    })
    void spellMultiplierNouveauxElements(Element spell, Element enemy, double expected) {
        assertEquals(expected, ElementRules.spellMultiplier(spell, enemy));
    }

    @ParameterizedTest
    @CsvSource({
        "GLITCH, FIRE,   0.75",
        "GLITCH, GLITCH, 1.25",
        "FIRE,   GLITCH, 1.0",
        "GEOMETRIC, ALGEBRAIC, 0.75",
    })
    void terrainAttackMultiplierNouveauxElements(Element terrain, Element enemy, double expected) {
        assertEquals(expected, ElementRules.terrainAttackMultiplier(terrain, enemy));
    }
}

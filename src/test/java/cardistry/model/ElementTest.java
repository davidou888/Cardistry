package cardistry.model;

import static cardistry.model.Element.ALGEBRAIC;
import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.GEOMETRIC;
import static cardistry.model.Element.GLITCH;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.SPIRIT;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ElementTest {

    @Test
    void cycleFeuNatureEau() {
        assertTrue(FIRE.beats(NATURE));
        assertTrue(NATURE.beats(WATER));
        assertTrue(WATER.beats(FIRE));
    }

    @Test
    void cycleGeometriqueAlgebriqueSpirituel() {
        assertTrue(GEOMETRIC.beats(ALGEBRAIC));
        assertTrue(ALGEBRAIC.beats(SPIRIT));
        assertTrue(SPIRIT.beats(GEOMETRIC));
    }

    /**
     * Hors GLITCH, chaque élément jouable bat exactement un autre élément et est battu par exactement
     * un autre élément de son triangle, plus par GLITCH.
     */
    @ParameterizedTest
    @EnumSource(value = Element.class, names = {"FIRE", "WATER", "NATURE", "ALGEBRAIC", "SPIRIT", "GEOMETRIC"})
    void chaqueElementBatUnAutreEtEstBattuParUnAutreEtParGlitch(Element element) {
        assertFalse(element.beats(element));
        assertEquals(1, Element.PLAYABLE.stream().filter(element::beats).count());
        assertEquals(2, Element.PLAYABLE.stream().filter(other -> other.beats(element)).count());
        assertTrue(GLITCH.beats(element));
        assertFalse(element.beats(GLITCH));
    }

    @Test
    void glitchBatTout() {
        for (Element element : Element.values()) {
            assertTrue(GLITCH.beats(element), "GLITCH bat " + element);
        }
    }

    @Test
    void lesDeuxTrianglesNeSeCroisentPas() {
        for (Element a : java.util.List.of(FIRE, WATER, NATURE)) {
            for (Element b : java.util.List.of(ALGEBRAIC, SPIRIT, GEOMETRIC)) {
                assertFalse(a.beats(b), a + " ne bat pas " + b);
                assertFalse(b.beats(a), b + " ne bat pas " + a);
            }
        }
    }
}

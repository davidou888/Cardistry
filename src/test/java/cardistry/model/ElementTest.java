package cardistry.model;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
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

    @ParameterizedTest
    @EnumSource(value = Element.class, names = {"FIRE", "WATER", "NATURE"})
    void chaqueElementBatExactementUnAutreEtEstBattuParExactementUnAutre(Element element) {
        assertFalse(element.beats(element));
        assertEquals(1, Element.PLAYABLE.stream().filter(element::beats).count());
        assertEquals(1, Element.PLAYABLE.stream().filter(other -> other.beats(element)).count());
    }
}

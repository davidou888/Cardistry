package cardistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FormatTest {

    @Test
    void nombres() {
        assertEquals("15", Format.num(15.0));
        assertEquals("6.25", Format.num(6.25));
        assertEquals("-3", Format.num(-3));
    }

    @Test
    void multiplicateur() {
        assertEquals("", Format.mult(1.0));
        assertEquals(" (×1.5)", Format.mult(1.5));
    }

    @Test
    void wrapParMots() {
        assertEquals(List.of("Pluie de", "flammes"), Format.wrap("Pluie de flammes", 8));
        assertEquals(List.of("Tempête", "d'épines"), Format.wrap("Tempête d'épines", 8));
        assertEquals(List.of("EPFL"), Format.wrap("EPFL", 8));
    }

    @Test
    void wrapCoupeLesMotsTropLongs() {
        assertEquals(List.of("Caïpiri-", "nha"), Format.wrap("Caïpirinha", 8));
    }
}

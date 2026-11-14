package cardistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.items.Collectible;
import cardistry.items.ItemRegistry;
import org.junit.jupiter.api.Test;

class GlyphsTest {

    private static boolean isAscii(String text) {
        return text.chars().allMatch(c -> c < 128);
    }

    @Test
    void cadresBarresEtSymboles() {
        assertEquals("+-+|=|+", Glyphs.toAscii("┌─┐│═║╚"));
        assertEquals("###...", Glyphs.toAscii("███░░░"));
        assertEquals("< > ^ v x * >", Glyphs.toAscii("← → ↑ ↓ × ★ ▶"));
        assertEquals("\"Cursus\" . - - .", Glyphs.toAscii("«Cursus» · − — …"));
    }

    @Test
    void accentsRetires() {
        assertEquals("Sirene, Caipirinha, Shot de Jager, A chaque tour", Glyphs.toAscii("Sirène, Caïpirinha, Shot de Jäger, À chaque tour"));
    }

    @Test
    void inconnuDevientPointDInterrogation() {
        assertEquals("?", Glyphs.toAscii("✝"));
        assertEquals("??", Glyphs.toAscii("🍺"), "un emoji (paire UTF-16) garde sa largeur de 2");
    }

    @Test
    void laLongueurNeChangeJamais() {
        for (Collectible c : ItemRegistry.all()) {
            for (String text : new String[] {c.name(), c.description(), c.flavor()}) {
                String ascii = Glyphs.toAscii(text);
                assertTrue(isAscii(ascii), ascii);
                assertEquals(text.length(), ascii.length(), text);
            }
        }
    }

    @Test
    void detectionDeLEncodage() {
        assertTrue(Glyphs.isUnicode("UTF-8"));
        assertTrue(Glyphs.isUnicode("utf8"));
        assertFalse(Glyphs.isUnicode("ANSI_X3.4-1968"));
        assertFalse(Glyphs.isUnicode("Cp850"));
        assertFalse(Glyphs.isUnicode("windows-1252"));
    }
}

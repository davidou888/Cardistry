package cardistry.ui.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ConsoleTest {

    private static String print(boolean ascii, String text) {
        return print(ascii, true, text);
    }

    private static String print(boolean ascii, boolean color, String text) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Console console = new Console(new ByteArrayInputStream(new byte[0]),
                new PrintStream(bytes, true, StandardCharsets.UTF_8), true, ascii, color);
        console.print(text);
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void modeAsciiConvertitTout() {
        assertEquals("> Tu recuperes 15 PV (x1.5)", print(true, "→ Tu récupères 15 PV (×1.5)"));
    }

    @Test
    void sansCouleurLesCodesAnsiSontRetires() {
        assertEquals("Bienvenue dans Cardistry !", print(false, false, "Bienvenue dans \u001B[33mCardistry\u001B[39m !"));
        assertEquals("Bienvenue dans \u001B[33mCardistry\u001B[39m !", print(false, true, "Bienvenue dans \u001B[33mCardistry\u001B[39m !"));
    }

    @Test
    void modeNormalInchange() {
        assertEquals("→ Tu récupères 15 PV (×1.5)", print(false, "→ Tu récupères 15 PV (×1.5)"));
    }
}

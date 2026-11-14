package cardistry.ui;

import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Repli ASCII pur pour les terminaux qui n'affichent pas l'Unicode. La conversion est faite au
 * dernier moment (Painter, Console) et garde la <b>longueur</b> des chaînes : un caractère donne
 * toujours un caractère, la mise en page ne bouge pas.
 */
public final class Glyphs {

    private static final Map<Character, Character> SYMBOLS = Map.ofEntries(
            // Cadres
            Map.entry('┌', '+'), Map.entry('┐', '+'), Map.entry('└', '+'), Map.entry('┘', '+'),
            Map.entry('├', '+'), Map.entry('┤', '+'), Map.entry('╔', '+'), Map.entry('╗', '+'),
            Map.entry('╚', '+'), Map.entry('╝', '+'), Map.entry('─', '-'), Map.entry('═', '='),
            Map.entry('│', '|'), Map.entry('║', '|'), Map.entry('╥', '|'),
            // Barres, bannière
            Map.entry('█', '#'), Map.entry('░', '.'),
            // Flèches et symboles
            Map.entry('→', '>'), Map.entry('←', '<'), Map.entry('↑', '^'), Map.entry('↓', 'v'),
            Map.entry('▶', '>'), Map.entry('★', '*'), Map.entry('•', '*'), Map.entry('×', 'x'),
            Map.entry('−', '-'), Map.entry('—', '-'), Map.entry('·', '.'), Map.entry('…', '.'),
            Map.entry('«', '"'), Map.entry('»', '"'), Map.entry('’', '\''), Map.entry(' ', ' '));

    private Glyphs() {
    }

    /** Équivalent ASCII de {@code text}, de même longueur (accents retirés, symboles remplacés, sinon « ? »). */
    public static String toAscii(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(toAscii(text.charAt(i)));
        }
        return out.toString();
    }

    static char toAscii(char c) {
        if (c < 128) {
            return c;
        }
        Character symbol = SYMBOLS.get(c);
        if (symbol != null) {
            return symbol;
        }
        char base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).charAt(0);
        return base < 128 ? base : '?';
    }

    /**
     * {@code true} si la sortie standard est probablement en UTF-8 (sinon : repli ASCII). Lit
     * {@code stdout.encoding} (JDK 19+, reflète la page de code de la console sous Windows), à défaut
     * l'encodage par défaut.
     */
    public static boolean unicodeLikely() {
        String encoding = System.getProperty("stdout.encoding");
        if (encoding == null || encoding.isBlank()) {
            encoding = Charset.defaultCharset().name();
        }
        return isUnicode(encoding);
    }

    static boolean isUnicode(String encoding) {
        return encoding.toUpperCase(Locale.ROOT).replace("-", "").startsWith("UTF");
    }
}

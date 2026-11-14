package cardistry.ui.window;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Polices embarquées (voir {@code src/main/resources/fonts/}, licences OFL 1.1 à côté) :
 * <ul>
 *   <li>Departure Mono (Helena Zhang) : police pixel principale. Sa grille fait 11 px : on ne
 *       l'affiche qu'à des multiples de {@link #BASE_SIZE} pour qu'elle reste nette ;</li>
 *   <li>JetBrains Mono : secours pour les caractères absents de la première (▶).</li>
 * </ul>
 */
public final class CellFonts {

    /** Taille de dessin de la police pixel (une cellule fait alors 7×14 px). */
    public static final int BASE_SIZE = 11;

    private static final Font MAIN = load("/fonts/DepartureMono-Regular.otf");
    private static final Font FALLBACK = load("/fonts/JetBrainsMono-Regular.ttf");

    private final int scale;
    private final Font main;
    private final Font fallback;

    /** @param scale multiple de {@link #BASE_SIZE} (1, 2, 3…) */
    public CellFonts(int scale) {
        this.scale = Math.max(1, scale);
        this.main = MAIN.deriveFont((float) (BASE_SIZE * this.scale));
        this.fallback = FALLBACK.deriveFont((float) (BASE_SIZE * this.scale));
    }

    public int scale() {
        return scale;
    }

    public Font main() {
        return main;
    }

    public Font fallback() {
        return fallback;
    }

    /** Police qui sait dessiner {@code codePoint}, ou {@code null} si aucune. */
    public Font fontFor(int codePoint) {
        if (main.canDisplay(codePoint)) {
            return main;
        }
        return fallback.canDisplay(codePoint) ? fallback : null;
    }

    private static Font load(String path) {
        try (InputStream in = CellFonts.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Police manquante : " + path);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (FontFormatException e) {
            throw new IllegalStateException("Police illisible : " + path, e);
        }
    }
}

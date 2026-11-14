package cardistry.ui.window;

import com.googlecode.lanterna.TextColor;
import java.awt.Color;

/** Couleurs de la fenêtre : nos propres valeurs RGB pour les 16 couleurs ANSI, identiques sur tous les OS. */
public final class CellPalette {

    public static final Color BACKGROUND = new Color(0x12, 0x11, 0x17);
    public static final Color FOREGROUND = new Color(0xD8, 0xD4, 0xC8);

    private CellPalette() {
    }

    /** Couleur d'avant-plan ({@code DEFAULT} = texte clair). */
    public static Color foreground(TextColor color) {
        return color == TextColor.ANSI.DEFAULT ? FOREGROUND : rgb(color);
    }

    /** Couleur de fond ({@code DEFAULT} = fond de la fenêtre). */
    public static Color background(TextColor color) {
        return color == TextColor.ANSI.DEFAULT ? BACKGROUND : rgb(color);
    }

    private static Color rgb(TextColor color) {
        if (color instanceof TextColor.ANSI ansi) {
            return switch (ansi) {
                case BLACK -> new Color(0x1C, 0x1B, 0x22);
                case RED -> new Color(0xC2, 0x3B, 0x3B);
                case GREEN -> new Color(0x4F, 0x9A, 0x48);
                case YELLOW -> new Color(0xC9, 0x9A, 0x2E);
                case BLUE -> new Color(0x3D, 0x6B, 0xC4);
                case MAGENTA -> new Color(0xA2, 0x4D, 0xB0);
                case CYAN -> new Color(0x3C, 0xA0, 0xAE);
                case WHITE -> new Color(0xBF, 0xBB, 0xB2);
                case BLACK_BRIGHT -> new Color(0x6A, 0x67, 0x72);
                case RED_BRIGHT -> new Color(0xFF, 0x5E, 0x57);
                case GREEN_BRIGHT -> new Color(0x7E, 0xD9, 0x6B);
                case YELLOW_BRIGHT -> new Color(0xFF, 0xD8, 0x4F);
                case BLUE_BRIGHT -> new Color(0x6E, 0x9C, 0xFF);
                case MAGENTA_BRIGHT -> new Color(0xE0, 0x7B, 0xF0);
                case CYAN_BRIGHT -> new Color(0x6F, 0xE0, 0xEE);
                case WHITE_BRIGHT -> new Color(0xFF, 0xFB, 0xF0);
                case DEFAULT -> FOREGROUND;
            };
        }
        return color.toColor();
    }
}

package cardistry.ui.lanterna;

import cardistry.items.Rarity;
import cardistry.model.Element;
import com.googlecode.lanterna.TextColor;

/** Couleurs de l'interface. */
final class Palette {

    static final TextColor TEXT = TextColor.ANSI.DEFAULT;
    static final TextColor DIM = TextColor.ANSI.BLACK_BRIGHT;
    static final TextColor FRAME = TextColor.ANSI.WHITE;
    static final TextColor ACCENT = TextColor.ANSI.YELLOW_BRIGHT;
    static final TextColor DAMAGE = TextColor.ANSI.RED_BRIGHT;
    static final TextColor HEAL = TextColor.ANSI.GREEN_BRIGHT;
    static final TextColor WARNING = TextColor.ANSI.YELLOW;
    static final TextColor INFO = TextColor.ANSI.CYAN;
    /** Secrets (même couleur que la rareté Secret). */
    static final TextColor SECRET = TextColor.ANSI.MAGENTA_BRIGHT;

    private Palette() {
    }

    /**
     * Couleur d'une lettre de carte de couleurs de sprite : r g y b m c w k (majuscule = vif) ;
     * autre caractère = {@code fallback}.
     */
    static TextColor sprite(char letter, TextColor fallback) {
        return switch (letter) {
            case 'r' -> TextColor.ANSI.RED;
            case 'R' -> TextColor.ANSI.RED_BRIGHT;
            case 'g' -> TextColor.ANSI.GREEN;
            case 'G' -> TextColor.ANSI.GREEN_BRIGHT;
            case 'y' -> TextColor.ANSI.YELLOW;
            case 'Y' -> TextColor.ANSI.YELLOW_BRIGHT;
            case 'b' -> TextColor.ANSI.BLUE;
            case 'B' -> TextColor.ANSI.BLUE_BRIGHT;
            case 'm' -> TextColor.ANSI.MAGENTA;
            case 'M' -> TextColor.ANSI.MAGENTA_BRIGHT;
            case 'c' -> TextColor.ANSI.CYAN;
            case 'C' -> TextColor.ANSI.CYAN_BRIGHT;
            case 'w' -> TextColor.ANSI.WHITE;
            case 'W' -> TextColor.ANSI.WHITE_BRIGHT;
            case 'k', 'K' -> TextColor.ANSI.BLACK_BRIGHT;
            default -> fallback;
        };
    }

    static TextColor element(Element element) {
        if (element == null) {
            return DIM; // carte morte (NO CARDS)
        }
        return switch (element) {
            case FIRE -> TextColor.ANSI.RED_BRIGHT;
            case WATER -> TextColor.ANSI.BLUE_BRIGHT;
            case NATURE -> TextColor.ANSI.GREEN_BRIGHT;
            case NEUTRAL -> TextColor.ANSI.WHITE;
        };
    }

    static TextColor rarity(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> TextColor.ANSI.WHITE_BRIGHT;
            case UNCOMMON -> TextColor.ANSI.GREEN_BRIGHT;
            case RARE -> TextColor.ANSI.BLUE_BRIGHT;
            case LEGENDARY -> TextColor.ANSI.YELLOW_BRIGHT;
            case SECRET -> SECRET;
        };
    }

    /** Couleur d'une barre de PV selon le pourcentage restant. */
    static TextColor health(double ratio) {
        if (ratio > 0.5) {
            return TextColor.ANSI.GREEN_BRIGHT;
        } else if (ratio > 0.25) {
            return TextColor.ANSI.YELLOW_BRIGHT;
        }
        return TextColor.ANSI.RED_BRIGHT;
    }
}

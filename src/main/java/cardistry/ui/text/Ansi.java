package cardistry.ui.text;

import cardistry.model.Element;

/** Couleurs ANSI pour le rendu texte. */
public final class Ansi {

    public static final String RESET = "\u001B[0m";
    public static final String END_COLOR = "\u001B[39m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    /** Secrets. */
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String GRAY = "\u001B[90m";

    private Ansi() {
    }

    public static String element(Element element) {
        return element == null ? "sans élément" : element(element, element.displayName());
    }

    /** {@code text} coloré selon {@code element}. */
    public static String element(Element element, String text) {
        if (element == null) {
            return text; // carte morte (NO CARDS)
        }
        String color = switch (element) {
            case FIRE -> RED;
            case WATER -> BLUE;
            case NATURE -> GREEN;
            case NEUTRAL -> "";
            case ALGEBRAIC -> CYAN;
            case GEOMETRIC -> YELLOW;
            case SPIRIT -> MAGENTA;
            case GLITCH -> GRAY;
        };
        return color + text + END_COLOR;
    }

    public static String yellow(String text) {
        return YELLOW + text + END_COLOR;
    }
}

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

    private Ansi() {
    }

    public static String element(Element element) {
        return element(element, element.displayName());
    }

    /** {@code text} coloré selon {@code element}. */
    public static String element(Element element, String text) {
        String color = switch (element) {
            case FIRE -> RED;
            case WATER -> BLUE;
            case NATURE -> GREEN;
        };
        return color + text + END_COLOR;
    }

    public static String yellow(String text) {
        return YELLOW + text + END_COLOR;
    }
}

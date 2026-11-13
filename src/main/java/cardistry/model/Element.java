package cardistry.model;

/** Les trois éléments du jeu. Cycle : Feu > Nature > Eau > Feu. */
public enum Element {
    FIRE("feu"),
    WATER("eau"),
    NATURE("nature");

    private final String displayName;

    Element(String displayName) {
        this.displayName = displayName;
    }

    /** Nom affiché au joueur (français). */
    public String displayName() {
        return displayName;
    }

    /** {@code true} si cet élément bat {@code other}. */
    public boolean beats(Element other) {
        return switch (this) {
            case FIRE -> other == NATURE;
            case NATURE -> other == WATER;
            case WATER -> other == FIRE;
        };
    }
}

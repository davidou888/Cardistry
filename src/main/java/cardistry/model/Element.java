package cardistry.model;

import java.util.List;

/**
 * Éléments du jeu. Cycle : Feu > Plante > Eau > Feu. {@link #NEUTRAL} n'existe que pour des
 * adversaires : il ne bat rien et n'est battu par rien (×1 contre tout sort, terrain sans effet).
 * L'élément nature s'affiche « plante ».
 */
public enum Element {
    FIRE("feu"),
    WATER("eau"),
    NATURE("plante"),
    NEUTRAL("neutre"),
    ALGEBRAIC("algebrique"),
    SPIRIT("spirituel"),
    GEOMETRIC("geometrique"),
    GLITCH("error: null");

    /** Éléments des cartes, terrains et soins (tout sauf {@link #NEUTRAL}). */
    public static final List<Element> PLAYABLE = List.of(FIRE, WATER, NATURE, ALGEBRAIC, SPIRIT, GEOMETRIC, GLITCH);

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
            case NEUTRAL -> false;
            case GEOMETRIC -> other == ALGEBRAIC;
            case ALGEBRAIC -> other == SPIRIT;
            case SPIRIT -> other == GEOMETRIC;
            case GLITCH -> true; // bat tout, lui-même et le neutre compris
        };
    }

    /** {@code true} si cet élément bat tous les éléments, lui-même compris (GLITCH). */
    public boolean beatsEverything() {
        return java.util.Arrays.stream(values()).allMatch(this::beats);
    }
}

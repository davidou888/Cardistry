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
    NEUTRAL("neutre");

    /** Éléments des cartes, terrains et soins (tout sauf {@link #NEUTRAL}). */
    public static final List<Element> PLAYABLE = List.of(FIRE, WATER, NATURE);

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
        };
    }
}

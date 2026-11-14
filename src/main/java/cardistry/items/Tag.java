package cardistry.items;

/** Étiquettes des objets et trinkets, pour les synergies. */
public enum Tag {
    FIRE("Feu"),
    WATER("Eau"),
    NATURE("Plante"),
    HEAL("Soin"),
    DRAW("Pioche"),
    TERRAIN("Terrain"),
    DEFENSE("Défense"),
    EPFL("EPFL"),
    PARTY("Soirée"),
    DRINK("Boisson"),
    META("Méta");

    private final String displayName;

    Tag(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

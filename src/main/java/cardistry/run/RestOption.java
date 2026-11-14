package cardistry.run;

/** Choix de la Sieste (un seul). */
public enum RestOption {
    /** Soin de 30 % des PV max. */
    HEAL("Dormir", "+30 % de tes PV max"),
    /** +3 clopes. */
    CIGARETTES("Rouler tranquille", "+3 clopes"),
    /** Retirer une carte du deck (avec le deck construit). */
    REMOVE_CARD("Faire le tri", "retire une carte de ton deck");

    private final String displayName;
    private final String description;

    RestOption(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}

package cardistry.core.status;

/**
 * Statuts applicables au joueur et aux adversaires. Les chiffres de chaque statut sont ici ; la
 * façon dont ils s'appliquent est dans le moteur ({@code CombatEngine}).
 */
public enum StatusType {
    /** −{@link #INTOX_DAMAGE} PV par tour ; impossible à réappliquer tant qu'il est actif. */
    INTOX("Intox", "intox", 3, Reapply.REFUSE);

    /** PV perdus par tour d'Intox. */
    public static final double INTOX_DAMAGE = 5;

    /** Réapplication d'un statut déjà actif. */
    public enum Reapply {
        /** Refusée tant que le statut est actif. */
        REFUSE,
        /** Durée remise au max. */
        RESET
    }

    private final String displayName;
    private final String id;
    private final int defaultTurns;
    private final Reapply reapply;

    StatusType(String displayName, String id, int defaultTurns, Reapply reapply) {
        this.displayName = displayName;
        this.id = id;
        this.defaultTurns = defaultTurns;
        this.reapply = reapply;
    }

    /** Nom affiché au joueur. */
    public String displayName() {
        return displayName;
    }

    /** Identifiant (icône {@code images/status/<id>.png}, son {@code sfx.status.<id>}). */
    public String id() {
        return id;
    }

    /** Durée en tours de la cible quand l'effet n'en précise pas. */
    public int defaultTurns() {
        return defaultTurns;
    }

    public Reapply reapply() {
        return reapply;
    }
}

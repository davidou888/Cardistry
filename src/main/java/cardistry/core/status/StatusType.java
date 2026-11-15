package cardistry.core.status;

/**
 * Statuts applicables au joueur et aux adversaires. Les chiffres de chaque statut sont ici ; la
 * façon dont ils s'appliquent est dans le moteur ({@code CombatEngine}).
 *
 * <p>Durée : un statut dure N tours de sa cible (effet au début de son tour, décompte à la fin),
 * sauf {@link #HYPNOTIZED}, compté en tours de celui qui frappe. Un statut ne décompte jamais à la
 * fin du tour où il a été posé.
 */
public enum StatusType {
    /** −{@link #INTOX_DAMAGE} PV par tour ; impossible à réappliquer tant qu'il est actif. */
    INTOX("Intox", "intox", 3, 3, Reapply.REFUSE, false),
    /** La cible ne joue pas (le joueur passe ses tours, l'adversaire n'attaque pas). 1 à 3 tours. */
    FONS("Fons", "fons", 1, 3, Reapply.RESET, false),
    /** Les cartes d'attaque font ×{@link #HYPNOSIS_MULTIPLIER} à la cible. */
    HYPNOTIZED("Hypnotisé", "hypnotise", 1, 1, Reapply.RESET, true),
    /** Vole {@link #BONDAGE_STEAL} PV par tour à la cible ({@link #BONDAGE_STEAL_PLANT} sur un terrain plante). */
    BONDAGE("Bondage", "bondage", 3, 3, Reapply.RESET, false),
    /** −{@link #BURN_DAMAGE_FIRE} PV par tour sur un terrain feu, −{@link #BURN_DAMAGE} ailleurs ; éteinte par l'eau. */
    BURN("Brûlure", "brulure", 3, 3, Reapply.RESET, false);

    /** PV perdus par tour d'Intox. */
    public static final double INTOX_DAMAGE = 5;
    /** Multiplicateur des cartes d'attaque contre une cible hypnotisée. */
    public static final double HYPNOSIS_MULTIPLIER = 2;
    public static final double BONDAGE_STEAL = 5;
    public static final double BONDAGE_STEAL_PLANT = 8;
    public static final double BURN_DAMAGE = 4;
    public static final double BURN_DAMAGE_FIRE = 8;

    /** Réapplication d'un statut déjà actif. */
    public enum Reapply {
        /** Refusée tant que le statut est actif. */
        REFUSE,
        /** Durée remise au max. */
        RESET
    }

    private final String displayName;
    private final String id;
    private final int minTurns;
    private final int maxTurns;
    private final Reapply reapply;
    private final boolean countsOnAttackerTurns;

    StatusType(String displayName, String id, int minTurns, int maxTurns, Reapply reapply,
            boolean countsOnAttackerTurns) {
        this.displayName = displayName;
        this.id = id;
        this.minTurns = minTurns;
        this.maxTurns = maxTurns;
        this.reapply = reapply;
        this.countsOnAttackerTurns = countsOnAttackerTurns;
    }

    /** Nom affiché au joueur. */
    public String displayName() {
        return displayName;
    }

    /** Identifiant (icône {@code images/status/<id>.png}, son {@code sfx.status.<id>}). */
    public String id() {
        return id;
    }

    /** Durée minimale quand l'effet n'en précise pas (tirée entre min et max). */
    public int minTurns() {
        return minTurns;
    }

    /** Durée maximale quand l'effet n'en précise pas. */
    public int maxTurns() {
        return maxTurns;
    }

    public Reapply reapply() {
        return reapply;
    }

    /** Camp dont les fins de tour font décompter ce statut porté par {@code owner}. */
    public StatusTarget countingSide(StatusTarget owner) {
        return countsOnAttackerTurns ? owner.other() : owner;
    }
}

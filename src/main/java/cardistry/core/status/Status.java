package cardistry.core.status;

/** Un statut actif : son type, les tours qui lui restent, et qui l'a infligé. */
public final class Status {

    private final StatusType type;
    private final StatusTarget inflictedBy;
    private int turnsLeft;
    private int maxTurns;
    private StatusExtras extras;
    /** Posé pendant le tour en cours : ne décompte pas à la fin de ce tour. */
    private boolean fresh = true;

    public Status(StatusType type, int turns, StatusTarget inflictedBy) {
        this(type, turns, inflictedBy, StatusExtras.NONE);
    }

    public Status(StatusType type, int turns, StatusTarget inflictedBy, StatusExtras extras) {
        if (turns <= 0) {
            throw new IllegalArgumentException("Durée de statut invalide : " + turns);
        }
        this.type = type;
        this.turnsLeft = turns;
        this.maxTurns = turns;
        this.inflictedBy = inflictedBy;
        this.extras = extras;
    }

    public StatusExtras extras() {
        return extras;
    }

    public StatusType type() {
        return type;
    }

    public int turnsLeft() {
        return turnsLeft;
    }

    /** Durée au moment où il a été (ré)appliqué. */
    public int maxTurns() {
        return maxTurns;
    }

    public StatusTarget inflictedBy() {
        return inflictedBy;
    }

    /** Remet la durée au max (réapplication). */
    void reset(int turns, StatusExtras more) {
        turnsLeft = Math.max(turnsLeft, turns);
        maxTurns = turnsLeft;
        extras = extras.merge(more);
        fresh = true;
    }

    /**
     * Fin d'un tour : décompte d'un tour si {@code counts} (tour du camp qui le fait décompter) et
     * s'il n'a pas été posé pendant ce tour.
     *
     * @return {@code true} s'il est terminé
     */
    boolean endTurn(boolean counts) {
        if (fresh) {
            fresh = false;
            return false;
        }
        if (counts) {
            turnsLeft--;
        }
        return turnsLeft <= 0;
    }
}

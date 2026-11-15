package cardistry.core.status;

/** Un statut actif : son type, les tours qui lui restent, et qui l'a infligé. */
public final class Status {

    private final StatusType type;
    private final StatusTarget inflictedBy;
    private int turnsLeft;
    private int maxTurns;

    public Status(StatusType type, int turns, StatusTarget inflictedBy) {
        if (turns <= 0) {
            throw new IllegalArgumentException("Durée de statut invalide : " + turns);
        }
        this.type = type;
        this.turnsLeft = turns;
        this.maxTurns = turns;
        this.inflictedBy = inflictedBy;
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
    void reset(int turns) {
        turnsLeft = Math.max(turnsLeft, turns);
        maxTurns = turnsLeft;
    }

    /** Un tour de moins ; {@code true} s'il est terminé. */
    boolean countDown() {
        turnsLeft--;
        return turnsLeft <= 0;
    }
}

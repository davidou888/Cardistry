package cardistry.core.status;

/** Qui porte un statut (ou qui l'a infligé). */
public enum StatusTarget {
    PLAYER,
    ENEMY;

    /** L'autre camp. */
    public StatusTarget other() {
        return this == PLAYER ? ENEMY : PLAYER;
    }
}

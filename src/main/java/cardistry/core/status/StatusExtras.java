package cardistry.core.status;

/**
 * Variantes d'un statut posé par une carte précise (Fons de la Petite taffe, de la Taffe de foufou
 * bâtard) : soin par tour en % des PV max, immunité aux coups adverses.
 */
public record StatusExtras(double healMaxHpPerTurn, boolean immuneToAttacks) {

    public static final StatusExtras NONE = new StatusExtras(0, false);

    /** Garde le plus fort de chaque variante (réapplication). */
    StatusExtras merge(StatusExtras other) {
        return new StatusExtras(Math.max(healMaxHpPerTurn, other.healMaxHpPerTurn),
                immuneToAttacks || other.immuneToAttacks);
    }
}

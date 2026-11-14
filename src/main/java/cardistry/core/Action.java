package cardistry.core;

/** Ce que le joueur peut faire pendant son tour. */
public sealed interface Action {

    /** Jouer la carte à l'index {@code slot} de la main (0 = première carte). */
    record PlayCard(int slot) implements Action {
    }

    /** Piocher une carte. */
    record DrawCard() implements Action {
    }

    /** Fumer une clope : action gratuite. */
    record Smoke() implements Action {
    }

    /** Rouler une clope : termine le tour, comme piocher. */
    record RollCigarette() implements Action {
    }

    /**
     * Annuler son dernier tour (trinket Ctrl+Z) : gérée par {@code run.Run}, qui rejoue la partie ;
     * le moteur seul la refuse.
     */
    record Undo() implements Action {
    }

    /** Utiliser l'objet à l'index {@code slot} de l'inventaire : action gratuite. */
    record UseItem(int slot) implements Action {
    }
}

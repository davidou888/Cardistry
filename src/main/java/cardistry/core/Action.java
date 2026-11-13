package cardistry.core;

/** Ce que le joueur peut faire pendant son tour. */
public sealed interface Action {

    /** Jouer la carte à l'index {@code slot} de la main (0 = première carte). */
    record PlayCard(int slot) implements Action {
    }

    /** Piocher une carte. */
    record DrawCard() implements Action {
    }
}

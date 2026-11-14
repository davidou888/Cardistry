package cardistry.ui;

/**
 * Le joueur abandonne la partie en cours (Q puis confirmation, ou « Quitter la partie » du menu
 * pause) : retour à l'écran titre, l'application reste ouverte. Quitter l'application elle-même,
 * c'est {@link QuitGameException}.
 */
public final class LeaveGameException extends RuntimeException {

    public LeaveGameException() {
        super("Retour à l'écran titre");
    }
}

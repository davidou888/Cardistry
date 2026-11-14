package cardistry.ui;

/**
 * Le joueur quitte l'application (« Quitter » de l'écran titre, fenêtre fermée, Ctrl+D, entrée
 * fermée…). Abandonner seulement la partie en cours : {@link LeaveGameException}.
 */
public final class QuitGameException extends RuntimeException {

    public QuitGameException() {
        super("Partie quittée");
    }
}

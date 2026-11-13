package cardistry.ui;

/** Le joueur quitte la partie (touche Quitter, Ctrl+D, entrée fermée…). */
public final class QuitGameException extends RuntimeException {

    public QuitGameException() {
        super("Partie quittée");
    }
}

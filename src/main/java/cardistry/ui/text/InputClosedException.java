package cardistry.ui.text;

/** L'entrée standard est fermée (Ctrl+D, fin d'un fichier redirigé…). */
public final class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("Entrée fermée");
    }
}

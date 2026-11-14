package cardistry.ui.window;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Images à dessiner par-dessus la grille, alignées sur les cellules. L'interface les ajoute pendant
 * qu'elle dessine un écran ({@link #begin()}, {@link #add}, {@link #commit()}) ; la fenêtre lit la
 * dernière liste validée au moment d'afficher. Sans fenêtre (terminal), il n'y a pas de calque.
 */
public final class ImageLayer {

    /**
     * Une image dans le rectangle de cellules ({@code col}, {@code row}, {@code cols} × {@code rows}),
     * centrée et agrandie sans lissage.
     *
     * @param tint si non null, l'image est entièrement de cette couleur (dégâts reçus)
     * @param alpha opacité, de 0 à 1
     */
    public record Placement(int col, int row, int cols, int rows, String path, Color tint, float alpha) {
    }

    private final List<Placement> drawing = new ArrayList<>();
    private volatile List<Placement> committed = List.of();

    public synchronized void begin() {
        drawing.clear();
    }

    public synchronized void add(Placement placement) {
        drawing.add(placement);
    }

    public synchronized void commit() {
        committed = List.copyOf(drawing);
    }

    /** Dernière liste validée. */
    public List<Placement> snapshot() {
        return committed;
    }
}

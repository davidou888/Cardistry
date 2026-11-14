package cardistry.ui;

import cardistry.run.map.MapNode;
import cardistry.run.map.RunMap;

/**
 * Dessin ASCII d'une carte, partagé par les deux interfaces : le boss en haut, l'étage 1 en bas,
 * un nœud tous les {@link #SPACING} caractères, et les liens entre étages ({@code | / \}).
 */
public final class MapArt {

    public static final int SPACING = 6;
    public static final int MARGIN = 2;
    public static final int WIDTH = MARGIN * 2 + (RunMap.COLUMNS - 1) * SPACING + 1;
    public static final int HEIGHT = RunMap.FLOORS * 2 - 1;

    private MapArt() {
    }

    /** Ligne d'un nœud dans le dessin (0 = boss). */
    public static int row(MapNode node) {
        return (RunMap.FLOORS - node.floor()) * 2;
    }

    /** Colonne d'un nœud dans le dessin. */
    public static int col(MapNode node) {
        return MARGIN + node.column() * SPACING;
    }

    /** Grille {@link #HEIGHT} × {@link #WIDTH} : symboles des nœuds et liens. */
    public static char[][] grid(RunMap map) {
        char[][] grid = new char[HEIGHT][WIDTH];
        for (char[] line : grid) {
            java.util.Arrays.fill(line, ' ');
        }
        for (MapNode node : map.nodes()) {
            for (MapNode next : map.choicesAfter(node)) {
                int x = (col(node) + col(next)) / 2;
                char link = next.column() > node.column() ? '/' : next.column() < node.column() ? '\\' : '|';
                grid[row(node) - 1][x] = link;
            }
            grid[row(node)][col(node)] = node.type().symbol();
        }
        return grid;
    }
}

package cardistry.ui;

import cardistry.core.Rng;
import cardistry.run.map.MapNode;
import cardistry.run.map.NodeType;
import cardistry.run.map.RunMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dessin « organique » d'une carte, de gauche (étage 1) à droite (boss) : chaque nœud est décalé au
 * hasard dans sa case, et chaque lien est une courbe bruitée tracée en pointillés. Tout est tiré d'un
 * sous-flux de la seed réservé au dessin ({@code carte-dessin-<niveau>}) : même seed, même dessin, et
 * la génération de la carte elle-même n'est pas touchée.
 *
 * <p>Les coordonnées sont des pixels logiques dans une zone {@code width} × {@code height} (les unités
 * du calque de la fenêtre). Aucun affichage ici : l'écran de la carte pose les icônes et les points.
 */
public final class MapLayout {

    /** Décalage maximal d'un nœud, en fraction de l'écart entre deux étages (de part et d'autre). */
    public static final double JITTER_X = 0.20;
    /** Décalage maximal d'un nœud, en fraction de l'écart entre deux colonnes. */
    public static final double JITTER_Y = 0.18;
    /** Distance entre deux points d'un chemin. */
    public static final int DOT_STEP = 5;
    /** Marge sans point autour d'une icône. */
    public static final int CLEARANCE = 5;
    /** Le boss est dessiné plus grand que les autres nœuds. */
    public static final int BOSS_EXTRA = 8;

    private static final int SAMPLES = 200;

    public record Point(int x, int y) {
    }

    /** Lien entre deux nœuds : les points du pointillé, du départ vers l'arrivée. */
    public record Path(MapNode from, MapNode to, List<Point> dots) {
    }

    private final Map<MapNode, Point> positions = new LinkedHashMap<>();
    private final List<Path> paths = new ArrayList<>();
    private final int iconSize;

    private MapLayout(int iconSize) {
        this.iconSize = iconSize;
    }

    /** Dessin de {@code map} pour la seed {@code seed}, dans une zone de {@code width} × {@code height}. */
    public static MapLayout of(RunMap map, long seed, int width, int height) {
        double dx = width / (double) RunMap.FLOORS;
        double dy = height / (double) RunMap.COLUMNS;
        // Icônes de 16 px : taille en multiples de 8 pour rester nette à l'échelle habituelle (×2).
        MapLayout layout = new MapLayout(Math.max(8, (int) (0.5 * Math.min(dx, dy) / 8) * 8));
        Rng rng = new Rng(seed).fork("carte-dessin-" + map.level());

        Map<MapNode, double[]> exact = new LinkedHashMap<>();
        for (MapNode node : map.nodes()) {
            double jx = (rng.nextDouble() - 0.5) * 2 * JITTER_X * dx;
            double jy = (rng.nextDouble() - 0.5) * 2 * JITTER_Y * dy;
            double x = (node.floor() - 0.5) * dx;
            double y = (node.column() + 0.5) * dy;
            if (node.type() == NodeType.BOSS) {
                y = height / 2.0; // le boss reste au bout, au milieu
            } else {
                x += jx;
                y += jy;
            }
            exact.put(node, new double[] {x, y});
            layout.positions.put(node, new Point((int) Math.round(x), (int) Math.round(y)));
        }
        for (MapNode node : map.nodes()) {
            for (MapNode next : map.choicesAfter(node)) {
                double amplitude = (0.04 + 0.10 * rng.nextDouble()) * dy;
                double phase = rng.nextDouble() * 2 * Math.PI;
                double frequency = 1 + 1.5 * rng.nextDouble();
                layout.paths.add(new Path(node, next, dots(exact.get(node), exact.get(next), amplitude, phase,
                        frequency, layout.size(node) / 2.0 + CLEARANCE, layout.size(next) / 2.0 + CLEARANCE)));
            }
        }
        return layout;
    }

    /**
     * Points d'une courbe de Bézier à tangentes horizontales, déformée par une onde qui s'annule aux
     * deux bouts ; un point tous les {@link #DOT_STEP}, aucun sous les icônes.
     */
    private static List<Point> dots(double[] a, double[] b, double amplitude, double phase, double frequency,
            double clearFrom, double clearTo) {
        List<Point> dots = new ArrayList<>();
        double middle = (a[0] + b[0]) / 2;
        double length = 0;
        double nextDot = 0;
        double px = a[0];
        double py = a[1];
        for (int i = 1; i <= SAMPLES; i++) {
            double t = i / (double) SAMPLES;
            double u = 1 - t;
            double x = u * u * u * a[0] + 3 * u * u * t * middle + 3 * u * t * t * middle + t * t * t * b[0];
            double y = (u * u * u + 3 * u * u * t) * a[1] + (3 * u * t * t + t * t * t) * b[1];
            y += amplitude * Math.sin(Math.PI * t) * Math.sin(frequency * 2 * Math.PI * t + phase);
            length += Math.hypot(x - px, y - py);
            px = x;
            py = y;
            if (length >= nextDot) {
                nextDot += DOT_STEP;
                if (Math.hypot(x - a[0], y - a[1]) > clearFrom && Math.hypot(x - b[0], y - b[1]) > clearTo) {
                    Point dot = new Point((int) Math.round(x), (int) Math.round(y));
                    if (dots.isEmpty() || !dots.get(dots.size() - 1).equals(dot)) {
                        dots.add(dot);
                    }
                }
            }
        }
        return dots;
    }

    /** Centre de l'icône du nœud. */
    public Point position(MapNode node) {
        return positions.get(node);
    }

    /** Tous les liens, dans l'ordre des nœuds. */
    public List<Path> paths() {
        return java.util.Collections.unmodifiableList(paths);
    }

    /** Côté de l'icône d'un nœud ordinaire. */
    public int iconSize() {
        return iconSize;
    }

    /** Côté de l'icône de {@code node} (le boss est plus grand). */
    public int size(MapNode node) {
        return node.type() == NodeType.BOSS ? iconSize + BOSS_EXTRA : iconSize;
    }
}

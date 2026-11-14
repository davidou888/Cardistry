package cardistry.run.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Carte d'un niveau : {@link #FLOORS} étages sur {@link #COLUMNS} colonnes, un boss au sommet. */
public final class RunMap {

    public static final int FLOORS = 8;
    public static final int COLUMNS = 5;
    /** Étage de la Sieste (juste avant le boss). */
    public static final int REST_FLOOR = FLOORS - 1;

    private final int level;
    private final List<List<MapNode>> floors;

    RunMap(int level, List<List<MapNode>> floors) {
        this.level = level;
        this.floors = floors.stream().map(List::copyOf).toList();
    }

    public int level() {
        return level;
    }

    /** Nœuds d'un étage (1 à {@link #FLOORS}), par colonne croissante. */
    public List<MapNode> floor(int floor) {
        return floors.get(floor - 1);
    }

    public Optional<MapNode> node(int floor, int column) {
        if (floor < 1 || floor > FLOORS) {
            return Optional.empty();
        }
        return floor(floor).stream().filter(n -> n.column() == column).findFirst();
    }

    /** Nœuds atteignables après {@code from} ; {@code null} = départ (étage 1). */
    public List<MapNode> choicesAfter(MapNode from) {
        if (from == null) {
            return floor(1);
        }
        List<MapNode> out = new ArrayList<>();
        for (int column : from.next()) {
            node(from.floor() + 1, column).ifPresent(out::add);
        }
        return out;
    }

    /** Tous les nœuds, étage par étage. */
    public List<MapNode> nodes() {
        return floors.stream().flatMap(List::stream).toList();
    }

    public MapNode boss() {
        return floor(FLOORS).get(0);
    }
}

package cardistry.run.map;

import static cardistry.run.map.RunMap.COLUMNS;
import static cardistry.run.map.RunMap.FLOORS;
import static cardistry.run.map.RunMap.REST_FLOOR;

import cardistry.core.Rng;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Génère la carte d'un niveau, façon Slay the Spire : {@link #PATHS} chemins montent du bas
 * (étage 1) vers le boss, en se déplaçant d'au plus une colonne par étage ; ils peuvent se
 * rejoindre mais jamais se croiser en X. Puis chaque nœud reçoit un type :
 * <ul>
 *   <li>étage 1 : Combat ; étage {@link RunMap#REST_FLOOR} : Sieste ; étage {@link RunMap#FLOORS} : Boss ;</li>
 *   <li>étages 2-3 : {@link #EARLY_WEIGHTS} (pas d'élite ni de sieste) ; 4-6 : {@link #LATE_WEIGHTS} ;</li>
 *   <li>jamais deux Shop, deux Élites ou deux Siestes à la suite sur un chemin (donc pas de Sieste à
 *       l'étage 6) ;</li>
 *   <li>au moins un Shop sur la carte, et au moins un chemin sans élite.</li>
 * </ul>
 */
public final class MapGenerator {

    public static final int PATHS = 4;
    /** Colonne du boss (au centre). */
    public static final int BOSS_COLUMN = COLUMNS / 2;

    /** Poids des types aux étages 2 et 3. */
    public static final Map<NodeType, Integer> EARLY_WEIGHTS = weights(60, 25, 15, 0, 0, 25); //A MODIFIER !!!
    /** Poids des types aux étages 4 à 6. */
    public static final Map<NodeType, Integer> LATE_WEIGHTS = weights(45, 22, 10, 15, 8, 22);

    private MapGenerator() {
    }

    private static Map<NodeType, Integer> weights(int combat, int event, int shop, int elite, int rest, int altar) {
        Map<NodeType, Integer> w = new EnumMap<>(NodeType.class);
        w.put(NodeType.COMBAT, combat);
        w.put(NodeType.EVENT, event);
        w.put(NodeType.SHOP, shop);
        w.put(NodeType.ELITE, elite);
        w.put(NodeType.REST, rest);
        w.put(NodeType.ALTAR, altar);
        return w;
    }

    /** Carte du niveau {@code level}, tirée dans {@code rng} (un sous-flux dédié à la carte). */
    public static RunMap generate(Rng rng, int level) {
        // edges[f] : colonne de l'étage f → colonnes de l'étage f+1 (f de 1 à FLOORS - 1).
        List<Map<Integer, Set<Integer>>> edges = new ArrayList<>();
        for (int f = 0; f <= FLOORS; f++) {
            edges.add(new TreeMap<>());
        }
        int firstStart = -1;
        for (int p = 0; p < PATHS; p++) {
            int column = rng.nextInt(COLUMNS);
            if (p == 1) {
                while (column == firstStart) {
                    column = rng.nextInt(COLUMNS);
                }
            }
            if (p == 0) {
                firstStart = column;
            }
            for (int floor = 1; floor < REST_FLOOR; floor++) {
                List<Integer> options = new ArrayList<>();
                for (int next = column - 1; next <= column + 1; next++) {
                    if (next >= 0 && next < COLUMNS && !crosses(edges.get(floor), column, next)) {
                        options.add(next);
                    }
                }
                int next = rng.pick(options); // tout droit ne croise jamais : jamais vide
                edges.get(floor).computeIfAbsent(column, c -> new TreeSet<>()).add(next);
                column = next;
            }
            edges.get(REST_FLOOR).computeIfAbsent(column, c -> new TreeSet<>()).add(BOSS_COLUMN);
        }

        Map<Integer, Map<Integer, NodeType>> types = new HashMap<>();
        for (int floor = 1; floor <= FLOORS; floor++) {
            types.put(floor, new TreeMap<>());
        }
        for (int floor = 1; floor <= REST_FLOOR; floor++) {
            for (int column : columns(edges, floor)) {
                types.get(floor).put(column, pickType(rng, floor, parents(edges, types, floor, column)));
            }
        }
        types.get(FLOORS).put(BOSS_COLUMN, NodeType.BOSS);
        ensureShop(rng, edges, types);
        ensurePathWithoutElite(rng, edges, types);

        List<List<MapNode>> floors = new ArrayList<>();
        for (int floor = 1; floor <= FLOORS; floor++) {
            List<MapNode> nodes = new ArrayList<>();
            for (Map.Entry<Integer, NodeType> entry : types.get(floor).entrySet()) {
                Set<Integer> next = floor < FLOORS ? edges.get(floor).getOrDefault(entry.getKey(), Set.of()) : Set.of();
                nodes.add(new MapNode(floor, entry.getKey(), entry.getValue(), new ArrayList<>(next)));
            }
            floors.add(nodes);
        }
        return new RunMap(level, floors);
    }

    /** {@code true} si l'arête {@code from → to} croise une arête déjà posée sur cet étage. */
    static boolean crosses(Map<Integer, Set<Integer>> floorEdges, int from, int to) {
        for (Map.Entry<Integer, Set<Integer>> entry : floorEdges.entrySet()) {
            int a = entry.getKey();
            for (int b : entry.getValue()) {
                if ((a < from && b > to) || (a > from && b < to)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Colonnes occupées à l'étage {@code floor} (1 à {@link RunMap#REST_FLOOR}). */
    private static Set<Integer> columns(List<Map<Integer, Set<Integer>>> edges, int floor) {
        Set<Integer> columns = new TreeSet<>(edges.get(floor).keySet());
        if (floor > 1) {
            edges.get(floor - 1).values().forEach(columns::addAll);
        }
        return columns;
    }

    /** Types des nœuds de l'étage précédent reliés à ({@code floor}, {@code column}). */
    private static List<NodeType> parents(List<Map<Integer, Set<Integer>>> edges,
            Map<Integer, Map<Integer, NodeType>> types, int floor, int column) {
        List<NodeType> parents = new ArrayList<>();
        if (floor > 1) {
            edges.get(floor - 1).forEach((from, targets) -> {
                if (targets.contains(column)) {
                    parents.add(types.get(floor - 1).get(from));
                }
            });
        }
        return parents;
    }

    private static List<NodeType> children(List<Map<Integer, Set<Integer>>> edges,
            Map<Integer, Map<Integer, NodeType>> types, int floor, int column) {
        List<NodeType> children = new ArrayList<>();
        for (int next : edges.get(floor).getOrDefault(column, Set.of())) {
            children.add(types.get(floor + 1).get(next));
        }
        return children;
    }

    private static NodeType pickType(Rng rng, int floor, List<NodeType> parents) {
        if (floor == 1) {
            return NodeType.COMBAT;
        }
        if (floor == REST_FLOOR) {
            return NodeType.REST;
        }
        Map<NodeType, Integer> weights = new EnumMap<>(floor <= 3 ? EARLY_WEIGHTS : LATE_WEIGHTS);
        if (floor == REST_FLOOR - 1) {
            weights.remove(NodeType.REST);
        }
        for (NodeType parent : parents) {
            if (parent.noRepeat()) {
                weights.remove(parent);
            }
        }
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = rng.nextInt(total);
        for (Map.Entry<NodeType, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        throw new AssertionError("poids vides");
    }

    /** Pas de Shop tiré : un Combat ou un Événement des étages 2-6 en devient un (sans Shop autour). */
    private static void ensureShop(Rng rng, List<Map<Integer, Set<Integer>>> edges,
            Map<Integer, Map<Integer, NodeType>> types) {
        List<int[]> candidates = new ArrayList<>();
        for (int floor = 2; floor < REST_FLOOR; floor++) {
            for (Map.Entry<Integer, NodeType> entry : types.get(floor).entrySet()) {
                if (entry.getValue() == NodeType.SHOP) {
                    return;
                }
                boolean free = (entry.getValue() == NodeType.COMBAT || entry.getValue() == NodeType.EVENT)
                        && !parents(edges, types, floor, entry.getKey()).contains(NodeType.SHOP)
                        && !children(edges, types, floor, entry.getKey()).contains(NodeType.SHOP);
                if (free) {
                    candidates.add(new int[] {floor, entry.getKey()});
                }
            }
        }
        if (!candidates.isEmpty()) {
            int[] chosen = rng.pick(candidates);
            types.get(chosen[0]).put(chosen[1], NodeType.SHOP);
        }
    }

    /** Tant que tous les chemins passent par une élite, une élite redevient un Combat. */
    private static void ensurePathWithoutElite(Rng rng, List<Map<Integer, Set<Integer>>> edges,
            Map<Integer, Map<Integer, NodeType>> types) {
        while (!hasPathWithoutElite(edges, types)) {
            List<int[]> elites = new ArrayList<>();
            types.forEach((floor, row) -> row.forEach((column, type) -> {
                if (type == NodeType.ELITE) {
                    elites.add(new int[] {floor, column});
                }
            }));
            int[] chosen = rng.pick(elites);
            types.get(chosen[0]).put(chosen[1], NodeType.COMBAT);
        }
    }

    private static boolean hasPathWithoutElite(List<Map<Integer, Set<Integer>>> edges,
            Map<Integer, Map<Integer, NodeType>> types) {
        Set<Integer> reachable = new TreeSet<>();
        types.get(1).forEach((column, type) -> reachable.add(column));
        for (int floor = 1; floor < REST_FLOOR; floor++) {
            Set<Integer> next = new TreeSet<>();
            for (int column : reachable) {
                if (types.get(floor).get(column) == NodeType.ELITE) {
                    continue;
                }
                next.addAll(edges.get(floor).getOrDefault(column, Set.of()));
            }
            reachable.clear();
            reachable.addAll(next);
        }
        return !reachable.isEmpty();
    }
}

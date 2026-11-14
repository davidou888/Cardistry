package cardistry.run.map;

import java.util.List;

/**
 * Un nœud : étage (1 = départ, {@link RunMap#FLOORS} = boss), colonne, type, et colonnes des nœuds
 * de l'étage suivant qu'il relie (triées).
 */
public record MapNode(int floor, int column, NodeType type, List<Integer> next) {

    public MapNode {
        next = List.copyOf(next);
    }
}

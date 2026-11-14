package cardistry.run.map;

import cardistry.core.GameState;
import java.util.List;

/**
 * Ce que l'interface montre de la carte : la carte du niveau, les nœuds visités, la position, et
 * les nœuds où le joueur peut aller.
 */
public record MapView(GameState state, RunMap map, List<MapNode> visited, MapNode position, List<MapNode> choices) {

    public MapView {
        visited = List.copyOf(visited);
        choices = List.copyOf(choices);
    }
}

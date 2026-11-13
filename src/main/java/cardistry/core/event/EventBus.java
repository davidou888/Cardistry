package cardistry.core.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bus synchrone : chaque événement est livré à tous les abonnés, dans l'ordre d'abonnement.
 *
 * <p>Un événement publié <i>pendant</i> la livraison d'un autre (par exemple un trinket qui
 * réagit) est mis en file et livré juste après : tous les abonnés voient les événements dans le
 * même ordre. {@link #publish} ne rend la main qu'une fois la file vide.
 */
public final class EventBus {

    private final List<Consumer<? super GameEvent>> listeners = new ArrayList<>();
    private final Deque<GameEvent> queue = new ArrayDeque<>();
    private boolean dispatching = false;

    public void subscribe(Consumer<? super GameEvent> listener) {
        listeners.add(listener);
    }

    public void publish(GameEvent event) {
        queue.add(event);
        if (dispatching) {
            return;
        }
        dispatching = true;
        try {
            while (!queue.isEmpty()) {
                GameEvent next = queue.poll();
                for (Consumer<? super GameEvent> listener : List.copyOf(listeners)) {
                    listener.accept(next);
                }
            }
        } finally {
            dispatching = false;
            queue.clear();
        }
    }
}

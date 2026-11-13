package cardistry.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Bus synchrone : chaque événement est livré à tous les abonnés, dans l'ordre d'abonnement. */
public final class EventBus {

    private final List<Consumer<? super GameEvent>> listeners = new ArrayList<>();

    public void subscribe(Consumer<? super GameEvent> listener) {
        listeners.add(listener);
    }

    public void publish(GameEvent event) {
        for (Consumer<? super GameEvent> listener : List.copyOf(listeners)) {
            listener.accept(event);
        }
    }
}

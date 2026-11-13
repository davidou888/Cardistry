package cardistry.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @Test
    void unEvenementPublieEnReactionEstLivreApresLEvenementEnCours() {
        EventBus bus = new EventBus();
        List<String> seen = new ArrayList<>();
        // Le premier abonné réagit à TurnStarted en publiant GameWon (comme un trinket).
        bus.subscribe(event -> {
            seen.add("A:" + event.getClass().getSimpleName());
            if (event instanceof GameEvent.TurnStarted) {
                bus.publish(new GameEvent.GameWon());
            }
        });
        bus.subscribe(event -> seen.add("B:" + event.getClass().getSimpleName()));

        bus.publish(new GameEvent.TurnStarted(1));

        assertEquals(List.of("A:TurnStarted", "B:TurnStarted", "A:GameWon", "B:GameWon"), seen);
    }

    @Test
    void uneExceptionVideLaFile() {
        EventBus bus = new EventBus();
        List<GameEvent> seen = new ArrayList<>();
        bus.subscribe(event -> {
            if (event instanceof GameEvent.TurnStarted) {
                bus.publish(new GameEvent.GameWon());
                throw new IllegalStateException("boum");
            }
            seen.add(event);
        });

        try {
            bus.publish(new GameEvent.TurnStarted(1));
        } catch (IllegalStateException expected) {
            // ok
        }
        bus.publish(new GameEvent.GameLost());

        assertEquals(List.of(new GameEvent.GameLost()), seen);
    }
}

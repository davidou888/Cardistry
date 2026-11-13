package cardistry;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.event.EventBus;
import cardistry.model.card.CardPool;
import cardistry.ui.GameLoop;
import cardistry.ui.text.Console;
import cardistry.ui.text.ConsoleInput;
import cardistry.ui.text.TextRenderer;
import java.util.List;

public final class Main {

    private Main() {
    }

    /** Options : {@code --fast} supprime les pauses (pratique pour tester). */
    public static void main(String[] args) {
        boolean fast = List.of(args).contains("--fast");

        Console console = new Console(System.in, System.out, fast);
        EventBus bus = new EventBus();
        CardPool pool = new CardPool();
        CombatEngine engine = new CombatEngine(GameState.newGame(pool), pool, bus);

        TextRenderer renderer = new TextRenderer(console);
        bus.subscribe(renderer::onEvent);

        GameLoop.run(engine, renderer, new ConsoleInput(console));
    }
}

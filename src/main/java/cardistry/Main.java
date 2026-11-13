package cardistry;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.HookDispatcher;
import cardistry.model.card.CardPool;
import cardistry.ui.GameLoop;
import cardistry.ui.QuitGameException;
import cardistry.ui.text.Console;
import cardistry.ui.text.ConsoleInput;
import cardistry.ui.text.TextRenderer;
import java.util.List;

public final class Main {

    private Main() {
    }

    /**
     * Options :
     * <ul>
     *   <li>{@code --seed N} rejoue la partie de seed N</li>
     *   <li>{@code --fast} supprime les pauses (pratique pour tester)</li>
     * </ul>
     */
    public static void main(String[] args) {
        List<String> options = List.of(args);
        boolean fast = options.contains("--fast");
        Rng rng = parseSeed(options);

        Console console = new Console(System.in, System.out, fast);
        EventBus bus = new EventBus();
        CardPool pool = new CardPool();
        CombatEngine engine = new CombatEngine(GameState.newGame(pool, rng), pool, rng, bus);

        // Les trinkets s'enregistreront ici (hooks.register(...)).
        HookDispatcher hooks = new HookDispatcher();
        bus.subscribe(hooks);

        TextRenderer renderer = new TextRenderer(console);
        bus.subscribe(renderer::onEvent);

        try {
            GameLoop.run(engine, renderer, new ConsoleInput(console));
        } catch (QuitGameException e) {
            console.println();
            console.println("Partie interrompue.");
        }
    }

    private static Rng parseSeed(List<String> options) {
        int index = options.indexOf("--seed");
        if (index < 0) {
            return Rng.withRandomSeed();
        }
        if (index + 1 >= options.size()) {
            throw new IllegalArgumentException("--seed attend un nombre");
        }
        try {
            return new Rng(Long.parseLong(options.get(index + 1)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Seed invalide : " + options.get(index + 1), e);
        }
    }
}

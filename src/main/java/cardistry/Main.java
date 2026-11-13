package cardistry;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.HookDispatcher;
import cardistry.model.card.CardPool;
import cardistry.ui.GameLoop;
import cardistry.ui.QuitGameException;
import cardistry.ui.lanterna.LanternaUi;
import cardistry.ui.text.Console;
import cardistry.ui.text.ConsoleInput;
import cardistry.ui.text.TextRenderer;
import java.io.IOException;
import java.util.List;

public final class Main {

    private Main() {
    }

    /**
     * Options :
     * <ul>
     *   <li>{@code --seed N} rejoue la partie de seed N</li>
     *   <li>{@code --fast} supprime les pauses et animations (pratique pour tester)</li>
     *   <li>{@code --no-ui} rendu texte simple au lieu du plein écran ; automatique si l'entrée
     *       n'est pas un vrai terminal (pipe, {@code ./gradlew run})</li>
     * </ul>
     */
    public static void main(String[] args) {
        List<String> options = List.of(args);
        boolean fast = options.contains("--fast");
        Rng rng = parseSeed(options);

        EventBus bus = new EventBus();
        CardPool pool = new CardPool();
        CombatEngine engine = new CombatEngine(GameState.newGame(), pool, rng, bus);

        // Les trinkets s'enregistreront ici (hooks.register(...)).
        HookDispatcher hooks = new HookDispatcher();
        bus.subscribe(hooks);

        if (!options.contains("--no-ui") && isInteractiveTerminal()) {
            runFullScreen(engine, bus, fast);
        } else {
            runText(engine, bus, fast);
        }
    }

    private static void runFullScreen(CombatEngine engine, EventBus bus, boolean fast) {
        try (LanternaUi ui = LanternaUi.open(fast)) {
            bus.subscribe(ui::onEvent);
            GameLoop.run(engine, ui, ui);
        } catch (QuitGameException e) {
            // Le joueur a quitté : l'écran est déjà restauré par close().
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le plein écran (" + e.getMessage() + "). Relance avec --no-ui.");
        }
    }

    private static void runText(CombatEngine engine, EventBus bus, boolean fast) {
        Console console = new Console(System.in, System.out, fast);
        TextRenderer renderer = new TextRenderer(console);
        bus.subscribe(renderer::onEvent);
        try {
            GameLoop.run(engine, renderer, new ConsoleInput(console));
        } catch (QuitGameException e) {
            console.println();
            console.println("Partie interrompue.");
        }
    }

    /**
     * {@code true} si stdin/stdout sont un vrai terminal. Depuis Java 22, {@code System.console()}
     * n'est plus null quand les flux sont redirigés : on interroge alors {@code isTerminal()}
     * (absent en Java 21, d'où la réflexion).
     */
    private static boolean isInteractiveTerminal() {
        java.io.Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            return (boolean) java.io.Console.class.getMethod("isTerminal").invoke(console);
        } catch (NoSuchMethodException e) {
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
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

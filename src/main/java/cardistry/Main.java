package cardistry;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.model.card.CardPool;
import cardistry.platform.Os;
import cardistry.ui.GameLoop;
import cardistry.ui.Glyphs;
import cardistry.ui.QuitGameException;
import cardistry.ui.lanterna.LanternaUi;
import cardistry.ui.text.Console;
import cardistry.ui.text.ConsoleInput;
import cardistry.ui.text.TextRenderer;
import java.awt.GraphicsEnvironment;
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
     *   <li>{@code --no-ui} rendu texte simple ; automatique si l'entrée n'est pas un vrai terminal
     *       (pipe, {@code ./gradlew run}) ou s'il n'y a pas d'affichage graphique pour la fenêtre</li>
     *   <li>{@code --window} jeu dans une fenêtre (émulateur Lanterna) ; par défaut sous Windows,
     *       où Lanterna 3.1 n'a pas de console native</li>
     *   <li>{@code --ascii} affichage en ASCII pur ; automatique si la sortie n'est pas en UTF-8</li>
     *   <li>{@code --no-color} mode texte sans couleurs ANSI ; automatique dans la vieille console
     *       Windows (hors Windows Terminal et terminaux de type Unix)</li>
     * </ul>
     */
    public static void main(String[] args) {
        List<String> options = List.of(args);
        Os os = Os.current();
        boolean fast = options.contains("--fast");
        boolean window = options.contains("--window") || os == Os.WINDOWS;
        boolean asciiRequested = options.contains("--ascii");
        boolean color = !options.contains("--no-color") && ansiColorsLikely(os);
        Rng rng = parseSeed(options);

        EventBus bus = new EventBus();
        CardPool pool = new CardPool();
        CombatEngine engine = new CombatEngine(GameState.newGame(), pool, rng, bus);

        boolean textAscii = asciiRequested || !Glyphs.unicodeLikely();
        if (options.contains("--no-ui")) {
            runText(engine, bus, fast, textAscii, color);
        } else if (window) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("Pas d'affichage graphique pour la fenêtre : mode texte.");
                runText(engine, bus, fast, textAscii, color);
            } else {
                runWindow(engine, bus, fast, asciiRequested);
            }
        } else if (isInteractiveTerminal()) {
            runFullScreen(engine, bus, fast, textAscii, color);
        } else {
            runText(engine, bus, fast, textAscii, color);
        }
    }

    private static void runFullScreen(CombatEngine engine, EventBus bus, boolean fast, boolean ascii, boolean color) {
        LanternaUi ui;
        try {
            ui = LanternaUi.open(fast, ascii);
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le plein écran (" + e.getMessage() + ") : mode texte.");
            runText(engine, bus, fast, ascii, color);
            return;
        }
        try (ui) {
            bus.subscribe(ui::onEvent);
            GameLoop.run(engine, ui, ui);
        } catch (QuitGameException e) {
            // Le joueur a quitté : l'écran est restauré par close().
        } catch (IOException e) {
            System.err.println("Erreur du terminal : " + e.getMessage());
        }
    }

    /** Fenêtre de l'émulateur Lanterna (Unicode toujours disponible) ; quitte la JVM à la fin. */
    private static void runWindow(CombatEngine engine, EventBus bus, boolean fast, boolean ascii) {
        try (LanternaUi ui = LanternaUi.openWindow(fast, ascii)) {
            bus.subscribe(ui::onEvent);
            GameLoop.run(engine, ui, ui);
        } catch (QuitGameException e) {
            // Le joueur a quitté.
        } catch (IOException e) {
            System.err.println("Erreur de la fenêtre : " + e.getMessage());
        }
        System.exit(0); // ferme les fils Swing
    }

    /**
     * Couleurs ANSI comprises ? Toujours sous Linux et macOS ; sous Windows, seulement dans Windows
     * Terminal ({@code WT_SESSION}) ou un terminal de type Unix ({@code TERM}, ex. Git Bash).
     */
    static boolean ansiColorsLikely(Os os) {
        return os != Os.WINDOWS || System.getenv("WT_SESSION") != null || System.getenv("TERM") != null;
    }

    private static void runText(CombatEngine engine, EventBus bus, boolean fast, boolean ascii, boolean color) {
        Console console = new Console(System.in, System.out, fast, ascii, color);
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

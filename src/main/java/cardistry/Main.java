package cardistry;

import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.platform.Os;
import cardistry.platform.UnlockFile;
import cardistry.run.Run;
import cardistry.run.Secret;
import cardistry.run.UnlockStore;
import cardistry.run.UnlockTracker;
import cardistry.ui.GameLoop;
import cardistry.ui.Glyphs;
import cardistry.ui.QuitGameException;
import cardistry.ui.lanterna.LanternaUi;
import cardistry.ui.text.Console;
import cardistry.ui.text.ConsoleInput;
import cardistry.ui.text.SpriteViewerText;
import cardistry.platform.SettingsFile;
import cardistry.ui.OptionsController;
import cardistry.ui.Settings;
import cardistry.ui.audio.Audio;
import cardistry.ui.audio.AudioDirector;
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
     *   <li>{@code --reset-unlocks} efface les secrets débloqués, puis lance le jeu</li>
     *   <li>{@code --sprites} visionneuse : fait défiler les sprites et leurs animations, sans partie</li>
     *   <li>{@code --fast} supprime les pauses et animations (pratique pour tester)</li>
     *   <li>par défaut : le jeu dans sa propre fenêtre (police pixel, images, sans scintillement),
     *       sur les trois OS ; {@code --window} est gardée comme alias</li>
     *   <li>{@code --terminal} plein écran dans le terminal (Lanterna)</li>
     *   <li>{@code --no-ui} rendu texte simple</li>
     *   <li>{@code --crt} filtre CRT (lignes de balayage) dans la fenêtre ; F10 le bascule</li>
     *   <li>{@code --no-images} sprites ASCII même dans la fenêtre</li>
     *   <li>{@code --mute} sans aucun son, {@code --no-music} sans musique (pour cette session : les
     *       réglages sauvegardés ne changent pas)</li>
     *   <li>sans affichage graphique : plein écran terminal si c'est un vrai terminal, sinon texte</li>
     *   <li>{@code --ascii} affichage en ASCII pur ; automatique si la sortie n'est pas en UTF-8</li>
     *   <li>{@code --no-color} mode texte sans couleurs ANSI ; automatique dans la vieille console
     *       Windows (hors Windows Terminal et terminaux de type Unix)</li>
     * </ul>
     */
    /** Comment afficher le jeu. */
    enum Mode {
        WINDOW,
        TERMINAL,
        TEXT
    }

    /**
     * Mode d'affichage : {@code --no-ui} → texte ; {@code --terminal} → plein écran terminal (texte si
     * ce n'est pas un vrai terminal) ; sinon la fenêtre, ou, sans affichage graphique, le plein écran
     * terminal (texte si ce n'est pas un vrai terminal).
     */
    static Mode mode(List<String> options, boolean headless, boolean interactiveTerminal) {
        if (options.contains("--no-ui")) {
            return Mode.TEXT;
        }
        if (options.contains("--terminal") || headless) {
            return interactiveTerminal ? Mode.TERMINAL : Mode.TEXT;
        }
        return Mode.WINDOW;
    }

    public static void main(String[] args) {
        List<String> options = List.of(args);
        Os os = Os.current();
        boolean fast = options.contains("--fast");
        boolean asciiRequested = options.contains("--ascii");
        boolean color = !options.contains("--no-color") && ansiColorsLikely(os);
        boolean textAscii = asciiRequested || !Glyphs.unicodeLikely();
        boolean headless = GraphicsEnvironment.isHeadless();
        Mode mode = mode(options, headless, isInteractiveTerminal());
        if (headless && mode != Mode.TEXT && !options.contains("--terminal") && !options.contains("--no-ui")) {
            System.err.println("Pas d'affichage graphique : plein écran dans le terminal.");
        } else if (headless && mode == Mode.TEXT && !options.contains("--no-ui")) {
            System.err.println("Pas d'affichage graphique ni de vrai terminal : mode texte.");
        }
        Display display = new Display(mode, fast, asciiRequested, textAscii, color, options.contains("--crt"),
                !options.contains("--no-images"));
        if (options.contains("--sprites")) {
            viewSprites(display);
            return;
        }
        UnlockStore store = UnlockFile.inSaveDirectory();
        if (options.contains("--reset-unlocks")) {
            System.out.println("Secrets débloqués effacés.");
        }
        Game game = prepare(options, store);
        Sound sound = Sound.open(options, SettingsFile.inSaveDirectory());
        game.bus().subscribe(sound.director()); // avant l'interface : le son part avec l'animation
        switch (mode) {
            case TEXT -> runText(game, game.bus(), fast, textAscii, color, sound);
            case TERMINAL -> runFullScreen(game, game.bus(), fast, textAscii, color, sound);
            case WINDOW -> runWindow(game, game.bus(), display, sound);
        }
        sound.director().audio().close();
    }

    /** Le son et les réglages : réglages sauvegardés, {@code --mute} / {@code --no-music} pour la session. */
    record Sound(AudioDirector director, OptionsController settings) {

        static Sound open(List<String> options, SettingsFile file) {
            Settings saved = file.load();
            Audio audio = Audio.open(saved.music(), saved.effects(), saved.muted(), options.contains("--mute"),
                    options.contains("--no-music"));
            OptionsController controller = new OptionsController() {
                private Settings current = saved;

                @Override
                public Settings settings() {
                    return current;
                }

                @Override
                public void apply(Settings settings) {
                    current = settings;
                    audio.setLevels(settings.music(), settings.effects(), settings.muted());
                    file.save(settings);
                }
            };
            return new Sound(new AudioDirector(audio), controller);
        }
    }

    /** Options d'affichage. */
    record Display(Mode mode, boolean fast, boolean asciiRequested, boolean textAscii, boolean color, boolean crt,
            boolean images) {
    }

    /** Visionneuse de sprites, dans le même affichage que le jeu (fenêtre, plein écran ou texte). */
    private static void viewSprites(Display d) {
        if (d.mode() == Mode.TEXT) {
            SpriteViewerText.run(new Console(System.in, System.out, d.fast(), d.textAscii(), d.color()));
            return;
        }
        boolean window = d.mode() == Mode.WINDOW;
        try (LanternaUi ui = window ? LanternaUi.openWindow(d.fast(), d.asciiRequested(), d.crt(), d.images())
                : LanternaUi.open(d.fast(), d.textAscii())) {
            ui.viewSprites();
        } catch (QuitGameException e) {
            // Fermé.
        } catch (IOException e) {
            System.err.println("Erreur du terminal : " + e.getMessage());
        }
        if (window) {
            System.exit(0);
        }
    }

    /** Partie prête à lancer : la partie, le suivi des déblocages et le bus de l'interface. */
    record Game(Run run, UnlockTracker tracker, EventBus bus) {
    }

    /**
     * Prépare la partie selon les options : {@code --reset-unlocks} (efface les déblocages avant de
     * les lire), {@code --seed N} ou {@code --seed null} (seed au hasard et NullPointerException
     * équipé pour cette partie seulement).
     */
    static Game prepare(List<String> options, UnlockStore store) {
        if (options.contains("--reset-unlocks")) {
            store.reset();
        }
        EventBus bus = new EventBus();
        Run run = new Run(parseSeed(options).seed(), store.load(), bus);
        if (isNullSeed(options)) {
            run.addCode(Secret.NULL_POINTER_EXCEPTION);
        }
        UnlockTracker tracker = new UnlockTracker(run::state, store, bus);
        bus.subscribe(tracker);
        return new Game(run, tracker, bus);
    }

    private static void play(Game game, cardistry.ui.Renderer renderer, cardistry.ui.Input input, Sound sound) {
        GameLoop.run(game.run(), renderer, input, game.tracker()::unlockedThisRun, sound.director());
    }

    private static void runFullScreen(Game game, EventBus bus, boolean fast, boolean ascii, boolean color, Sound sound) {
        LanternaUi ui;
        try {
            ui = LanternaUi.open(fast, ascii);
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le plein écran (" + e.getMessage() + ") : mode texte.");
            runText(game, bus, fast, ascii, color, sound);
            return;
        }
        try (ui) {
            bus.subscribe(ui::onEvent);
            ui.setSounds(sound.director()::ui);
            ui.setOptions(sound.settings());
            play(game, ui, ui, sound);
        } catch (QuitGameException e) {
            // Le joueur a quitté : l'écran est restauré par close().
        } catch (IOException e) {
            System.err.println("Erreur du terminal : " + e.getMessage());
        }
    }

    /** Fenêtre du jeu (police embarquée : Unicode toujours disponible) ; quitte la JVM à la fin. */
    private static void runWindow(Game game, EventBus bus, Display d, Sound sound) {
        try (LanternaUi ui = LanternaUi.openWindow(d.fast(), d.asciiRequested(), d.crt(), d.images())) {
            bus.subscribe(ui::onEvent);
            ui.setSounds(sound.director()::ui);
            ui.setOptions(sound.settings());
            play(game, ui, ui, sound);
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

    private static void runText(Game game, EventBus bus, boolean fast, boolean ascii, boolean color, Sound sound) {
        Console console = new Console(System.in, System.out, fast, ascii, color);
        TextRenderer renderer = new TextRenderer(console);
        bus.subscribe(renderer::onEvent);
        try {
            ConsoleInput input = new ConsoleInput(console);
            input.setOptions(sound.settings());
            play(game, renderer, input, sound);
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

    /** {@code --seed null} : code secret NullPointerException. */
    static boolean isNullSeed(List<String> options) {
        int index = options.indexOf("--seed");
        return index >= 0 && index + 1 < options.size() && options.get(index + 1).equals("null");
    }

    static Rng parseSeed(List<String> options) {
        int index = options.indexOf("--seed");
        if (index < 0 || isNullSeed(options)) {
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

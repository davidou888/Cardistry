package cardistry.platform;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Dossier de sauvegarde du jeu, selon l'OS :
 * <ul>
 *   <li>Linux (et autres Unix) : {@code $XDG_DATA_HOME/cardistry}, sinon {@code ~/.local/share/cardistry}</li>
 *   <li>macOS : {@code ~/Library/Application Support/Cardistry}</li>
 *   <li>Windows : {@code %APPDATA%\Cardistry}, sinon {@code ~\AppData\Roaming\Cardistry}</li>
 * </ul>
 * Uniquement des {@link Path} : aucun séparateur écrit en dur.
 */
public final class SaveDirectory {

    private SaveDirectory() {
    }

    /** Dossier de sauvegarde sur cette machine (n'est pas créé). */
    public static Path resolve() {
        return resolve(System.getProperty("os.name", ""), System::getenv, Path.of(System.getProperty("user.home")));
    }

    /** Version testable : nom d'OS, variables d'environnement et dossier personnel injectés. */
    static Path resolve(String osName, Function<String, String> env, Path home) {
        return switch (Os.detect(osName)) {
            case WINDOWS -> absolute(env.apply("APPDATA"))
                    .orElse(home.resolve("AppData").resolve("Roaming"))
                    .resolve("Cardistry");
            case MAC -> home.resolve("Library").resolve("Application Support").resolve("Cardistry");
            case LINUX -> absolute(env.apply("XDG_DATA_HOME"))
                    .orElse(home.resolve(".local").resolve("share"))
                    .resolve("cardistry");
        };
    }

    /** Chemin absolu lu dans une variable d'environnement ; vide si absente, vide ou relative. */
    private static java.util.Optional<Path> absolute(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            Path path = Path.of(value);
            return path.isAbsolute() ? java.util.Optional.of(path) : java.util.Optional.empty();
        } catch (InvalidPathException e) {
            return java.util.Optional.empty();
        }
    }
}

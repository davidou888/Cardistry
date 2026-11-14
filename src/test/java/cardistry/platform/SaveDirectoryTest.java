package cardistry.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests indépendants de l'OS qui les exécute : chemins construits avec Path.resolve, jamais en dur. */
class SaveDirectoryTest {

    @TempDir
    Path home;

    @TempDir
    Path elsewhere;

    private static java.util.function.Function<String, String> env(Map<String, String> values) {
        return values::get;
    }

    @Test
    void detectionDeLOs() {
        assertEquals(Os.WINDOWS, Os.detect("Windows 11"));
        assertEquals(Os.MAC, Os.detect("Mac OS X"));
        assertEquals(Os.LINUX, Os.detect("Linux"));
        assertEquals(Os.LINUX, Os.detect("FreeBSD"));
        assertEquals(Os.LINUX, Os.detect(""));
    }

    @Test
    void linuxAvecXdgDataHome() {
        Path xdg = elsewhere.resolve("data");
        assertEquals(xdg.resolve("cardistry"),
                SaveDirectory.resolve("Linux", env(Map.of("XDG_DATA_HOME", xdg.toString())), home));
    }

    @Test
    void linuxSansXdgDataHome() {
        assertEquals(home.resolve(".local").resolve("share").resolve("cardistry"),
                SaveDirectory.resolve("Linux", env(Map.of()), home));
    }

    @Test
    void linuxXdgDataHomeRelatifOuVideIgnore() {
        Path expected = home.resolve(".local").resolve("share").resolve("cardistry");
        assertEquals(expected, SaveDirectory.resolve("Linux", env(Map.of("XDG_DATA_HOME", "relatif")), home));
        assertEquals(expected, SaveDirectory.resolve("Linux", env(Map.of("XDG_DATA_HOME", "  ")), home));
    }

    @Test
    void macOs() {
        assertEquals(home.resolve("Library").resolve("Application Support").resolve("Cardistry"),
                SaveDirectory.resolve("Mac OS X", env(Map.of("XDG_DATA_HOME", elsewhere.toString())), home));
    }

    @Test
    void windowsAvecAppData() {
        Path appData = elsewhere.resolve("Roaming");
        assertEquals(appData.resolve("Cardistry"),
                SaveDirectory.resolve("Windows 10", env(Map.of("APPDATA", appData.toString())), home));
    }

    @Test
    void windowsSansAppData() {
        assertEquals(home.resolve("AppData").resolve("Roaming").resolve("Cardistry"),
                SaveDirectory.resolve("Windows 10", env(Map.of()), home));
    }

    @Test
    void surCetteMachineLeCheminEstAbsolu() {
        assertTrue(SaveDirectory.resolve().isAbsolute());
        assertTrue(SaveDirectory.resolve().getFileName().toString().equalsIgnoreCase("cardistry"));
    }
}

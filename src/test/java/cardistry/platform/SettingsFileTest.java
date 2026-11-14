package cardistry.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.ui.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsFileTest {

    @TempDir
    Path dir;

    @Test
    void absentDonneLesValeursParDefaut() {
        Settings settings = new SettingsFile(dir.resolve("settings.txt")).load();
        assertEquals(new Settings(6, 8, false), settings);
        assertEquals(Settings.defaults(), settings);
    }

    @Test
    void sauvegardePuisRelecture() throws IOException {
        SettingsFile file = new SettingsFile(dir.resolve("sous/dossier/settings.txt"));
        file.save(new Settings(3, 10, true));
        assertEquals(new Settings(3, 10, true), file.load());
        assertEquals("cardistry-settings 1\nmusic=3\neffects=10\nmuted=true\n",
                Files.readString(file.path()).replace("\r\n", "\n"));
    }

    @Test
    void fichierCorrompu() throws IOException {
        Path path = dir.resolve("settings.txt");
        Files.writeString(path, "n'importe quoi\nmusic=2\n");
        assertEquals(Settings.defaults(), new SettingsFile(path).load());
        Files.writeString(path, "cardistry-settings 1\nmusic=fort\neffects=42\nmuted=peut-être\ninconnu=1\n");
        assertEquals(new Settings(6, 10, false), new SettingsFile(path).load(), "valeurs bornées ou par défaut");
    }

    @Test
    void aCoteDesDeblocages() {
        Path home = Path.of("/home/test");
        assertEquals(UnlockFile.inSaveDirectory("Linux", k -> null, home).path().getParent(),
                SaveDirectory.resolve("Linux", k -> null, home));
        assertEquals("settings.txt", SettingsFile.FILE_NAME);
    }
}

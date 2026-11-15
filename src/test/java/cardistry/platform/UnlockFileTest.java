package cardistry.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.run.Run;
import cardistry.run.Secret;
import cardistry.run.UnlockTracker;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Sauvegarde des secrets débloqués. */
class UnlockFileTest {

    @TempDir
    Path dir;

    private UnlockFile file() {
        return new UnlockFile(dir.resolve("cardistry").resolve(UnlockFile.FILE_NAME));
    }

    @Test
    void fichierAbsentAucunSecret() {
        assertTrue(file().load().isEmpty());
    }

    @Test
    void sauvegardeRelue() {
        file().save(EnumSet.of(Secret.CTRL_Z, Secret.NULL_POINTER_EXCEPTION));
        assertEquals(EnumSet.of(Secret.CTRL_Z, Secret.NULL_POINTER_EXCEPTION), file().load());
    }

    @Test
    void formatLisible() throws IOException {
        file().save(EnumSet.of(Secret.CLAUDE_MD));
        List<String> lines = Files.readAllLines(file().path(), StandardCharsets.UTF_8);
        assertEquals(UnlockFile.HEADER, lines.get(0));
        assertTrue(lines.contains("claude-md"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("#")));
        try (var files = Files.list(file().path().getParent())) {
            assertEquals(1, files.count(), "pas de fichier temporaire oublié");
        }
    }

    @Test
    void commentairesEtLignesVidesIgnores() throws IOException {
        write(UnlockFile.HEADER + "\n\n# commentaire\n  ctrl-z  \n");
        assertEquals(Set.of(Secret.CTRL_Z), file().load());
    }

    private void write(String content) throws IOException {
        Files.createDirectories(file().path().getParent());
        Files.writeString(file().path(), content, StandardCharsets.UTF_8);
    }

    @Test
    void fichierCorrompuOnRepartDeZero() throws IOException {
        write("n'importe quoi\nctrl-z\n");
        assertTrue(file().load().isEmpty(), "en-tête manquant");

        write(UnlockFile.HEADER + "\nctrl-z\nsecret-inconnu\n");
        assertTrue(file().load().isEmpty(), "ligne inconnue");

        write("");
        assertTrue(file().load().isEmpty(), "fichier vide");

        Files.write(file().path(), new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0xFF, 0x00});
        assertTrue(file().load().isEmpty(), "octets invalides");
    }

    @Test
    void fichierCorrompuReecritAuProchainDeblocage() throws IOException {
        write("garbage");
        UnlockFile store = file();
        new UnlockTrackerHarness(store).unlockNullPointerException();
        assertEquals(Set.of(Secret.NULL_POINTER_EXCEPTION), file().load());
    }

    @Test
    void dossierALaPlaceDuFichierSansPlanter() throws IOException {
        Files.createDirectories(file().path());
        Files.writeString(file().path().resolve("autre"), "dossier non vide");
        assertTrue(file().load().isEmpty());
        file().save(EnumSet.of(Secret.CTRL_Z)); // ne lève rien
        assertTrue(file().load().isEmpty());
    }

    @Test
    void ecritureImpossibleSansPlanter() throws IOException {
        Path blocker = dir.resolve("bloque");
        Files.writeString(blocker, "un fichier, pas un dossier");
        UnlockFile store = new UnlockFile(blocker.resolve(UnlockFile.FILE_NAME));
        store.save(EnumSet.of(Secret.CTRL_Z)); // ne lève rien
        assertTrue(store.load().isEmpty());
    }

    @Test
    void resetEfface() {
        file().save(EnumSet.allOf(Secret.class));
        file().reset();
        assertFalse(Files.exists(file().path()));
        assertTrue(file().load().isEmpty());
        file().reset(); // déjà absent : rien à faire
    }

    @Test
    void persistanceEntreDeuxParties() {
        // Partie 1 : NullPointerException débloqué (combat gagné main vide).
        new UnlockTrackerHarness(file()).unlockNullPointerException();

        // Partie 2 : nouveau programme, même fichier.
        Run next = new Run(1, file().load(), new EventBus());
        assertEquals(Set.of(Secret.NULL_POINTER_EXCEPTION), next.unlockedSecrets());
    }

    // --- Dossier de sauvegarde selon l'OS (os.name simulé) ---

    private void sauvegardeRelueDans(String osName, java.util.Map<String, String> env, Path expectedDir) {
        Path home = dir.resolve("home");
        UnlockFile store = UnlockFile.inSaveDirectory(osName, env::get, home);
        assertEquals(expectedDir.resolve(UnlockFile.FILE_NAME), store.path());
        store.save(EnumSet.of(Secret.CTRL_Z));
        assertTrue(Files.isRegularFile(expectedDir.resolve(UnlockFile.FILE_NAME)));
        assertEquals(Set.of(Secret.CTRL_Z), UnlockFile.inSaveDirectory(osName, env::get, home).load());
    }

    @Test
    void sauvegardeLinuxXdg() {
        Path xdg = dir.resolve("xdg");
        sauvegardeRelueDans("Linux", java.util.Map.of("XDG_DATA_HOME", xdg.toString()), xdg.resolve("cardistry"));
    }

    @Test
    void sauvegardeLinuxSansXdg() {
        sauvegardeRelueDans("Linux", java.util.Map.of(),
                dir.resolve("home").resolve(".local").resolve("share").resolve("cardistry"));
    }

    @Test
    void sauvegardeMacOs() {
        // XDG_DATA_HOME est ignoré sous macOS.
        sauvegardeRelueDans("Mac OS X", java.util.Map.of("XDG_DATA_HOME", dir.resolve("xdg").toString()),
                dir.resolve("home").resolve("Library").resolve("Application Support").resolve("Cardistry"));
    }

    @Test
    void sauvegardeWindowsAppData() {
        Path appData = dir.resolve("AppData").resolve("Roaming");
        sauvegardeRelueDans("Windows 11", java.util.Map.of("APPDATA", appData.toString()), appData.resolve("Cardistry"));
    }

    @Test
    void sauvegardeWindowsSansAppData() {
        sauvegardeRelueDans("Windows 10", java.util.Map.of(),
                dir.resolve("home").resolve("AppData").resolve("Roaming").resolve("Cardistry"));
    }

    /** Une partie réduite où l'on gagne le premier combat avec la dernière carte. */
    private static final class UnlockTrackerHarness {

        private final UnlockFile store;

        UnlockTrackerHarness(UnlockFile store) {
            this.store = store;
        }

        void unlockNullPointerException() {
            EventBus bus = new EventBus();
            cardistry.core.stats.Stats stats = new cardistry.core.stats.Stats();
            stats.setBase(cardistry.core.stats.StatType.STARTING_HAND, 1);
            Run run = new Run(1, store.load(), bus,
                    () -> new cardistry.core.GameState(new cardistry.model.Player(100),
                            List.of(new cardistry.model.Enemy("A", cardistry.model.Element.FIRE, 10, 1),
                                    new cardistry.model.Enemy("B", cardistry.model.Element.FIRE, 10, 1)),
                            new cardistry.model.card.TerrainCard("EPFL", cardistry.model.Element.FIRE, cardistry.items.Rarity.COMMON), List.of(), stats),
                    () -> new cardistry.model.card.CardPool(List.of(
                            new cardistry.model.card.SpellCard("Boule de feu", cardistry.model.Element.FIRE, 10, cardistry.items.Rarity.COMMON))));
            UnlockTracker tracker = new UnlockTracker(run::state, store, bus);
            bus.subscribe(tracker);
            run.start();
            run.startTurn();
            run.submit(new cardistry.core.Action.PlayCard(0));
            assertEquals(List.of(Secret.NULL_POINTER_EXCEPTION), tracker.unlockedThisRun());
        }
    }
}

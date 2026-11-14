package cardistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.run.UnlockStore;
import cardistry.ui.OptionsController;
import cardistry.ui.QuitGameException;
import cardistry.ui.Settings;
import cardistry.ui.audio.Audio;
import cardistry.ui.audio.AudioDirector;
import cardistry.ui.lanterna.LanternaUi;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Enchaînement des parties : quitter une partie ramène à l'écran titre, pas hors de l'application. */
class MainSessionsTest {

    private static String screen(DefaultVirtualTerminal terminal) {
        StringBuilder out = new StringBuilder();
        for (int row = 0; row < terminal.getTerminalSize().getRows(); row++) {
            for (int col = 0; col < terminal.getTerminalSize().getColumns(); col++) {
                out.append(terminal.getCharacter(col, row).getCharacterString());
            }
            out.append('\n');
        }
        return out.toString();
    }

    @Test
    void quitterUnePartieRamèneAuTitreAvecUneNouvellePartie() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(80, 24));
        LanternaUi ui = new LanternaUi(terminal, true);
        Main.Sound sound = new Main.Sound(new AudioDirector(Audio.silent()),
                OptionsController.inMemory(new Settings(6, 8, false)));
        List<Main.Game> started = new ArrayList<>();
        java.util.function.Supplier<Main.Game> games = () -> {
            Main.Game game = Main.prepare(List.of(), new UnlockStore.InMemory());
            started.add(game);
            return game;
        };
        terminal.addInput(new KeyStroke(KeyType.Enter));        // titre : Jouer
        terminal.addInput(new KeyStroke(KeyType.Enter));        // personnage → carte de la 1re partie
        terminal.addInput(new KeyStroke('q', false, false));    // quitter la partie…
        terminal.addInput(new KeyStroke('o', false, false));    // … confirmé : retour au titre
        terminal.addInput(new KeyStroke(KeyType.Enter));        // titre : Jouer
        terminal.addInput(new KeyStroke(KeyType.Enter));        // personnage → 2e partie
        terminal.addInput(new KeyStroke(KeyType.Escape));       // pause
        terminal.addInput(new KeyStroke(KeyType.ArrowDown));
        terminal.addInput(new KeyStroke(KeyType.ArrowDown));
        terminal.addInput(new KeyStroke(KeyType.Enter));        // Quitter la partie
        terminal.addInput(new KeyStroke('o', false, false));    // retour au titre
        terminal.addInput(new KeyStroke('q', false, false));    // titre : quitter l'application

        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> Main.sessions(games, ui, sound)));

        assertEquals(3, started.size(), "une partie neuve à chaque retour au titre");
        assertNotSame(started.get(0).run(), started.get(1).run());
        assertTrue(started.get(0).run().state() != null, "la 1re partie avait commencé");
        String text = screen(terminal);
        assertTrue(text.contains("Jouer") && text.contains("Quitter"), "écran titre à la fin : " + text);
        ui.close();
    }
}

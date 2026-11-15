package cardistry.ui.text;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.GameState;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Mode texte : « d » affiche le deck à n'importe quelle saisie, puis la question est reposée. */
class ConsoleDeckTest {

    @Test
    void commandeDAfficheLeDeck() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ConsoleInput input = new ConsoleInput(new Console(new ByteArrayInputStream("d\n0\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8), true, false, false));
        GameState state = GameState.newGame();
        Action action = input.nextAction(state);
        String out = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(action instanceof Action.DrawCard);
        assertTrue(out.contains("Ton deck : 12 cartes"), out);
        assertTrue(out.contains("  ×3 Potion · soin plante · Commun : +10 PV"), out);
        assertTrue(out.indexOf("Que veux-tu jouer ? ") < out.lastIndexOf("Que veux-tu jouer ? "), "question reposée");
    }
}

package cardistry.ui.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.items.ItemRegistry;
import cardistry.run.Secret;
import cardistry.ui.TitleChoice;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mode texte : code secret sur l'écran titre, annulation. */
class ConsoleInputTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private ConsoleInput input(String typed) {
        Console console = new Console(new ByteArrayInputStream(typed.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8), true, false, false);
        return new ConsoleInput(console);
    }

    @Test
    void codeClaudeSurLEcranTitre() {
        TitleChoice choice = input("claude\ns\n").title(12);
        assertEquals(List.of(Secret.CLAUDE_MD), choice.codes());
        assertFalse(choice.showRules());
        String text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Seed : 12"));
        assertTrue(text.contains("Code accepté : CLAUDE.md"));
    }

    @Test
    void sansCode() {
        TitleChoice choice = input("\n").title(1);
        assertTrue(choice.codes().isEmpty());
        assertTrue(choice.showRules());
    }

    @Test
    void zAnnuleAvecCtrlZ() {
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new cardistry.model.card.CardPool(), new Rng(1), new EventBus());
        assertEquals(new Action.DrawCard(), input("z\n0\n").nextAction(state), "sans Ctrl+Z, z n'annule rien");
        engine.equip(ItemRegistry.trinket(ItemRegistry.CTRL_Z));
        assertEquals(new Action.Undo(), input("z\n").nextAction(state));
    }
}

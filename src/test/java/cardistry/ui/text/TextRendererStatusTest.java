package cardistry.ui.text;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.TerrainCard;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Mode texte ({@code --no-ui}) : statuts dans l'état et dans le journal, adversaire endormi. */
class TextRendererStatusTest {

    @Test
    void statutsAffichesEtJournalises() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TextRenderer renderer = new TextRenderer(new Console(new ByteArrayInputStream(new byte[0]),
                new PrintStream(bytes, true, StandardCharsets.UTF_8), true, false, false));
        GameState state = new GameState(new Player(100), List.of(new Enemy("Mannequin", Element.FIRE, 20, 5)),
                new TerrainCard("Dojo", Element.NEUTRAL, Rarity.COMMON), List.of(), new Stats());
        state.statuses(StatusTarget.ENEMY).apply(StatusType.FONS, 2, StatusTarget.PLAYER);
        state.statuses(StatusTarget.PLAYER).apply(StatusType.INTOX, 3, StatusTarget.ENEMY);
        renderer.showStatus(state);
        renderer.onEvent(new GameEvent.StatusApplied(StatusTarget.ENEMY, StatusType.FONS, 2, false));
        renderer.onEvent(new GameEvent.StatusEnded(StatusTarget.PLAYER, StatusType.INTOX));
        String out = bytes.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("Statuts : Intox (3 tours)"), out);
        assertTrue(out.contains("Statuts : Fons (2 tours)"), out);
        assertTrue(out.contains("Prochain tour : Endormi (2 tours)"), out);
        assertTrue(out.contains("Mannequin subit Fons (2 tours)"), out);
        assertTrue(out.contains("Intox de toi : terminé"), out);
    }
}

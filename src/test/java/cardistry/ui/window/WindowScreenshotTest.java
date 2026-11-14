package cardistry.ui.window;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.model.card.CardPool;
import cardistry.model.enemy.EnemyRegistry;
import cardistry.ui.lanterna.LanternaUi;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Captures du rendu fenêtre (sans écran) dans {@code build/screens/*.png}, pour relire la mise en page. */
class WindowScreenshotTest {

    static Path save(java.awt.image.BufferedImage image, String name) throws IOException {
        Path dir = Path.of("build", "screens");
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".png");
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    @Test
    void combatDansLaFenetre() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(100, 32));
        ImageLayer layer = new ImageLayer();
        LanternaUi ui = new LanternaUi(terminal, true, false, layer);
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(4), new EventBus());
        engine.start();
        engine.startCombat(EnemyRegistry.create(EnemyRegistry.VAFI, 1), false);
        ui.onEvent(new GameEvent.GameStarted(4));
        ui.showStatus(state);
        GridRenderer renderer = new GridRenderer(new CellFonts(2));
        save(renderer.render(terminal, layer.snapshot(), false), "80-fenetre-combat");
        save(renderer.render(terminal, layer.snapshot(), true), "81-fenetre-combat-crt");
        ui.close();
        assertTrue(Files.exists(Path.of("build/screens/80-fenetre-combat.png")));
    }

    private static void mapScreenshot(int cols, int rows, String name) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
        ImageLayer layer = new ImageLayer();
        LanternaUi ui = new LanternaUi(terminal, true, false, layer);
        cardistry.run.Run run = new cardistry.run.Run(42, java.util.Set.of(), new EventBus());
        run.start();
        ui.onEvent(new GameEvent.GameStarted(42));
        ui.showMap(new cardistry.run.map.MapView(run.state(), run.map().orElseThrow(), run.visited(),
                run.position(), run.choices()));
        ImageLayer.Frame frame = layer.frame();
        save(new GridRenderer(new CellFonts(2)).render(terminal, frame.images(), frame.marks(), false), name);
        ui.close();
    }

    @Test
    void carteDansLaFenetre() throws IOException {
        mapScreenshot(100, 32, "82-fenetre-carte");
        mapScreenshot(80, 24, "83-fenetre-carte-80x24");
        assertTrue(Files.exists(Path.of("build/screens/82-fenetre-carte.png")));
    }
}

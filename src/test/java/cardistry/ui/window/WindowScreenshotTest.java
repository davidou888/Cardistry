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
        save(renderer.render(terminal, layer.snapshot(), layer.marks(), false), "80-fenetre-combat");
        save(renderer.render(terminal, layer.snapshot(), layer.marks(), true), "81-fenetre-combat-crt");
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

    private static void combatScreenshot(int cols, int rows, String name, boolean journal) throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
        ImageLayer layer = new ImageLayer();
        LanternaUi ui = new LanternaUi(terminal, true, false, layer);
        GameState state = GameState.newGame();
        EventBus bus = new EventBus();
        bus.subscribe(ui::onEvent);
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(4), bus);
        engine.start();
        engine.give(cardistry.items.ItemRegistry.items().get(0));
        engine.give(cardistry.items.ItemRegistry.items().get(3));
        cardistry.items.ItemRegistry.trinkets().stream().limit(3).forEach(engine::equip);
        engine.startCombat(EnemyRegistry.create(EnemyRegistry.CADDIE, 1), false);
        ui.onEvent(new GameEvent.GameStarted(4));
        if (journal) {
            terminal.addInput(new com.googlecode.lanterna.input.KeyStroke('j', false, false));
        }
        terminal.addInput(new com.googlecode.lanterna.input.KeyStroke('p', false, false));
        ui.nextAction(state);
        ImageLayer.Frame frame = layer.frame();
        save(new GridRenderer(new CellFonts(2)).render(terminal, frame.images(), frame.marks(), false), name);
        ui.close();
    }

    @Test
    void combatRefonduDansLaFenetre() throws IOException {
        combatScreenshot(100, 32, "86-fenetre-combat-100x32", false);
        combatScreenshot(80, 24, "87-fenetre-combat-80x24", false);
        combatScreenshot(100, 32, "88-fenetre-combat-journal", true);
        assertTrue(Files.exists(Path.of("build/screens/88-fenetre-combat-journal.png")));
    }

    @Test
    void recompenseEtShopDansLaFenetre() throws IOException {
        DefaultVirtualTerminal terminal = new DefaultVirtualTerminal(new TerminalSize(100, 32));
        ImageLayer layer = new ImageLayer();
        LanternaUi ui = new LanternaUi(terminal, true, false, layer);
        GameState state = GameState.newGame();
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(4), new EventBus());
        engine.start();
        java.util.List<cardistry.items.Collectible> options = new java.util.ArrayList<>();
        for (cardistry.items.Rarity rarity : java.util.List.of(cardistry.items.Rarity.COMMON,
                cardistry.items.Rarity.RARE, cardistry.items.Rarity.LEGENDARY)) {
            options.add(cardistry.items.ItemRegistry.trinkets().stream().filter(t -> t.rarity() == rarity)
                    .findFirst().orElseThrow());
        }
        terminal.addInput(new com.googlecode.lanterna.input.KeyStroke('s', false, false));
        ui.chooseReward(state, options);
        GridRenderer renderer = new GridRenderer(new CellFonts(2));
        save(renderer.render(terminal, layer.snapshot(), layer.marks(), false), "89-fenetre-recompense");

        engine.openShop();
        terminal.addInput(new com.googlecode.lanterna.input.KeyStroke(com.googlecode.lanterna.input.KeyType.Escape));
        ui.chooseShopAction(state, engine.shop().orElseThrow(), 75);
        save(renderer.render(terminal, layer.snapshot(), layer.marks(), false), "90-fenetre-shop");
        ui.close();
        assertTrue(Files.exists(Path.of("build/screens/90-fenetre-shop.png")));
    }

    @Test
    void carteDansLaFenetre() throws IOException {
        mapScreenshot(100, 32, "82-fenetre-carte");
        mapScreenshot(80, 24, "83-fenetre-carte-80x24");
        assertTrue(Files.exists(Path.of("build/screens/82-fenetre-carte.png")));
    }
}

package cardistry.tools;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyRegistry;
import cardistry.run.Run;
import cardistry.run.map.MapNode;
import cardistry.run.map.MapView;
import cardistry.run.map.RunMap;
import cardistry.ui.lanterna.LanternaUi;
import cardistry.ui.window.CellFonts;
import cardistry.ui.window.CellWindow;
import cardistry.ui.window.GridRenderer;
import cardistry.ui.window.ImageLayer;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;

/**
 * Vérifie les animations dans la VRAIE fenêtre ({@code ./gradlew animationCheck}) : le jeu s'ouvre,
 * joue tout seul (carte, combats, journal) à vitesse normale, et un fil échantillonne ce que la
 * fenêtre affiche. La trace (image de l'adversaire, décalage, clignotement, cercles de la carte) est
 * écrite sur la sortie ; une capture par image d'adversaire distincte va dans {@code build/animation/}.
 */
public final class AnimationCheck {

    private static final long START = System.currentTimeMillis();
    private static volatile String phase = "";
    private static final java.util.Set<String> SAVED = new java.util.HashSet<>();

    private AnimationCheck() {
    }

    public static void main(String[] args) throws Exception {
        List<String> enemies = args.length > 0 ? List.of(args) : List.of(EnemyRegistry.CADDIE, EnemyRegistry.SEAU,
                EnemyRegistry.CAME, EnemyRegistry.PARESSEUX, EnemyRegistry.PENEU, EnemyRegistry.TREX,
                EnemyRegistry.ETUDIANT, EnemyRegistry.VAFI);
        CellWindow window = CellWindow.open("Cardistry — vérification des animations", false);
        DefaultVirtualTerminal terminal = (DefaultVirtualTerminal) window.terminal();
        LanternaUi ui = new LanternaUi(terminal, false, false, window.images());
        AtomicBoolean running = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> sample(terminal, window.images(), running), "sampler");
        sampler.setDaemon(true);
        sampler.start();
        try {
            map(ui, terminal);
            boolean journal = true;
            for (String id : enemies) {
                combat(ui, terminal, id, journal);
                journal = false;
            }
        } finally {
            running.set(false);
            sampler.join(500);
            ui.close();
            window.close();
        }
        System.exit(0);
    }

    private static void log(String text) {
        System.out.printf("%6d ms  %-22s %s%n", System.currentTimeMillis() - START, phase, text);
    }

    private static void later(DefaultVirtualTerminal terminal, int millis, KeyStroke key) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                return;
            }
            terminal.addInput(key);
        });
        t.setDaemon(true);
        t.start();
    }

    private static KeyStroke ch(char c) {
        return new KeyStroke(c, false, false);
    }

    private static void map(LanternaUi ui, DefaultVirtualTerminal terminal) {
        phase = "carte";
        Run run = new Run(42, java.util.Set.of(), new EventBus());
        run.start();
        ui.onEvent(new GameEvent.GameStarted(42));
        RunMap map = run.map().orElseThrow();
        MapNode first = map.floor(1).get(1);
        MapNode second = map.choicesAfter(first).get(0);
        later(terminal, 1800, new KeyStroke(KeyType.ArrowDown));
        later(terminal, 3200, new KeyStroke(KeyType.Enter));
        int chosen = ui.chooseNode(new MapView(run.state(), map, List.of(first, second), second,
                map.choicesAfter(second)));
        log("nœud choisi : " + chosen);
    }

    private static void combat(LanternaUi ui, DefaultVirtualTerminal terminal, String id, boolean journal) {
        phase = id;
        Enemy enemy = EnemyRegistry.create(id, 1);
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hand.add(new SpellCard("Tsunami", Element.WATER, 12));
        }
        GameState state = new GameState(new Player(100), List.of(enemy), new TerrainCard("EPFL", Element.FIRE), hand,
                new Stats());
        EventBus bus = new EventBus();
        // Avant l'interface : l'événement est noté, puis son animation se joue.
        bus.subscribe(event -> {
            String name = event.getClass().getSimpleName();
            if (event instanceof GameEvent.DamageDealt d) {
                name += d.target() instanceof Enemy ? " (adversaire)" : " (joueur)";
            }
            log("ÉVÉNEMENT " + name);
        });
        bus.subscribe(ui::onEvent);
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), bus);
        engine.start();
        int turns = 0;
        while (!engine.isOver() && turns++ < 30) {
            if (journal && turns == 7) {
                journal(terminal);
            }
            later(terminal, turns == 1 ? 1300 : journal && turns == 7 ? 2400 : 250,
                    ch(state.hand().isEmpty() ? 'p' : '1'));
            Action action = ui.nextAction(state);
            engine.submit(action);
        }
        log(engine.isOver() ? (state.isWon() ? "adversaire vaincu" : "joueur vaincu") : "arrêt après 30 tours");
    }

    /** J, ↑, ↑, ↓, J : la ligne du haut du journal est relevée après chaque touche. */
    private static void journal(DefaultVirtualTerminal terminal) {
        KeyStroke[] keys = {ch('j'), new KeyStroke(KeyType.ArrowUp), new KeyStroke(KeyType.ArrowUp),
            new KeyStroke(KeyType.ArrowDown), ch('j')};
        String[] names = {"J (ouvrir)", "↑", "↑", "↓", "J (fermer)"};
        for (int i = 0; i < keys.length; i++) {
            later(terminal, 300 + i * 400, keys[i]);
            int index = i;
            Thread reader = new Thread(() -> {
                try {
                    Thread.sleep(300 + index * 400 + 200);
                } catch (InterruptedException e) {
                    return;
                }
                log("JOURNAL après " + names[index] + " : « " + row(terminal, 3).strip() + " » / bas : « "
                        + row(terminal, terminal.getTerminalSize().getRows() - 6).strip() + " »");
            });
            reader.setDaemon(true);
            reader.start();
        }
    }

    private static String row(DefaultVirtualTerminal terminal, int row) {
        StringBuilder out = new StringBuilder();
        for (int col = 2; col < terminal.getTerminalSize().getColumns() - 2; col++) {
            out.append(terminal.getCharacter(col, row).getCharacterString());
        }
        return out.toString();
    }

    /** Relève toutes les 10 ms l'image de l'adversaire, celle du joueur et les cercles de la carte. */
    private static void sample(DefaultVirtualTerminal terminal, ImageLayer layer, AtomicBoolean running) {
        String last = "";
        GridRenderer renderer = new GridRenderer(new CellFonts(2));
        new File("build/animation").mkdirs();
        while (running.get()) {
            ImageLayer.Frame frame = layer.frame();
            StringBuilder now = new StringBuilder();
            String enemyPath = null;
            for (ImageLayer.Placement p : frame.images()) {
                if (p.path().startsWith("enemies/")) {
                    enemyPath = p.path();
                    now.append(p.path().substring(8)).append(" col=").append(p.col())
                            .append(p.tint() != null ? " BLANC" : "").append(p.alpha() < 1 ? " alpha=" + p.alpha() : "");
                } else if (p.path().startsWith("player/") && p.tint() != null) {
                    now.append(" | joueur clignote");
                }
            }
            long rings = frame.marks().stream().filter(m -> m instanceof ImageLayer.Fill f && f.width() == 2
                    && f.height() == 2 && f.color().getBlue() > 200).mapToLong(m -> ((ImageLayer.Fill) m).x() * 31L
                            + ((ImageLayer.Fill) m).y()).sum();
            if (rings != 0) {
                now.append("cercles de la carte #").append(Long.toHexString(rings));
            }
            if (phase.equals("carte") ? rings != 0 : true) {
                String text = now.toString();
                if (!text.equals(last)) {
                    last = text;
                    log(text.isEmpty() ? "(aucune image d'adversaire)" : text);
                    if (enemyPath != null && SAVED.add(enemyPath)) {
                        try {
                            ImageIO.write(renderer.render(terminal, frame.images(), frame.marks(), false), "png",
                                    new File("build/animation/" + enemyPath.substring(8)));
                        } catch (Exception e) {
                            log("capture impossible : " + e);
                        }
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}

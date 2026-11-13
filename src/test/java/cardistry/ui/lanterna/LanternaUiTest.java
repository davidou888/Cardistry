package cardistry.ui.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.items.Collectible;
import cardistry.items.Item;
import cardistry.items.ItemRegistry;
import cardistry.items.Trinket;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.EnemyRoster;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.ui.GameLoop;
import cardistry.ui.QuitGameException;
import cardistry.ui.Renderer;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * L'interface est testée sur un terminal virtuel. Chaque écran testé est aussi écrit dans
 * {@code build/screens/} pour pouvoir relire la mise en page.
 */
class LanternaUiTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private DefaultVirtualTerminal terminal;
    private LanternaUi ui;

    private void open(int cols, int rows) throws IOException {
        terminal = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
        ui = new LanternaUi(terminal, true);
    }

    @AfterEach
    void close() throws IOException {
        if (ui != null) {
            ui.close();
        }
    }

    private String screen() {
        TerminalSize size = terminal.getTerminalSize();
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < size.getRows(); row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < size.getColumns(); col++) {
                line.append(terminal.getCharacter(col, row).getCharacterString());
            }
            text.append(line.toString().stripTrailing()).append('\n');
        }
        return text.toString();
    }

    private String dump(String name) throws IOException {
        String text = screen();
        Path dir = Path.of("build", "screens");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(name + ".txt"), text);
        return text;
    }

    private void type(String keys) {
        for (char c : keys.toCharArray()) {
            terminal.addInput(new KeyStroke(c, false, false));
        }
    }

    private void press(KeyType type) {
        terminal.addInput(new KeyStroke(type));
    }

    private static GameState combat(List<Card> hand, Enemy... enemies) {
        return new GameState(new Player(100), List.of(enemies), new TerrainCard("EPFL", Element.FIRE), hand, new Stats());
    }

    // --- Titre ---

    @Test
    void titreEntreeLanceSansIntro() throws IOException {
        open(80, 24);
        ui.onEvent(new GameEvent.GameStarted(42));
        press(KeyType.Enter);

        assertFalse(assertTimeoutPreemptively(TIMEOUT, ui::wantsIntro));

        String text = dump("01-titre");
        assertTrue(text.contains("[Entrée] Jouer"));
        assertTrue(text.contains("Seed 42"));
    }

    @Test
    void titreRAfficheLesRegles() throws IOException {
        open(80, 24);
        type("r");
        assertTrue(assertTimeoutPreemptively(TIMEOUT, ui::wantsIntro));

        GameState state = GameState.newGame();
        press(KeyType.Enter);
        assertTimeoutPreemptively(TIMEOUT, () -> ui.showIntro(state));

        String text = dump("02-regles");
        assertTrue(text.contains("Hydre"));
        assertTrue(text.contains("Sirène"));
        assertTrue(text.contains("Goblin"));
    }

    @Test
    void titreQQuitte() throws IOException {
        open(80, 24);
        type("q");
        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(TIMEOUT, ui::wantsIntro));
    }

    // --- Combat ---

    @Test
    void ecranDeCombat() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(
                new SpellCard("Jet d'eau", Element.WATER, 10),
                new SpellCard("Pluie de flammes", Element.FIRE, 15),
                new HealCard("Caïpirinha", Element.WATER, 10),
                new TerrainCard("Marécage", Element.WATER)),
                EnemyRoster.createDefault().toArray(Enemy[]::new));
        ui.onEvent(new GameEvent.GameStarted(42));
        ui.showStatus(state);

        String text = dump("03-combat");
        assertTrue(text.contains("HYDRE"));
        assertTrue(text.contains("Combat 1/3"));
        assertTrue(text.contains("Attaque 5 ×1.25 = 6.25"));
        assertTrue(text.contains("EPFL (feu)"));
        assertTrue(text.contains("100/100"));
        assertTrue(text.contains("Ta main 4/7"));
        assertTrue(text.contains("Jet d'eau"));
        assertTrue(text.contains("Caïpiri-"));
        assertTrue(text.contains("vide"));
        // Détail de la carte sélectionnée (la première) avec son multiplicateur réel.
        assertTrue(text.contains("10 ×1.5 = 15 dégâts à Hydre"));
        assertTrue(text.contains("P piocher"));
    }

    @Test
    void flechesPuisEntreeJoueLaCarteSelectionnee() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(
                new TerrainCard("A", Element.FIRE), new TerrainCard("B", Element.WATER),
                new TerrainCard("C", Element.NATURE)), hydra());
        press(KeyType.ArrowRight);
        press(KeyType.ArrowRight);
        press(KeyType.Enter);

        assertEquals(new Action.PlayCard(2), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void flecheGaucheBoucle() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new TerrainCard("A", Element.FIRE), new TerrainCard("B", Element.WATER)), hydra());
        press(KeyType.ArrowLeft);
        press(KeyType.Enter);

        assertEquals(new Action.PlayCard(1), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void chiffresEtPioche() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new TerrainCard("A", Element.FIRE)), hydra());
        type("3p0");

        assertEquals(new Action.PlayCard(2), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        assertEquals(new Action.DrawCard(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        assertEquals(new Action.DrawCard(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void quitterDemandeConfirmation() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new TerrainCard("A", Element.FIRE)), hydra());
        type("qn1");
        assertEquals(new Action.PlayCard(0), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));

        type("qo");
        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void terminalTropPetit() throws IOException {
        open(60, 20);
        ui.showStatus(combat(List.of(), hydra()));
        String text = dump("05-trop-petit");
        assertTrue(text.contains("Terminal trop petit : 60×20"));
    }

    @Test
    void grandTerminalAgranditLeJournal() throws IOException {
        open(120, 40);
        GameState state = combat(List.of(new SpellCard("Jet d'eau", Element.WATER, 10)), hydra());
        ui.showStatus(state);
        for (int i = 1; i <= 15; i++) {
            ui.onEvent(new GameEvent.TurnStarted(i));
        }
        ui.showHand(state);
        String text = dump("06-grand-terminal");
        assertTrue(text.contains("— Tour 1 —"));
        assertTrue(text.contains("— Tour 15 —"));
    }

    // --- Partie complète ---

    @Test
    void partieCompleteJusquAEcranDeFin() throws IOException {
        open(80, 24);
        Rng rng = new Rng(7);
        CardPool pool = new CardPool();
        EventBus bus = new EventBus();
        CombatEngine engine = new CombatEngine(GameState.newGame(), pool, rng, bus);
        bus.subscribe(ui::onEvent);

        press(KeyType.Enter); // titre
        for (int i = 0; i < 200; i++) {
            type("1ps"); // jouer la 1re carte, piocher, refuser les récompenses
        }

        // Capture l'écran de combat au tour 3.
        Renderer recorder = new Renderer() {
            @Override public void onEvent(GameEvent event) { }
            @Override public void showIntro(GameState state) { ui.showIntro(state); }
            @Override public void showStatus(GameState state) { ui.showStatus(state); }
            @Override public void showHand(GameState state) {
                ui.showHand(state);
                if (state.turn() == 3) {
                    try {
                        dump("04-combat-tour-3");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            @Override public void showGameOver(GameState state) { ui.showGameOver(state); }
        };

        assertTimeoutPreemptively(TIMEOUT, () -> GameLoop.run(engine, recorder, ui));

        assertTrue(engine.isOver());
        String text = dump("07-fin");
        assertTrue(text.contains(engine.state().isWon() ? "VICTOIRE" : "DÉFAITE"));
        assertTrue(text.contains("--seed 7"));
    }

    // --- Objets, trinkets, récompenses ---

    private static Item mate() {
        return ItemRegistry.items().get(0);
    }

    private static Trinket briquets() {
        return ItemRegistry.trinkets().get(0);
    }

    /** État avec inventaire : passe par le moteur pour équiper. */
    private static GameState withInventory(List<Item> items, List<Trinket> trinkets) {
        GameState state = combat(List.of(new SpellCard("Jet d'eau", Element.WATER, 10)), hydra());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus());
        items.forEach(engine::give);
        trinkets.forEach(engine::equip);
        return state;
    }

    @Test
    void combatAfficheObjetsEtTrinkets() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(mate()), List.of(briquets()));
        ui.showStatus(state);
        String text = dump("08-combat-inventaire");
        assertTrue(text.contains("[A] Maté"));
        assertTrue(text.contains("[Z] ·"));
        assertTrue(text.contains("Trinkets 1/5"));
        assertTrue(text.contains("Redistribution des briquets"));
        assertTrue(text.contains("A/Z/E objets"));
        assertTrue(text.contains("Q quitter ─"), "l'aide tient dans le cadre");
    }

    @Test
    void toucheAUtiliseLePremierObjet() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(mate()), List.of());
        type("a");
        assertEquals(new Action.UseItem(0), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void toucheIOuvreLInventaire() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(mate()), List.of(briquets()));
        Action[] result = new Action[1];
        Thread player = new Thread(() -> result[0] = ui.nextAction(state));
        player.setDaemon(true);
        player.start();
        type("i");

        waitFor(() -> screen().contains("Inventaire"));
        String text = dump("09-inventaire");
        type("x");   // retour au combat
        type("p");   // puis piocher
        player.join(TIMEOUT.toMillis());

        assertTrue(text.contains("Maté"));
        assertTrue(text.contains("+25 % dégâts feu"));
        assertEquals(new Action.DrawCard(), result[0]);
    }

    @Test
    void ecranDeRecompensePrendreLaDeuxieme() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        List<Collectible> options = List.of(mate(), briquets(), mate());
        press(KeyType.ArrowRight);
        press(KeyType.Enter);

        RewardChoice choice = assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseReward(state, options));

        assertEquals(RewardChoice.Take.into(1), choice);
    }

    @Test
    void ecranDeRecompenseRenduPuisRefus() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        List<Collectible> options = List.of(mate(), briquets());
        RewardChoice[] result = new RewardChoice[1];
        Thread player = new Thread(() -> result[0] = ui.chooseReward(state, options));
        player.setDaemon(true);
        player.start();

        waitFor(() -> screen().contains("Choisis une récompense"));
        String text = dump("10-recompense");
        type("s");
        player.join(TIMEOUT.toMillis());

        assertTrue(text.contains("Objet · Commun"));
        assertTrue(text.contains("Trinket · Commun"));
        assertTrue(text.contains("Maté"));
        assertTrue(text.contains("S refuser"));
        assertEquals(new RewardChoice.Skip(), result[0]);
    }

    @Test
    void inventairePleinDemandeQuoiRemplacer() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(mate(), mate(), mate()), List.of());
        List<Collectible> options = List.of(mate());
        press(KeyType.Enter);          // prendre → inventaire plein → écran de remplacement
        press(KeyType.ArrowRight);     // 2e objet
        press(KeyType.Enter);

        assertEquals(new RewardChoice.Take(0, 1), assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseReward(state, options)));
    }

    private void waitFor(java.util.function.BooleanSupplier condition) {
        long end = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > end) {
                throw new AssertionError("Écran attendu non affiché :\n" + screen());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static Enemy hydra() {
        return new Enemy("Hydre", Element.FIRE, 20, 5);
    }
}

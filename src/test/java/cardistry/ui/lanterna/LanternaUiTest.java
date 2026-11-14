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
        assertTrue(text.contains("[W] Maté"));
        assertTrue(text.contains("[E] ·"));
        assertTrue(text.contains("Trinkets 1/5"));
        assertTrue(text.contains("Redistribution des briquets"));
        assertTrue(text.contains("W/E/R objets"));
        assertTrue(text.contains("Q quitter ─"), "l'aide tient dans le cadre");
    }

    @Test
    void toucheWUtiliseLePremierObjet() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(mate()), List.of());
        type("w");
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

        // La ligne d'aide du bas est écrite en dernier : l'écran est alors complet.
        waitFor(() -> screen().contains("Appuie sur une touche pour revenir"));
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

        waitFor(() -> screen().contains("S refuser · Q quitter"));
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

    @Test
    void inventairePleinTientALEcran() throws IOException {
        open(80, 24);
        GameState state = withInventory(
                List.of(ItemRegistry.item("cafffeeeeinnnn"), ItemRegistry.item("gaz-du-bat-inf"), mate()),
                List.of(ItemRegistry.trinket("latte-macchiato-vanille"), ItemRegistry.trinket("comprehension-algebrique"),
                        ItemRegistry.trinket("george"), ItemRegistry.trinket("cms"), ItemRegistry.trinket("pax")));
        ui.showStatus(state);
        String text = dump("11-inventaire-plein");
        assertTrue(text.contains("[W] "));
        assertTrue(text.contains("[E] "));
        assertTrue(text.contains("[R] Maté"));
        assertTrue(text.contains("Trinkets 5/5"));
    }

    @Test
    void recompenseAvecDescriptionLongue() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        List<Collectible> options = List.of(ItemRegistry.trinket("george"),
                ItemRegistry.trinket("comprehension-algebrique"), ItemRegistry.item("gaz-du-bat-inf"));
        Thread player = new Thread(() -> ui.chooseReward(state, options));
        player.setDaemon(true);
        player.start();
        waitFor(() -> screen().contains("S refuser · Q quitter"));
        String text = dump("12-recompense-george");
        type("s");
        player.join(TIMEOUT.toMillis());
        assertTrue(text.contains("George"));
        assertTrue(text.contains("Légendaire"));
        assertTrue(text.contains("║pool "), "description complète (dernier mot)");
        assertTrue(text.contains("biens communs"), "phrase d'ambiance");
    }

    @Test
    void ivresseEtGueuleDeBoisAffichees() throws IOException {
        open(80, 24);
        Enemy first = hydra();
        GameState state = new GameState(new Player(100), List.of(first, new Enemy("Sirène", Element.WATER, 30, 10),
                new Enemy("Goblin", Element.NATURE, 40, 15)), new TerrainCard("EPFL", Element.FIRE),
                List.of(new SpellCard("Tsunami", Element.WATER, 100)), new Stats());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(),
                new cardistry.items.LootTable(List::of));
        for (int i = 0; i < 3; i++) {
            engine.give(ItemRegistry.item("spritz"));
            engine.submit(new Action.UseItem(0));
        }
        ui.showStatus(state);
        String during = dump("13-ivresse");
        assertTrue(during.contains("Ivresse 3/3"));
        assertTrue(during.contains("Gueule de bois à venir"));

        engine.submit(new Action.PlayCard(0)); // tue l'Hydre → combat suivant avec Gueule de bois
        ui.showStatus(state);
        String after = dump("14-gueule-de-bois");
        assertTrue(after.contains("Gueule de bois (−1 taille de main)"));
        assertTrue(after.contains("Ta main 5/6"));
    }

    @Test
    void journalDistingueCarteEtObjetHomonymes() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        ui.showStatus(state);
        ui.onEvent(new GameEvent.CardPlayed(new cardistry.model.card.HealCard("Petit pétou", Element.FIRE, 15)));
        ui.onEvent(new GameEvent.ItemUsed(ItemRegistry.item("petit-petou")));
        ui.showHand(state);
        String text = screen();
        assertTrue(text.contains("Tu joues la carte Petit pétou"));
        assertTrue(text.contains("Tu utilises l'objet Petit pétou"));
    }

    @Test
    void effetsEnCoursAffichesAvecLeurDuree() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(),
                new cardistry.items.LootTable(List::of));
        for (String id : List.of("shot-de-jager", "space-cake", "moscow-mule")) {
            engine.give(ItemRegistry.item(id));
            engine.submit(new Action.UseItem(0));
        }
        ui.showStatus(state);
        String text = dump("15-effets");
        // Trop long en version complète : version courte, objets d'abord.
        assertTrue(text.contains("Shot de Jäger · Space cake (dans 3) · +1"), text); // Moscow Mule ne tient pas

        // L'inventaire liste tout, avec la durée.
        Thread player = new Thread(() -> ui.nextAction(state));
        player.setDaemon(true);
        player.start();
        type("i");
        waitFor(() -> screen().contains("Appuie sur une touche pour revenir"));
        String inventory = dump("16-inventaire-effets");
        type("xqo");
        player.join(TIMEOUT.toMillis());
        assertTrue(inventory.contains("Shot de Jäger : prochain sort ×2"));
        assertTrue(inventory.contains("Space cake : dans 3 tours"));
        assertTrue(inventory.contains("Moscow Mule : annule la prochaine attaque"));
    }

    @Test
    void effetSeulEnVersionLongue() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(),
                new cardistry.items.LootTable(List::of));
        engine.give(ItemRegistry.item("shot-de-jager"));
        engine.submit(new Action.UseItem(0));
        ui.showStatus(state);
        assertTrue(screen().contains("Shot de Jäger : prochain sort ×2"));
    }

    @Test
    void paquetDeClopesJaugeHonoreEtPvAuDessusDuMax() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(ItemRegistry.item("spritz")),
                List.of(ItemRegistry.trinket("honore"), ItemRegistry.trinket("ross")));
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(),
                new cardistry.items.LootTable(List::of));
        engine.submit(new Action.UseItem(0));   // Spritz ×2 sans plafond → 160 PV
        engine.startTurn();
        engine.startTurn();                      // Honoré 2/5
        engine.submit(new Action.Smoke());       // 4 clopes, −5 PV
        ui.showStatus(state);
        String text = dump("17-clopes");
        assertTrue(text.contains("4/20 clopes"), text);
        assertTrue(text.contains("Honoré [##...]"), text);
        assertTrue(text.contains("F fumer · G rouler"));
        assertTrue(text.contains("155/100"), text);
        assertTrue(text.contains("┌╥──"), "1 bâton sur 5 pour 4 clopes sur 20");
    }

    @Test
    void toucheFFumeEtGRoule() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        type("fg");
        assertEquals(new Action.Smoke(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        assertEquals(new Action.RollCigarette(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void setsDansLInventaireEtNotification() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(),
                new cardistry.items.LootTable(List::of));
        ui.showStatus(state);
        engine.equip(ItemRegistry.trinket("diplome-epfl"));
        // L'UI n'est pas abonnée au bus de ce moteur : on lui envoie l'événement de palier directement.
        ui.onEvent(new GameEvent.SetTierActivated("EPFL « Cursus »", cardistry.items.Tag.EPFL, 2, "+10 % dégâts élémentaires"));
        engine.equip(ItemRegistry.trinket("pichet-de-sat"));
        engine.equip(ItemRegistry.trinket("pax"));
        ui.showHand(state);
        String combat = dump("18-notification-set");
        assertTrue(combat.contains("★ Set EPFL « Cursus » 2 activé"), combat);

        Thread player = new Thread(() -> ui.nextAction(state));
        player.setDaemon(true);
        player.start();
        type("i");
        waitFor(() -> screen().contains("Appuie sur une touche pour revenir"));
        String inventory = dump("19-inventaire-sets");
        type("xqo");
        player.join(TIMEOUT.toMillis());
        assertTrue(inventory.contains("EPFL « Cursus » (2) ×2 : palier 2 · prochain 3 : +1 taille de main"), inventory);
        assertTrue(inventory.contains("+25 % dégâts eau (effet ×2)"), inventory);
        assertTrue(inventory.contains("Soirée « Before → Afterparty » (1) : aucun palier · prochain 2"), inventory);
    }

    @Test
    void ecranDeDefausseObligatoire() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of(ItemRegistry.trinket("pax"), ItemRegistry.trinket("ross")));
        int[] result = {-1};
        Thread player = new Thread(() -> result[0] = ui.chooseTrinketToDiscard(state));
        player.setDaemon(true);
        player.start();
        waitFor(() -> screen().contains("↑/↓ choisir · Entrée jeter"));
        String text = dump("20-defausse");
        press(KeyType.Escape);        // pas d'échappatoire
        press(KeyType.ArrowDown);
        press(KeyType.Enter);
        player.join(TIMEOUT.toMillis());
        assertTrue(text.contains("choisis-en un à jeter"), text);
        assertTrue(text.contains("▶ Pax"), text);
        assertEquals(1, result[0]);
    }

    @Test
    void ecranDePiocheAuChoix() throws Exception {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of());
        List<cardistry.model.card.Card> options = List.of(new SpellCard("Jet d'eau", Element.WATER, 10),
                new cardistry.model.card.HealCard("Petit pétou", Element.FIRE, 15), new TerrainCard("Forêt", Element.NATURE));
        int[] first = {-1};
        Thread player = new Thread(() -> first[0] = ui.chooseDrawnCard(state, options));
        player.setDaemon(true);
        player.start();
        waitFor(() -> screen().contains("les autres sont défaussées"));
        String text = dump("21-pioche-au-choix");
        type("3");
        player.join(TIMEOUT.toMillis());

        assertEquals(2, first[0]);
        assertTrue(text.contains("Choisis la carte à garder"), text);
        assertTrue(text.contains("Un jeu de cartes dans ton jeu de cartes."), text);
        assertTrue(text.contains("10 ×1.5 = 15 dégâts à Hydre"), "effet réel de la sélectionnée");

        // Deuxième fois : plus de phrase méta ; flèches + Entrée.
        int[] second = {-1};
        Thread again = new Thread(() -> second[0] = ui.chooseDrawnCard(state, options));
        again.setDaemon(true);
        again.start();
        waitFor(() -> screen().contains("les autres sont défaussées"));
        String secondScreen = screen();
        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        again.join(TIMEOUT.toMillis());
        assertEquals(1, second[0]);
        assertTrue(!secondScreen.contains("Un jeu de cartes dans ton jeu de cartes."), "phrase méta seulement la 1re fois");
    }

    @Test
    void modeAsciiPurSurTousLesEcrans() throws Exception {
        terminal = new DefaultVirtualTerminal(new TerminalSize(80, 24));
        ui = new LanternaUi(terminal, true, true);
        GameState state = withInventory(List.of(ItemRegistry.item("caipirinha")), List.of(ItemRegistry.trinket("diplome-epfl")));
        ui.onEvent(new GameEvent.GameStarted(7));
        ui.showStatus(state);
        String combat = dump("22-ascii-combat");
        assertTrue(combat.chars().allMatch(c -> c < 128), combat);
        assertTrue(combat.contains("+- Cardistry -"), combat);
        assertTrue(combat.contains("####################"), "barres de PV");
        assertTrue(combat.contains("Caipirinha"), "accents retirés");

        type("q");
        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(TIMEOUT, ui::wantsIntro));
        String title = dump("23-ascii-titre");
        assertTrue(title.chars().allMatch(c -> c < 128), title);
        assertTrue(title.contains("###"), "bannière en #");
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

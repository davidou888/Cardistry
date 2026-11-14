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
import cardistry.items.Rarity;
import cardistry.items.Trinket;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.run.Run;
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
        press(KeyType.Enter);

        assertFalse(assertTimeoutPreemptively(TIMEOUT, () -> ui.title(42)).showRules());

        String text = dump("01-titre");
        assertTrue(text.contains("▶ Jouer"));
        assertTrue(text.contains("Options") && text.contains("Quitter"));
        assertTrue(text.contains("Seed 42"));
    }

    @Test
    void titreRAfficheLesRegles() throws IOException {
        open(80, 24);
        type("r");
        assertTrue(assertTimeoutPreemptively(TIMEOUT, () -> ui.title(1)).showRules());

        GameState state = GameState.newGame();
        press(KeyType.Enter);
        assertTimeoutPreemptively(TIMEOUT, () -> ui.showIntro(state));

        String text = dump("02-regles");
        assertTrue(text.contains("8 étages"));
        assertTrue(text.contains("prochain coup"));
    }

    @Test
    void titreQQuitte() throws IOException {
        open(80, 24);
        type("q");
        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(TIMEOUT, () -> ui.title(1)));
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
                new Enemy("Mannequin", Element.FIRE, 20, 5), new Enemy("Bouée", Element.WATER, 30, 10),
                new Enemy("Pantin", Element.NATURE, 40, 15));
        ui.onEvent(new GameEvent.GameStarted(42));
        ui.showStatus(state);

        String text = dump("03-combat");
        assertTrue(text.contains("MANNEQUIN"));
        assertTrue(text.contains("Combat 1/3"));
        assertTrue(text.contains("Prochain : Attaque : 6.25 (×1.25)"));
        assertTrue(text.contains("EPFL (feu)"));
        assertTrue(text.contains("100/100"));
        assertTrue(text.contains("Ta main 4/7"));
        assertTrue(text.contains("Jet d'eau"));
        assertTrue(text.contains("Caïpiri-"));
        assertTrue(text.contains("vide"));
        // Détail de la carte sélectionnée (la première) avec son multiplicateur réel.
        assertTrue(text.contains("10 ×1.5 = 15 dégâts à Mannequin"));
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
        EventBus bus = new EventBus();
        Run engine = new Run(7, java.util.Set.of(), bus);
        bus.subscribe(ui::onEvent);

        press(KeyType.Enter); // titre
        for (int i = 0; i < 3000; i++) {
            type("12ps"); // 1er nœud, 1re carte (ou 2e choix d'un événement), piocher, refuser les récompenses
            press(KeyType.Escape); // quitter le shop (ailleurs : demande de confirmation…
            type("n"); // … refusée)
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
            @Override public void showGameOver(GameState state, List<String> secrets) { ui.showGameOver(state, secrets); }
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
        GameState state = new GameState(new Player(100), List.of(first, new Enemy("Bouée", Element.WATER, 30, 10),
                new Enemy("Pantin", Element.NATURE, 40, 15)), new TerrainCard("EPFL", Element.FIRE),
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

        engine.submit(new Action.PlayCard(0)); // tue le Mannequin → combat suivant avec Gueule de bois
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
        assertTrue(text.contains("10 ×1.5 = 15 dégâts à Mannequin"), "effet réel de la sélectionnée");

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
        assertThrows(QuitGameException.class, () -> assertTimeoutPreemptively(TIMEOUT, () -> ui.title(1)));
        String title = dump("23-ascii-titre");
        assertTrue(title.chars().allMatch(c -> c < 128), title);
        assertTrue(title.contains("###"), "bannière en #");
    }

    // --- Secrets ---

    private void ctrl(char c) {
        terminal.addInput(new KeyStroke(c, true, false));
    }

    @Test
    void titreCodesSecrets() throws IOException {
        open(80, 24);
        ctrl('z');
        type("xclaude");
        ctrl('z'); // déjà pris : pas de doublon
        press(KeyType.Enter);
        cardistry.ui.TitleChoice choice = assertTimeoutPreemptively(TIMEOUT, () -> ui.title(3));
        assertEquals(List.of(cardistry.run.Secret.CTRL_Z, cardistry.run.Secret.CLAUDE_MD), choice.codes());
        assertFalse(choice.showRules());
        String text = dump("24-titre-codes");
        assertTrue(text.contains("Code accepté : Ctrl+Z équipé pour cette partie"), text);
        assertTrue(text.contains("Code accepté : CLAUDE.md équipé pour cette partie"), text);
    }

    @Test
    void titreCodeIncompletIgnore() throws IOException {
        open(80, 24);
        type("claud cloude");
        press(KeyType.Enter);
        assertTrue(assertTimeoutPreemptively(TIMEOUT, () -> ui.title(3)).codes().isEmpty());
        assertFalse(dump("24b-titre-sans-code").contains("Code accepté"));
    }

    @Test
    void ctrlZEnCombatDemandeLAnnulation() throws IOException {
        open(80, 24);
        GameState state = withInventory(List.of(), List.of(ItemRegistry.trinket(ItemRegistry.CTRL_Z)));
        ctrl('z');
        assertEquals(new Action.Undo(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        String text = dump("25-combat-ctrl-z");
        assertTrue(text.contains("Ctrl+Z annuler"), text);
        assertTrue(text.contains("Q quitter ─"), "l'aide tient dans le cadre");
    }

    @Test
    void ctrlZDisponiblePuisUtiliseAffiche() throws IOException {
        open(80, 24);
        EventBus bus = new EventBus();
        Run run = new Run(1, java.util.Set.of(), bus);
        run.addCode(cardistry.run.Secret.CTRL_Z);
        run.start();
        run.chooseNode(0);
        run.startTurn();
        ui.showStatus(run.state());
        String available = dump("29-ctrl-z-disponible");
        assertTrue(available.contains("Ctrl+Z : disponible"), available);

        run.submit(new Action.DrawCard());
        run.startTurn();
        run.submit(new Action.Undo());
        ui.showStatus(run.state());
        String used = dump("30-ctrl-z-utilise");
        assertTrue(used.contains("Ctrl+Z : utilisé"), used);
    }

    @Test
    void sansCtrlZPasDEtat() throws IOException {
        open(80, 24);
        ui.showStatus(withInventory(List.of(), List.of()));
        assertFalse(screen().contains("Ctrl+Z :"));
    }

    @Test
    void carteMorteAffichee() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new cardistry.model.card.DeadCard(), new SpellCard("Jet d'eau", Element.WATER, 10)),
                hydra());
        ui.showHand(state);
        String text = dump("26-no-cards");
        assertTrue(text.contains("NO CARDS"), text);
        assertTrue(text.contains("bloquée"), text);
        assertTrue(text.contains("injouable jusqu"), text);
    }

    @Test
    void ecranDeFinSecretDebloque() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(), hydra());
        state.player().takeDamage(1000);
        ui.showGameOver(state, List.of("Ctrl+Z"));
        String text = dump("27-fin-secret");
        assertTrue(text.contains("Secret débloqué : Ctrl+Z"), text);
        assertTrue(text.contains("--seed"), text);
    }

    @Test
    void ecranDeFinSansSecret() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(), hydra());
        ui.showGameOver(state, List.of());
        assertFalse(dump("28-fin-sans-secret").contains("Secret"));
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
        return new Enemy("Mannequin", Element.FIRE, 20, 5);
    }

    // --- Sprites ---

    @Test
    void visionneuseDeSprites() throws IOException {
        open(80, 24);
        press(KeyType.ArrowRight);
        type("2");
        type("q");
        assertTimeoutPreemptively(TIMEOUT, () -> ui.viewSprites());
        String text = dump("40-sprites");
        assertTrue(text.contains("Sprites"));
        assertTrue(text.contains("Choomah"), text); // 2e sprite du registre
        assertTrue(text.contains("sprites/choomah.txt"));
        assertTrue(text.contains("1 idle"));
    }

    @Test
    void spriteEnCouleurDansLeCombat() throws IOException {
        open(80, 24);
        Enemy caddie = cardistry.model.enemy.EnemyRegistry.create(cardistry.model.enemy.EnemyRegistry.CADDIE, 1);
        GameState state = combat(List.of(new SpellCard("Jet d'eau", Element.WATER, 10)), caddie);
        ui.onEvent(new GameEvent.GameStarted(42));
        ui.showStatus(state);
        String text = dump("41-combat-caddie");
        assertTrue(text.contains("(O)      (O)"), text);
        assertTrue(text.contains("Prochain : Roue voilée : 6"));
        int row = text.lines().toList().indexOf(text.lines().filter(l -> l.contains("(O)")).findFirst().orElseThrow());
        int col = text.lines().toList().get(row).indexOf("(O)") + 1;
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.YELLOW_BRIGHT,
                terminal.getCharacter(col, row).getForegroundColor());
    }

    // --- Carte ---

    private static cardistry.run.map.MapView mapView() {
        Run run = new Run(4, java.util.Set.of(), new EventBus());
        run.start();
        return new cardistry.run.map.MapView(run.state(), run.map().orElseThrow(), run.visited(), run.position(),
                run.choices());
    }

    @Test
    void ecranDeLaCarte() throws IOException {
        open(80, 24);
        cardistry.run.map.MapView view = mapView();
        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        int chosen = assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseNode(view));
        assertEquals(1 % view.choices().size(), chosen);
        ui.showMap(view);
        String text = dump("50-carte");
        assertTrue(text.contains("Carte · Niveau 1/3"), text);
        assertTrue(text.contains("Étage 0/8"));
        assertTrue(text.contains("Légende"));
        assertTrue(text.contains("[C]"), "nœud sélectionné");
        assertTrue(text.contains("B"), "boss");
        assertTrue(text.contains("15 Choomies"));
        assertTrue(text.contains("→ Étage 1 : Combat"));
    }

    @Test
    void carteChiffreDirect() throws IOException {
        open(80, 24);
        cardistry.run.map.MapView view = mapView();
        type("2");
        assertEquals(1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseNode(view)));
    }

    // --- Deck, récompense de carte, Sieste ---

    private static GameState deckState() {
        GameState state = GameState.newGame();
        cardistry.core.CombatEngine engine = new cardistry.core.CombatEngine(state, new cardistry.model.card.CardPool(),
                new cardistry.core.Rng(1), new EventBus());
        engine.start();
        engine.startCombat(cardistry.model.enemy.EnemyRegistry.create(cardistry.model.enemy.EnemyRegistry.CAME, 1), false);
        return state;
    }

    @Test
    void piocheEtDefausseAffichees() throws IOException {
        open(80, 24);
        ui.showStatus(deckState());
        String text = dump("60-combat-deck");
        assertTrue(text.contains("Ta main 5/7 · Pioche 7 · Défausse 0"), text);
    }

    @Test
    void recompenseDeCarte() throws IOException {
        open(80, 24);
        GameState state = deckState();
        List<Card> options = List.of(new SpellCard("Tsunami", Element.WATER, 15), new HealCard("Potion", Element.NATURE, 10),
                new TerrainCard("Forêt", Element.NATURE));
        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        assertEquals(1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseCardReward(state, options)));
        String text = dump("61-recompense-carte");
        assertTrue(text.contains("Ajoute une carte à ton deck"), text);
        assertTrue(text.contains("Deck 12 cartes"));
        type("s");
        assertEquals(-1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseCardReward(state, options)));
    }

    @Test
    void siesteEtTri() throws IOException {
        open(80, 24);
        GameState state = deckState();
        press(KeyType.ArrowDown);
        press(KeyType.ArrowDown);
        press(KeyType.Enter);
        List<cardistry.run.RestOption> options = List.of(cardistry.run.RestOption.values());
        assertEquals(cardistry.run.RestOption.REMOVE_CARD,
                assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseRest(state, options)));
        String rest = dump("62-sieste");
        assertTrue(rest.contains("Sieste"), rest);
        assertTrue(rest.contains("Dormir : +30 % de tes PV max"));
        assertTrue(rest.contains("Faire le tri"));

        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        assertEquals(1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseCardToRemove(state)));
        String deck = dump("63-tri");
        assertTrue(deck.contains("Choisis la carte à retirer"), deck);
        assertTrue(deck.contains("Boule de feu"));
    }

    @Test
    void resumeDuDeck() {
        List<Card> deck = List.of(new SpellCard("Tsunami", Element.WATER, 15), new HealCard("Potion", Element.NATURE, 10),
                new SpellCard("Tsunami", Element.WATER, 15));
        assertEquals("Tsunami ×2, Potion", ItemScreens.deckSummary(deck));
    }

    // --- Shop ---

    /** Partie avec un shop ouvert et {@code choomies} Choomies ; {@code items} objets en inventaire. */
    private static cardistry.core.CombatEngine shopEngine(int choomies, int items) {
        GameState state = GameState.newGame();
        cardistry.core.CombatEngine engine = new cardistry.core.CombatEngine(state, new cardistry.model.card.CardPool(),
                new cardistry.core.Rng(3), new EventBus());
        engine.start();
        engine.gainChoomies(choomies - state.choomies());
        for (int i = 0; i < items; i++) {
            engine.give(mate());
        }
        engine.openShop();
        return engine;
    }

    @Test
    void shopAffichePrixEtServices() throws IOException {
        open(80, 24);
        cardistry.core.CombatEngine engine = shopEngine(15, 0);
        GameState state = engine.state();
        cardistry.core.Shop shop = engine.shop().orElseThrow();
        press(KeyType.ArrowDown); // vers les objets
        press(KeyType.Enter);
        cardistry.core.shop.ShopAction action = assertTimeoutPreemptively(TIMEOUT,
                () -> ui.chooseShopAction(state, shop, 75));
        assertEquals(cardistry.core.shop.ShopAction.Buy.of(cardistry.core.Shop.CARDS), action);
        String text = dump("70-shop");
        assertTrue(text.contains("Shop"), text);
        assertTrue(text.contains("15 Choomies"));
        assertTrue(text.contains("Entre, entre"));
        assertTrue(text.contains("Objets") && text.contains("Trinkets"));
        assertTrue(text.contains("[R] Reroll : 20"));
        assertTrue(text.contains("[T] Retirer une carte : 75"));
        // 15 Choomies : tout est trop cher, prix grisés.
        String[] lines = text.split("\n");
        int prices = 0;
        for (int row = 0; row < lines.length; row++) {
            int col = lines[row].indexOf(" Ch.");
            while (col > 0) {
                assertEquals(Palette.DIM, terminal.getCharacter(col - 1, row).getForegroundColor(), lines[row]);
                prices++;
                col = lines[row].indexOf(" Ch.", col + 1);
            }
        }
        assertEquals(9, prices);
    }

    @Test
    void shopPrixAbordablesEnCouleur() throws IOException {
        open(80, 24);
        cardistry.core.CombatEngine engine = shopEngine(1000, 0);
        press(KeyType.Escape); // l'écran du shop reste affiché après le départ
        assertEquals(new cardistry.core.shop.ShopAction.Leave(), assertTimeoutPreemptively(TIMEOUT,
                () -> ui.chooseShopAction(engine.state(), engine.shop().orElseThrow(), 75)));
        String text = screen();
        int row = 0;
        for (String line : text.split("\n")) {
            int col = line.indexOf(" Ch.");
            if (col > 0) {
                assertEquals(Palette.ACCENT, terminal.getCharacter(col - 1, row).getForegroundColor(), line);
            }
            row++;
        }
    }

    @Test
    void shopRerollEtRetrait() throws IOException {
        open(80, 24);
        cardistry.core.CombatEngine engine = shopEngine(500, 0);
        GameState state = engine.state();
        cardistry.core.Shop shop = engine.shop().orElseThrow();
        type("r");
        assertEquals(new cardistry.core.shop.ShopAction.Reroll(),
                assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseShopAction(state, shop, 75)));
        type("t");
        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        assertEquals(new cardistry.core.shop.ShopAction.RemoveCard(1),
                assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseShopAction(state, shop, 75)));
    }

    @Test
    void shopInventairePleinEcranDeRemplacement() throws IOException {
        open(80, 24);
        cardistry.core.CombatEngine engine = shopEngine(1000, 3);
        GameState state = engine.state();
        cardistry.core.Shop shop = engine.shop().orElseThrow();
        press(KeyType.ArrowDown); // premier objet
        press(KeyType.Enter); // inventaire plein : écran de remplacement
        press(KeyType.ArrowRight);
        press(KeyType.Enter);
        assertEquals(new cardistry.core.shop.ShopAction.Buy(cardistry.core.Shop.CARDS, 1),
                assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseShopAction(state, shop, 75)));
        String text = dump("71-shop-remplacer");
        assertTrue(text.contains("Que remplace"), text);
    }

    // --- Images (fenêtre) ---

    private cardistry.ui.window.ImageLayer layer;

    private void openWithImages(int cols, int rows) throws IOException {
        terminal = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
        layer = new cardistry.ui.window.ImageLayer();
        ui = new LanternaUi(terminal, true, false, layer);
    }

    private static Enemy withImage() {
        return Enemy.builder("test-image", "Imagé", Element.FIRE, cardistry.model.enemy.EnemyRank.COMMON).hp(20)
                .behavior(cardistry.model.enemy.EnemyBehavior.always(cardistry.model.enemy.EnemyMove.basicAttack(5)))
                .build();
    }

    @Test
    void imageDeLAdversaireALaPlaceDuSpriteAscii() throws IOException {
        openWithImages(80, 24);
        ui.showStatus(combat(List.of(), withImage()));
        cardistry.ui.window.ImageLayer.Placement image = placed("enemies/test-image.idle.1.png");
        CombatWindowScreen.Layout l = CombatWindowScreen.Layout.of(80, 24);
        assertEquals(80 - 3 - 14, image.col(), "à droite");
        assertEquals(14, image.cols());
        assertEquals(7, image.rows());
        assertEquals(l.spriteTop(), image.row());
        assertFalse(screen().contains("? ?"), "pas de sprite ASCII (inconnu) dessous");
        cardistry.ui.window.ImageLayer.Placement player = placed(CombatWindowScreen.PLAYER_IMAGE);
        assertEquals(3, player.col(), "le joueur à gauche, en face");
        assertEquals(image.row(), player.row());
        assertEquals(image.cols(), player.cols());
    }

    @Test
    void sansImageLeSpriteAscii() throws IOException {
        openWithImages(80, 24);
        ui.showStatus(combat(List.of(), hydra())); // pas d'image pour cet adversaire
        assertTrue(layer.snapshot().stream().noneMatch(p -> p.path().startsWith("enemies/")));
        assertTrue(screen().contains("? ?"), "sprite inconnu.txt");
    }

    @Test
    void sansCalqueJamaisDImage() throws IOException {
        open(80, 24); // terminal ou --no-images : pas de calque
        ui.showStatus(combat(List.of(), withImage()));
        assertTrue(screen().contains("? ?"));
    }

    @Test
    void carteEnPixelsDansLaFenetre() throws IOException {
        openWithImages(80, 24);
        cardistry.run.map.MapView view = mapView();
        ui.showMap(view);
        String text = screen();
        assertTrue(text.contains("Carte · Niveau 1/3"), text);
        assertTrue(text.contains("15 Choomies"));
        assertTrue(text.contains("→ Étage 1 : Combat — Un adversaire commun."), text);
        assertFalse(text.contains("Légende"), "plus de panneau latéral");
        assertTrue(text.contains("Sieste") && text.contains("Boss"), "légende sur une ligne");
        List<cardistry.ui.window.ImageLayer.Mark> marks = layer.marks();
        long icons = marks.stream().filter(m -> m instanceof cardistry.ui.window.ImageLayer.Stamp).count();
        assertEquals(view.map().nodes().size(), icons, "une icône par nœud");
        assertTrue(marks.stream().anyMatch(m -> m instanceof cardistry.ui.window.ImageLayer.Stamp s
                && s.path().equals("map/boss.png")));
        assertTrue(marks.stream().filter(m -> m instanceof cardistry.ui.window.ImageLayer.Fill).count() > 100,
                "chemins en pointillés");
        assertEquals(cardistry.run.map.NodeType.values().length, layer.snapshot().size(), "icônes de la légende");
    }

    @Test
    void carteEnPixelsEstompeCeQuiNEstPlusAtteignable() throws IOException {
        openWithImages(100, 32);
        Run run = new Run(4, java.util.Set.of(), new EventBus());
        run.start();
        cardistry.run.map.RunMap map = run.map().orElseThrow();
        cardistry.run.map.MapNode start = map.floor(1).get(0);
        cardistry.run.map.MapView view = new cardistry.run.map.MapView(run.state(), map, List.of(start), start,
                map.choicesAfter(start));
        ui.showMap(view);
        java.util.Set<cardistry.run.map.MapNode> reachable = MapScreen.reachable(view);
        assertTrue(reachable.contains(map.boss()));
        long faded = layer.marks().stream().filter(m -> m instanceof cardistry.ui.window.ImageLayer.Stamp s
                && s.alpha() < 0.3f).count();
        assertEquals(map.nodes().size() - reachable.size(), faded);
        assertTrue(faded > 0, "les autres départs ne sont plus atteignables");
    }

    private cardistry.ui.window.ImageLayer.Placement placed(String path) {
        return layer.snapshot().stream().filter(p -> p.path().equals(path)).findFirst()
                .orElseThrow(() -> new AssertionError("image absente : " + path + " dans " + layer.snapshot()));
    }

    private static java.awt.Color rgb(com.googlecode.lanterna.TextColor color) {
        return cardistry.ui.window.CellPalette.foreground(color);
    }

    // --- Combat dans la fenêtre ---

    @Test
    void combatFenetreTientEn80x24() {
        CombatWindowScreen.Layout l = CombatWindowScreen.Layout.of(80, 24);
        assertEquals(6, l.cardHeight());
        assertEquals(11, l.separator());
        assertEquals(7, l.spriteRows());
        assertEquals(22, l.lastActionRow());
        assertEquals(l.lastActionRow() - 1, l.barRow() + 1, "barre sur 2 lignes, puis la dernière action");
        assertTrue(l.infoWidth() >= 18, "PV et états à côté du sprite");
        assertTrue(l.playerCol() + l.spriteCols() + 2 + l.infoWidth() <= l.enemyCol() - 2 - l.infoWidth(),
                "les deux blocs d'état ne se touchent pas");
        CombatWindowScreen.Layout big = CombatWindowScreen.Layout.of(100, 32);
        assertEquals(8, big.cardHeight());
        assertTrue(big.spriteRows() > l.spriteRows(), "le sprite profite de la place");
        assertTrue(big.playerCol() + big.spriteCols() + 2 + big.infoWidth() <= big.enemyCol() - 2 - big.infoWidth());
    }

    @Test
    void combatFenetreJoueurAGaucheAdversaireADroite() throws IOException {
        openWithImages(80, 24);
        GameState state = combat(List.of(new SpellCard("Tsunami", Element.WATER, 15),
                new HealCard("Potion", Element.NATURE, 10), new TerrainCard("Forêt", Element.NATURE)), withImage());
        ui.showStatus(state);
        String text = dump("84-fenetre-combat-texte");
        List<String> lines = text.lines().toList();
        String info = lines.stream().filter(line -> line.contains("TOI")).findFirst().orElseThrow();
        assertTrue(info.indexOf("TOI") < 40 && info.indexOf("IMAGÉ") > 40, info);
        assertTrue(text.contains("100/100") && text.contains("20/20"), text);
        assertTrue(text.contains("■ feu"));
        assertTrue(text.contains("Terrain : EPFL (feu)"));
        assertFalse(text.contains("Journal ─"), "journal fermé par défaut");
        assertTrue(text.contains("J journal"));
        assertTrue(lines.get(2).contains(cardistry.ui.IntentText.intent(state, state.currentEnemy())),
                "intent au-dessus de l'adversaire");
        assertEquals("ui/intent-attack.png", CombatWindowScreen.intentIcon(state.currentEnemy()));
        assertEquals(2, placed("ui/intent-attack.png").row());
    }

    @Test
    void cartesIllustreesAvecIconeDeTypeEtCouleurDElement() throws IOException {
        openWithImages(80, 24);
        ui.showStatus(combat(List.of(new SpellCard("Tsunami", Element.WATER, 15),
                new HealCard("Potion", Element.NATURE, 10), new TerrainCard("Forêt", Element.NATURE),
                new cardistry.model.card.DeadCard()), hydra()));
        List<cardistry.ui.window.ImageLayer.Placement> images = layer.snapshot();
        for (String type : List.of("spell", "heal", "terrain", "dead")) {
            List<cardistry.ui.window.ImageLayer.Placement> ofType = images.stream()
                    .filter(p -> p.path().equals("ui/type-" + type + ".png")).toList();
            assertEquals(2, ofType.size(), type + " : illustration générique + icône de type");
            assertEquals(1, ofType.stream().filter(p -> p.tint() == null && p.rows() == 1).count());
        }
        // Sans images/cards/<id>.png : l'icône de type en grand, à la couleur de l'élément.
        assertEquals(rgb(Palette.element(Element.WATER)), images.stream()
                .filter(p -> p.path().equals("ui/type-spell.png") && p.rows() == 2).findFirst().orElseThrow().tint());
        assertEquals(rgb(Palette.element(Element.NATURE)), images.stream()
                .filter(p -> p.path().equals("ui/type-heal.png") && p.rows() == 2).findFirst().orElseThrow().tint());
        String text = screen();
        assertTrue(text.contains("Tsunami") && text.contains("15") && text.contains("+10"), text);
        assertEquals("", CombatWindowScreen.value(new TerrainCard("Forêt", Element.NATURE)));
        assertEquals("ui/type-dead.png", CombatWindowScreen.typeIcon(new cardistry.model.card.DeadCard()));
    }

    @Test
    void barreDIconesAuxCouleursDeRarete() throws IOException {
        openWithImages(100, 32);
        Trinket rare = ItemRegistry.trinkets().stream().filter(t -> t.rarity() == Rarity.RARE).findFirst().orElseThrow();
        Trinket legendary = ItemRegistry.trinkets().stream().filter(t -> t.rarity() == Rarity.LEGENDARY).findFirst()
                .orElseThrow();
        GameState state = withInventory(List.of(mate()), List.of(rare, legendary));
        ui.showStatus(state);
        List<cardistry.ui.window.ImageLayer.Mark> marks = layer.marks();
        java.util.Set<java.awt.Color> frames = new java.util.HashSet<>();
        for (cardistry.ui.window.ImageLayer.Mark mark : marks) {
            if (mark instanceof cardistry.ui.window.ImageLayer.Fill fill) {
                frames.add(fill.color());
            }
        }
        assertTrue(frames.contains(rgb(Palette.rarity(mate().rarity()))), "cadre de l'objet");
        assertTrue(frames.contains(rgb(Palette.rarity(Rarity.RARE))), "cadre rare");
        assertTrue(frames.contains(rgb(Palette.rarity(Rarity.LEGENDARY))), "cadre légendaire");
        List<String> icons = marks.stream().filter(m -> m instanceof cardistry.ui.window.ImageLayer.Stamp)
                .map(m -> ((cardistry.ui.window.ImageLayer.Stamp) m).path()).toList();
        assertEquals(List.of("items/mate.png", "ui/trinket.png", "ui/trinket.png"), icons,
                "icône de l'objet, placeholder pour les trinkets sans image");
        String text = screen();
        assertTrue(text.contains("Maté") && text.contains("[W]"), text);
        assertFalse(text.contains(rare.name()), "trinkets en icônes seules");
        assertTrue(text.contains("5/20 clopes") && text.contains("F fumer · G rouler"));
        assertEquals("ui/pack.png", placed("ui/pack.png").path());
    }

    @Test
    void couleursDeRarete() {
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.WHITE_BRIGHT, Palette.rarity(Rarity.COMMON));
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.GREEN_BRIGHT, Palette.rarity(Rarity.UNCOMMON));
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.BLUE_BRIGHT, Palette.rarity(Rarity.RARE));
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.YELLOW_BRIGHT, Palette.rarity(Rarity.LEGENDARY));
        assertEquals(com.googlecode.lanterna.TextColor.ANSI.MAGENTA_BRIGHT, Palette.rarity(Rarity.SECRET));
    }

    @Test
    void jaugeDeTrinketACoteDesClopes() {
        cardistry.items.Gauge gauge = new cardistry.items.Gauge("Honoré", 5);
        gauge.add(3);
        assertEquals("Honoré [###..]", CombatWindowScreen.gaugeText(gauge));
    }

    @Test
    void journalFermePuisOuvertAvecJ() throws IOException {
        openWithImages(80, 24);
        GameState state = combat(List.of(new SpellCard("Tsunami", Element.WATER, 15)), withImage());
        ui.onEvent(new GameEvent.TurnStarted(1));
        ui.onEvent(new GameEvent.CardPlayed(state.hand().get(0)));
        type("j");
        type("p");
        assertEquals(new Action.DrawCard(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        String open = dump("85-fenetre-journal");
        assertTrue(open.contains("Journal") && open.contains("J fermer"), open);
        assertTrue(open.contains("— Tour 1 —"), "tout le journal, pas seulement la dernière ligne");
        assertTrue(layer.snapshot().stream().noneMatch(p -> p.path().startsWith("enemies/")
                || p.path().startsWith("player/")), "aucune image sous le panneau");
        type("j");
        type("p");
        assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state));
        String closed = screen();
        assertFalse(closed.contains("— Tour 1 —"));
        assertTrue(closed.contains("› Tu joues la carte Tsunami"), "dernière action sur une ligne : " + closed);
        assertTrue(closed.contains("J journal"));
        assertEquals("enemies/test-image.idle.1.png", placed("enemies/test-image.idle.1.png").path());
    }

    @Test
    void journalToucheJSansEffetEnTerminal() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new SpellCard("Tsunami", Element.WATER, 15)), hydra());
        type("j");
        type("p");
        assertEquals(new Action.DrawCard(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        assertTrue(screen().contains("Journal"), "le terminal garde son journal toujours visible");
    }

    /** Dessine avec un Painter muni d'un calque, et renvoie les images posées. */
    private static List<cardistry.ui.window.ImageLayer.Placement> drawn(java.util.function.Consumer<Painter> draw) {
        DefaultVirtualTerminal t = new DefaultVirtualTerminal(new TerminalSize(80, 30));
        cardistry.ui.window.ImageLayer images = new cardistry.ui.window.ImageLayer();
        images.begin();
        draw.accept(new Painter(t.newTextGraphics(), new TerminalSize(80, 30), false, images));
        images.commit();
        return images.snapshot();
    }

    @Test
    void iconesDObjets() {
        GameState state = withInventory(List.of(mate()), List.of());
        List<cardistry.ui.window.ImageLayer.Placement> inventory = drawn(p -> ItemScreens.inventory(p, state));
        assertEquals(1, inventory.size());
        assertEquals("items/mate.png", inventory.get(0).path());
        assertEquals(Painter.ICON_WIDTH, inventory.get(0).cols());
        assertEquals(1, inventory.get(0).rows());
        List<cardistry.ui.window.ImageLayer.Placement> reward = drawn(p -> ItemScreens.reward(p, state,
                List.of(mate(), ItemRegistry.trinket("pax")), 0, 1));
        assertEquals(1, reward.size(), "seul Maté a une icône");
        assertTrue(drawn(p -> ItemScreens.inventory(p, withInventory(List.of(), List.of()))).isEmpty());
    }

    // --- Événements ---

    @Test
    void ecranDEvenementEtResultat() throws IOException {
        open(80, 24);
        GameState state = GameState.newGame(); // 15 Choomies : le paquet de la machine est trop cher
        cardistry.story.Story machine = cardistry.story.StoryRegistry.byId("machine-a-clopes").orElseThrow();
        type("1"); // indisponible : ignoré
        press(KeyType.ArrowDown);
        press(KeyType.Enter);
        assertEquals(1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseStory(state, machine)));
        String text = dump("90-evenement");
        assertTrue(text.contains("Machine à clopes"), text);
        assertTrue(text.contains("[1] Acheter un paquet"));
        assertTrue(text.contains("(il te faut 20 Choomies)"));
        assertTrue(text.contains("[2] Taper dessus"));
        assertTrue(text.contains("[3] Partir"));

        press(KeyType.Enter);
        assertTimeoutPreemptively(TIMEOUT, () -> ui.showStoryResult(state,
                new GameEvent.StoryResolved("Machine à clopes", "Taper dessus", List.of("Deux clopes tombent ! (+2)"))));
        String result = dump("91-evenement-resultat");
        assertTrue(result.contains("Tu choisis : Taper dessus"), result);
        assertTrue(result.contains("Deux clopes tombent !"));
    }

    // --- Menu pause, options, sons d'interface ---

    @Test
    void titreOptionsPuisJouer() throws IOException {
        open(80, 24);
        cardistry.ui.OptionsController settings = cardistry.ui.OptionsController.inMemory(cardistry.ui.Settings.defaults());
        ui.setOptions(settings);
        press(KeyType.ArrowRight);
        press(KeyType.ArrowRight); // Options
        press(KeyType.Enter);
        press(KeyType.ArrowLeft); // musique 6 → 5
        press(KeyType.ArrowDown);
        press(KeyType.ArrowRight); // effets 8 → 9
        press(KeyType.ArrowDown);
        press(KeyType.Enter); // muet
        press(KeyType.Escape); // retour au titre
        press(KeyType.ArrowLeft);
        press(KeyType.ArrowLeft); // Jouer
        press(KeyType.Enter);
        assertFalse(assertTimeoutPreemptively(TIMEOUT, () -> ui.title(1)).showRules());
        assertEquals(new cardistry.ui.Settings(5, 9, true), settings.settings());
    }

    @Test
    void ecranOptions() throws IOException {
        open(80, 24);
        ui.setOptions(cardistry.ui.OptionsController.inMemory(new cardistry.ui.Settings(6, 8, false)));
        press(KeyType.ArrowDown);
        press(KeyType.ArrowDown);
        press(KeyType.Enter); // Options
        // Capture pendant l'écran Options : Échap (retour au titre) puis Échap (quitter le jeu).
        press(KeyType.Escape);
        press(KeyType.Escape);
        assertThrows(cardistry.ui.QuitGameException.class,
                () -> assertTimeoutPreemptively(TIMEOUT, () -> ui.title(1)));
        String text = MenuScreensProbe.options(new cardistry.ui.Settings(6, 8, false), 0);
        assertTrue(text.contains("Musique  ◀ ██████░░░░ ▶ 6/10"), text);
        assertTrue(text.contains("Effets   ◀ ████████░░ ▶ 8/10"));
        assertTrue(text.contains("Muet     [ ] non"));
        assertTrue(text.contains("Retour"));
    }

    /** Rendu d'un écran de menu dans un terminal virtuel, en texte. */
    static final class MenuScreensProbe {
        static String options(cardistry.ui.Settings settings, int selected) {
            DefaultVirtualTerminal t = new DefaultVirtualTerminal(new TerminalSize(80, 24));
            MenuScreens.options(new Painter(t.newTextGraphics(), new TerminalSize(80, 24)), settings, selected, 1);
            StringBuilder text = new StringBuilder();
            for (int row = 0; row < 24; row++) {
                for (int col = 0; col < 80; col++) {
                    text.append(t.getCharacter(col, row).getCharacterString());
                }
                text.append('\n');
            }
            return text.toString();
        }
    }

    @Test
    void echapOuvreLaPauseEnCombatPuisReprendre() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(new SpellCard("Jet d'eau", Element.WATER, 10)), hydra());
        press(KeyType.Escape); // pause
        press(KeyType.Enter); // Reprendre
        type("p");
        assertEquals(new Action.DrawCard(), assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
    }

    @Test
    void pauseOptionsPuisQuitter() throws IOException {
        open(80, 24);
        GameState state = combat(List.of(), hydra());
        cardistry.ui.OptionsController settings = cardistry.ui.OptionsController.inMemory(cardistry.ui.Settings.defaults());
        ui.setOptions(settings);
        press(KeyType.Escape); // pause
        press(KeyType.ArrowDown);
        press(KeyType.Enter); // Options
        press(KeyType.ArrowRight); // musique 7
        press(KeyType.Escape); // retour à la pause
        press(KeyType.ArrowDown);
        press(KeyType.Enter); // Quitter
        type("o"); // confirmer
        assertThrows(cardistry.ui.QuitGameException.class,
                () -> assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state)));
        assertEquals(7, settings.settings().music());
    }

    @Test
    void echapAnnuleDAbordDansLesSousEcrans() throws IOException {
        open(80, 24);
        GameState state = deckState();
        // « Faire le tri » : Échap annule (−1), sans ouvrir la pause.
        press(KeyType.Escape);
        assertEquals(-1, (int) assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseCardToRemove(state)));
        // Remplacement d'objet : Échap revient à la récompense, puis S refuse.
        GameState full = withInventory(List.of(mate(), mate(), mate()), List.of());
        press(KeyType.Enter); // inventaire plein → remplacement
        press(KeyType.Escape); // retour à la récompense (pas la pause)
        type("s");
        assertEquals(new RewardChoice.Skip(),
                assertTimeoutPreemptively(TIMEOUT, () -> ui.chooseReward(full, List.of(mate()))));
    }

    @Test
    void sonsDesTouches() throws IOException {
        open(80, 24);
        List<cardistry.ui.audio.UiSound> played = new java.util.ArrayList<>();
        ui.setSounds(played::add);
        GameState state = combat(List.of(new SpellCard("A", Element.WATER, 10), new SpellCard("B", Element.WATER, 10)),
                hydra());
        press(KeyType.ArrowRight);
        press(KeyType.Escape);
        press(KeyType.Escape);
        press(KeyType.Enter);
        assertTimeoutPreemptively(TIMEOUT, () -> ui.nextAction(state));
        assertEquals(List.of(cardistry.ui.audio.UiSound.CURSOR, cardistry.ui.audio.UiSound.BACK,
                cardistry.ui.audio.UiSound.BACK, cardistry.ui.audio.UiSound.CONFIRM), played);
    }
}

package cardistry.ui.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.GameState;
import cardistry.core.stats.Stats;
import cardistry.items.Collectible;
import cardistry.items.ItemRegistry;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Textes trop longs : jamais coupés pour de bon, ils passent à la ligne ou défilent par pages. */
class LongTextTest {

    /** Rendu texte de {@code draw} dans un terminal {@code cols}×{@code rows}, une seule ligne. */
    private static String render(int cols, int rows, Consumer<Painter> draw) {
        DefaultVirtualTerminal t = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
        draw.accept(new Painter(t.newTextGraphics(), new TerminalSize(cols, rows)));
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                text.append(t.getCharacter(col, row).getCharacterString());
            }
            text.append('\n');
        }
        return text.toString();
    }

    /** Le texte lisible de toutes les pages, sans cadres ni marques de suite. */
    private static String words(String screen) {
        return screen.replaceAll("[│║┌┐└┘╔╗╚╝─═…]", " ").replaceAll("\\s+", " ");
    }

    @Test
    void uneLigneTropLongueDefileParPagesSansPerdreDeMots() {
        String text = "Chaque tirage de chance en ta faveur est fait deux fois : tu gardes le meilleur des deux,"
                + " même pour la rareté des récompenses.";
        StringBuilder all = new StringBuilder();
        for (int tick = 0; tick < 6; tick++) {
            String page = Painter.paged(text, 40, tick);
            assertTrue(page.length() <= 40, page);
            all.append(' ').append(page.replace("…", " "));
        }
        for (String word : text.split("\\s+")) {
            assertTrue(all.toString().contains(word), "mot perdu : " + word);
        }
        assertTrue(Painter.paged(text, 40, 0).endsWith(" …"));
        assertTrue(Painter.paged(text, 40, 1).startsWith("… "));
        assertEquals("court", Painter.paged("court", 40, 5));
    }

    @Test
    void desLignesTropNombreusesDefilentParFenetres() {
        List<String> lines = List.of("a", "b", "c", "d", "e");
        assertEquals(List.of("a", "b", "c …"), Painter.pagedLines(lines, 3, 0));
        assertEquals(List.of("c", "d", "e"), Painter.pagedLines(lines, 3, 1));
        assertEquals(List.of("a", "b", "c …"), Painter.pagedLines(lines, 3, 2));
        assertEquals(lines, Painter.pagedLines(lines, 9, 1));
    }

    @Test
    void titreUneLigneParCycleDElementsEtMenuVisible() {
        String screen = render(80, 24, p -> MenuScreens.title(p, 1, List.of()));
        assertTrue(screen.contains("Feu > Plante > Eau > Feu"), screen);
        assertTrue(screen.contains("Algebrique > Spirituel > Geometrique > Algebrique"), screen);
        assertTrue(screen.contains("Error: null bat tout"), screen);
        assertTrue(screen.contains("▶ Jouer"), screen);
    }

    @Test
    void recompenseLaDescriptionDuSecretSeLitEnEntierEn80x24() {
        GameState state = GameState.newGame();
        Collectible longest = ItemRegistry.all().stream()
                .max(java.util.Comparator.comparingInt(c -> c.description().length())).orElseThrow();
        List<Collectible> options = List.of(longest, ItemRegistry.items().get(0), ItemRegistry.items().get(1));
        StringBuilder seen = new StringBuilder();
        for (int tick = 0; tick < 4; tick++) {
            int t = tick;
            seen.append(words(render(80, 24, p -> ItemScreens.reward(p, state, options, 0, 1, t))));
        }
        for (String word : longest.description().split("\\s+")) {
            assertTrue(seen.toString().contains(word), "mot perdu : " + word + "\n" + longest.description());
        }
    }

    @Test
    void recompenseDeCarteLaDescriptionPasseALaLigne() {
        GameState state = GameState.newGame();
        cardistry.model.card.Card space = new CardPool().cards().stream()
                .filter(c -> c.name().equals("Espace de dimension n")).findFirst().orElseThrow();
        String screen = render(80, 24, p -> ItemScreens.cardReward(p, state, List.of(space), 0, 1, 0));
        assertTrue(words(screen).contains("si tu y es encore"), screen);
    }

    @Test
    void combatLaDescriptionDeLaCarteDefile() {
        GameState state = new GameState(new Player(100), List.of(new Enemy("Mannequin", Element.NATURE, 50, 5)),
                CardPool.dojo(), List.of(CardPool.professeurDOr()), new Stats());
        ViewState view = new ViewState();
        String first = render(80, 24, p -> CombatScreen.draw(p, state, view, 1));
        view.pageTick = 1;
        String second = render(80, 24, p -> CombatScreen.draw(p, state, view, 1));
        String full = CombatScreen.describe(state, CardPool.professeurDOr());
        assertTrue(full.length() > 76, full);
        String tail = full.substring(full.lastIndexOf(' ') + 1);
        assertTrue(!first.contains(tail + " ") && second.contains(tail), second);
    }

    @Test
    void recompenseDansLaFenetreCapture() throws Exception {
        GameState state = GameState.newGame();
        Collectible longest = ItemRegistry.all().stream()
                .max(java.util.Comparator.comparingInt(c -> c.description().length())).orElseThrow();
        List<Collectible> options = List.of(longest, ItemRegistry.trinket("george"), ItemRegistry.items().get(1));
        for (int[] size : new int[][] {{80, 24}, {100, 32}}) {
            DefaultVirtualTerminal t = new DefaultVirtualTerminal(new TerminalSize(size[0], size[1]));
            cardistry.ui.window.ImageLayer images = new cardistry.ui.window.ImageLayer();
            images.begin();
            ItemScreens.reward(new Painter(t.newTextGraphics(), new TerminalSize(size[0], size[1]), false, images),
                    state, options, 0, 1, 0);
            images.commit();
            java.awt.image.BufferedImage png = new cardistry.ui.window.GridRenderer(new cardistry.ui.window.CellFonts(2))
                    .render(t, images.snapshot(), images.marks(), false);
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("build", "screens"));
            javax.imageio.ImageIO.write(png, "png", java.nio.file.Path.of("build", "screens",
                    "69-fenetre-recompense-longue-" + size[0] + "x" + size[1] + ".png").toFile());
        }
    }

    @Test
    void chaqueCarteAfficheSaRareteEnCoin() {
        assertEquals("C", cardistry.ui.CardText.rarityLetter(CardPool.bouleDeFeu()));
        assertEquals("P", cardistry.ui.CardText.rarityLetter(CardPool.moindreCarre()));
        assertEquals("R", cardistry.ui.CardText.rarityLetter(CardPool.pyromane()));
        assertEquals("L", cardistry.ui.CardText.rarityLetter(CardPool.professeurDOr()));
        assertEquals("", cardistry.ui.CardText.rarityLetter(new cardistry.model.card.DeadCard()));
        GameState state = new GameState(new Player(100), List.of(new Enemy("Mannequin", Element.NATURE, 50, 5)),
                CardPool.dojo(), List.of(CardPool.bouleDeFeu(), CardPool.pyromane(), CardPool.professeurDOr(),
                        new cardistry.model.card.SpellCard("Nu", Element.FIRE, 10, cardistry.items.Rarity.UNCOMMON)),
                new Stats());
        String screen = render(80, 24, p -> CombatScreen.draw(p, state, new ViewState(), 1));
        assertTrue(screen.contains("★C╗") || screen.contains("★C┐"), screen);
        assertTrue(screen.contains("★R┐") && screen.contains("★L┐") && screen.contains("─P┐"), screen);
    }
}

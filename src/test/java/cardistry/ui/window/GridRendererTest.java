package cardistry.ui.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Rendu de la grille dans une image : cellules, polices, images, filtre CRT. */
class GridRendererTest {

    private final GridRenderer renderer = new GridRenderer(new CellFonts(1));

    private static DefaultVirtualTerminal terminal(int cols, int rows) {
        return new DefaultVirtualTerminal(new TerminalSize(cols, rows));
    }

    private static void put(DefaultVirtualTerminal t, int col, int row, String text, TextColor fg, TextColor bg) {
        t.setCursorPosition(col, row);
        t.setForegroundColor(fg);
        t.setBackgroundColor(bg);
        t.putString(text);
    }

    /** Pixels de la cellule ({@code col}, {@code row}) de couleur {@code color}. */
    private int count(BufferedImage image, int col, int row, Color color) {
        int n = 0;
        for (int y = row * renderer.cellHeight(); y < (row + 1) * renderer.cellHeight(); y++) {
            for (int x = col * renderer.cellWidth(); x < (col + 1) * renderer.cellWidth(); x++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    void celluleDeLaGrillePolicePixel() {
        assertEquals(7, renderer.cellWidth());
        assertEquals(14, renderer.cellHeight());
        GridRenderer twice = new GridRenderer(new CellFonts(2));
        assertEquals(14, twice.cellWidth());
        assertEquals(28, twice.cellHeight());
    }

    @Test
    void tailleDeLImage() {
        BufferedImage image = renderer.render(terminal(80, 24), List.of(), false);
        assertEquals(80 * 7, image.getWidth());
        assertEquals(24 * 14, image.getHeight());
    }

    @Test
    void texteEtFondEnCouleur() {
        DefaultVirtualTerminal t = terminal(10, 3);
        put(t, 1, 1, "M", TextColor.ANSI.RED_BRIGHT, TextColor.ANSI.BLUE);
        BufferedImage image = renderer.render(t, List.of(), false);
        Color red = CellPalette.foreground(TextColor.ANSI.RED_BRIGHT);
        Color blue = CellPalette.background(TextColor.ANSI.BLUE);
        assertTrue(count(image, 1, 1, red) > 5, "le M est dessiné en rouge");
        assertTrue(count(image, 1, 1, blue) > 20, "fond bleu");
        assertEquals(0, count(image, 3, 1, red));
        assertEquals(7 * 14, count(image, 3, 1, CellPalette.BACKGROUND));
    }

    @Test
    void pixelsNetsSansLissage() {
        DefaultVirtualTerminal t = terminal(3, 1);
        put(t, 0, 0, "#", TextColor.ANSI.WHITE_BRIGHT, TextColor.ANSI.DEFAULT);
        BufferedImage image = renderer.render(t, List.of(), false);
        Color white = CellPalette.foreground(TextColor.ANSI.WHITE_BRIGHT);
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < 7; x++) {
                int rgb = image.getRGB(x, y);
                assertTrue(rgb == white.getRGB() || rgb == CellPalette.BACKGROUND.getRGB(),
                        "pixel intermédiaire (lissage) en " + x + "," + y);
            }
        }
    }

    @Test
    void caractereAbsentDeLaPolicePixel() {
        CellFonts fonts = new CellFonts(1);
        assertEquals(fonts.fallback(), fonts.fontFor('▶'));
        assertEquals(fonts.main(), fonts.fontFor('★'));
        DefaultVirtualTerminal t = terminal(3, 1);
        put(t, 0, 0, "▶", TextColor.ANSI.YELLOW_BRIGHT, TextColor.ANSI.DEFAULT);
        BufferedImage image = renderer.render(t, List.of(), false);
        long lit = 0;
        for (int y = 0; y < 14; y++) {
            for (int x = 0; x < 7; x++) {
                if (image.getRGB(x, y) != CellPalette.BACKGROUND.getRGB()) {
                    lit++;
                }
            }
        }
        assertTrue(lit > 5, "▶ dessiné avec la police de secours");
    }

    /** Tous les caractères non ASCII de l'interface s'affichent avec l'une des deux polices. */
    @Test
    void couvertureDeTousLesCaracteresDuJeu() throws IOException {
        Set<Integer> chars = new TreeSet<>();
        for (Path root : List.of(Path.of("src/main/java"), Path.of("src/main/resources/sprites"),
                Path.of("src/main/resources/banners"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Files.readString(file).codePoints().filter(c -> c > 127).forEach(chars::add);
                }
            }
        }
        CellFonts fonts = new CellFonts(1);
        StringBuilder missing = new StringBuilder();
        for (int c : chars) {
            if (fonts.fontFor(c) == null) {
                missing.appendCodePoint(c);
            }
        }
        assertEquals("", missing.toString(), "caractères sans police");
    }

    @Test
    void imageAlignéeSurLaGrilleSansLissage() {
        DefaultVirtualTerminal t = terminal(20, 10);
        // test-image.png : 10×5, rouge au centre (8×3), bord transparent.
        BufferedImage image = renderer.render(t, List.of(
                new ImageLayer.Placement(2, 2, 10, 5, "enemies/test-image.png", null, 1f)), false);
        // Zone 70×70 px, image 10×5 → ×7 (entier) = 70×35, centrée verticalement.
        int left = 2 * 7;
        int top = 2 * 14 + (70 - 35) / 2;
        assertEquals(Color.RED.getRGB(), image.getRGB(left + 7, top + 7));
        assertEquals(Color.RED.getRGB(), image.getRGB(left + 62, top + 27));
        assertEquals(CellPalette.BACKGROUND.getRGB(), image.getRGB(left + 3, top + 3), "bord transparent");
    }

    @Test
    void imageTeinteeEnBlanc() {
        BufferedImage image = renderer.render(terminal(20, 10), List.of(
                new ImageLayer.Placement(2, 2, 10, 5, "enemies/test-image.png", Color.WHITE, 1f)), false);
        assertEquals(Color.WHITE.getRGB(), image.getRGB(2 * 7 + 7, 2 * 14 + 17 + 7));
    }

    @Test
    void imageManquanteIgnoree() {
        BufferedImage image = renderer.render(terminal(10, 5), List.of(
                new ImageLayer.Placement(0, 0, 5, 5, "enemies/absente.png", null, 1f)), false);
        assertEquals(CellPalette.BACKGROUND.getRGB(), image.getRGB(10, 10));
    }

    @Test
    void filtreCrtAssombritUneLigneSurDeux() {
        DefaultVirtualTerminal t = terminal(10, 4);
        put(t, 0, 0, "██████████", TextColor.ANSI.WHITE_BRIGHT, TextColor.ANSI.DEFAULT);
        BufferedImage plain = renderer.render(t, List.of(), false);
        BufferedImage crt = renderer.render(t, List.of(), true);
        Color even = new Color(crt.getRGB(35, 4));
        Color odd = new Color(crt.getRGB(35, 5));
        assertTrue(odd.getRed() < even.getRed(), "ligne impaire assombrie");
        assertNotEquals(plain.getRGB(35, 5), crt.getRGB(35, 5));
        assertNotNull(crt);
    }

    @Test
    void calqueDImagesValideEntier() {
        ImageLayer layer = new ImageLayer();
        layer.begin();
        layer.add(new ImageLayer.Placement(0, 0, 1, 1, "a", null, 1f));
        assertTrue(layer.snapshot().isEmpty(), "rien avant commit");
        layer.commit();
        assertEquals(1, layer.snapshot().size());
        layer.begin();
        assertEquals(1, layer.snapshot().size(), "l'ancien écran reste affiché pendant le dessin");
    }
}

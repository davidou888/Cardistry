package cardistry.ui.window;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Dessine une grille de cellules (le contenu d'un terminal virtuel Lanterna) dans une image : fond et
 * caractère de chaque cellule, puis les images du calque, puis le filtre CRT éventuel. Aucune
 * dépendance à l'écran : utilisable sans affichage (tests).
 */
public final class GridRenderer {

    /** Opacité des lignes de balayage du filtre CRT. */
    static final float SCANLINE_ALPHA = 0.28f;

    private final CellFonts fonts;
    private final int cellWidth;
    private final int cellHeight;
    private final int ascent;

    public GridRenderer(CellFonts fonts) {
        this.fonts = fonts;
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = probe.createGraphics();
        FontMetrics metrics = g.getFontMetrics(fonts.main());
        g.dispose();
        this.cellWidth = metrics.charWidth('M');
        this.cellHeight = metrics.getHeight();
        this.ascent = metrics.getAscent();
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    /** Image de la grille entière (taille du terminal × taille d'une cellule). */
    public BufferedImage render(VirtualTerminal terminal, List<ImageLayer.Placement> images, boolean crt) {
        TerminalSize size = terminal.getTerminalSize();
        BufferedImage image = new BufferedImage(size.getColumns() * cellWidth, size.getRows() * cellHeight,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(CellPalette.BACKGROUND);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int row = 0; row < size.getRows(); row++) {
            for (int col = 0; col < size.getColumns(); col++) {
                drawCell(g, col, row, terminal.getCharacter(col, row));
            }
        }
        for (ImageLayer.Placement placement : images) {
            drawImage(g, placement);
        }
        if (crt) {
            crt(g, image.getWidth(), image.getHeight());
        }
        g.dispose();
        return image;
    }

    private void drawCell(Graphics2D g, int col, int row, TextCharacter cell) {
        if (cell == null) {
            return;
        }
        Color fg = CellPalette.foreground(cell.getForegroundColor());
        Color bg = CellPalette.background(cell.getBackgroundColor());
        if (cell.isReversed()) {
            Color swap = fg;
            fg = bg;
            bg = swap;
        }
        int x = col * cellWidth;
        int y = row * cellHeight;
        if (!bg.equals(CellPalette.BACKGROUND)) {
            g.setColor(bg);
            g.fillRect(x, y, cellWidth, cellHeight);
        }
        String text = cell.getCharacterString();
        if (text.isBlank()) {
            return;
        }
        int codePoint = text.codePointAt(0);
        Font font = fonts.fontFor(codePoint);
        if (font == null) {
            font = fonts.main();
            text = "?";
        }
        // Police pixel : pas de lissage (sinon elle bave) ; la police de secours, lissée.
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, font == fonts.main()
                ? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(fg);
        g.drawString(text, x, y + ascent);
    }

    private void drawImage(Graphics2D g, ImageLayer.Placement placement) {
        Images.get(placement.path()).ifPresent(source -> {
            BufferedImage img = placement.tint() != null ? tinted(source, placement.tint()) : source;
            int areaW = placement.cols() * cellWidth;
            int areaH = placement.rows() * cellHeight;
            double fit = Math.min((double) areaW / img.getWidth(), (double) areaH / img.getHeight());
            double scale = fit >= 1 ? Math.floor(fit) : fit; // pixel art : agrandissement entier
            int w = (int) Math.round(img.getWidth() * scale);
            int h = (int) Math.round(img.getHeight() * scale);
            int x = placement.col() * cellWidth + (areaW - w) / 2;
            int y = placement.row() * cellHeight + (areaH - h) / 2;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            java.awt.Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, placement.alpha()))));
            g.drawImage(img, x, y, w, h, null);
            g.setComposite(old);
        });
    }

    /** Silhouette de l'image dans la couleur {@code tint} (transparence gardée). */
    static BufferedImage tinted(BufferedImage source, Color tint) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(tint);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.dispose();
        return out;
    }

    /** Filtre CRT léger : une ligne de pixels sur deux assombrie, et un vignettage doux. */
    static void crt(Graphics2D g, int width, int height) {
        g.setColor(new Color(0, 0, 0, Math.round(SCANLINE_ALPHA * 255)));
        for (int y = 1; y < height; y += 2) {
            g.fillRect(0, y, width, 1);
        }
        float radius = Math.max(width, height) * 0.75f;
        g.setPaint(new RadialGradientPaint(width / 2f, height / 2f, radius, new float[] {0.6f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 90)}));
        g.fillRect(0, 0, width, height);
    }
}

package cardistry.ui.lanterna;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

/** Primitives de dessin au-dessus de {@link TextGraphics}. Tout ce qui déborde est coupé. */
final class Painter {

    private static final String SINGLE = "┌┐└┘─│";
    private static final String DOUBLE = "╔╗╚╝═║";

    private final TextGraphics g;
    private final TerminalSize size;

    Painter(TextGraphics g, TerminalSize size) {
        this.g = g;
        this.size = size;
    }

    int cols() {
        return size.getColumns();
    }

    int rows() {
        return size.getRows();
    }

    void text(int col, int row, String text, TextColor color) {
        text(col, row, text, color, false);
    }

    void text(int col, int row, String text, TextColor color, boolean bold) {
        if (row < 0 || row >= rows() || col >= cols() || text.isEmpty()) {
            return;
        }
        if (col < 0) {
            if (-col >= text.length()) {
                return;
            }
            text = text.substring(-col);
            col = 0;
        }
        if (col + text.length() > cols()) {
            text = text.substring(0, cols() - col);
        }
        g.setForegroundColor(color);
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
        if (bold) {
            g.putString(col, row, text, SGR.BOLD);
        } else {
            g.putString(col, row, text);
        }
    }

    /** Texte centré horizontalement sur la ligne {@code row}. */
    void centered(int row, String text, TextColor color, boolean bold) {
        text(Math.max(0, (cols() - text.length()) / 2), row, text, color, bold);
    }

    void box(int col, int row, int width, int height, TextColor color, boolean doubleLine) {
        String c = doubleLine ? DOUBLE : SINGLE;
        String horizontal = String.valueOf(c.charAt(4)).repeat(Math.max(0, width - 2));
        text(col, row, c.charAt(0) + horizontal + c.charAt(1), color, doubleLine);
        for (int r = row + 1; r < row + height - 1; r++) {
            text(col, r, String.valueOf(c.charAt(5)), color, doubleLine);
            text(col + width - 1, r, String.valueOf(c.charAt(5)), color, doubleLine);
        }
        text(col, row + height - 1, c.charAt(2) + horizontal + c.charAt(3), color, doubleLine);
    }

    /** Cadre de tout l'écran, avec un titre à gauche et une info à droite en haut. */
    void frame(String title, String rightInfo, String help) {
        box(0, 0, cols(), rows(), Palette.FRAME, false);
        text(2, 0, " " + title + " ", Palette.ACCENT, true);
        if (!rightInfo.isEmpty()) {
            text(cols() - rightInfo.length() - 4, 0, " " + rightInfo + " ", Palette.DIM);
        }
        if (!help.isEmpty()) {
            text(2, rows() - 1, " " + help + " ", Palette.DIM);
        }
    }

    /** Séparateur horizontal sur toute la largeur, raccordé au cadre, avec un titre. */
    void separator(int row, String title) {
        text(0, row, "├" + "─".repeat(cols() - 2) + "┤", Palette.FRAME);
        if (!title.isEmpty()) {
            text(2, row, " " + title + " ", Palette.TEXT, true);
        }
    }

    /** Barre de remplissage : ████░░░░. */
    static String bar(double value, double max, int width) {
        double ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, value / max));
        int filled = (int) Math.round(ratio * width);
        if (value > 0 && filled == 0) {
            filled = 1;
        }
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    /** Coupe {@code text} à {@code width} caractères avec « … ». */
    static String fit(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return width <= 1 ? text.substring(0, width) : text.substring(0, width - 1) + "…";
    }
}

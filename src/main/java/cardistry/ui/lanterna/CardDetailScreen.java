package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.model.Ids;
import cardistry.model.card.Card;
import cardistry.ui.CardDetail;
import cardistry.ui.Format;
import com.googlecode.lanterna.TextColor;
import java.util.List;

/** Fiche d'une carte (touche V, en combat ou au shop) : grande illustration dans la fenêtre. */
final class CardDetailScreen {

    static final String HELP = "V, Échap ou Entrée revenir";

    private CardDetailScreen() {
    }

    static void draw(Painter p, GameState state, Card card, boolean inCombat, long seed) {
        p.frame("Carte", "Seed " + seed, HELP);
        int textCol = 3;
        int artRows = Math.min(8, p.rows() - 6);
        if (p.hasImages() && p.image(3, 2, artRows * 2, artRows, "cards/" + Ids.slug(card.name()) + ".png", null, 1f)) {
            textCol = 3 + artRows * 2 + 3;
        }
        int width = p.cols() - textCol - 3;
        int row = 2;
        for (CardDetail.Line line : CardDetail.lines(state, card, inCombat)) {
            if (row >= p.rows() - 1) {
                break;
            }
            List<String> wrapped = line.text().isEmpty() ? List.of("") : Format.wrap(line.text(), width);
            for (String text : wrapped) {
                if (row >= p.rows() - 1) {
                    break;
                }
                // Sous l'illustration, le texte reprend toute la largeur.
                int col = row >= 2 + artRows ? 3 : textCol;
                p.text(col, row++, Painter.fit(text, p.cols() - col - 3), color(line.kind(), card), bold(line.kind()));
            }
        }
    }

    private static TextColor color(CardDetail.Kind kind, Card card) {
        return switch (kind) {
            case TITLE -> Palette.element(card.element());
            case SECTION -> Palette.ACCENT;
            case TOTAL -> TextColor.ANSI.WHITE_BRIGHT;
            case POWER -> Palette.SECRET;
            case DIM -> Palette.DIM;
            case TEXT -> Palette.TEXT;
        };
    }

    private static boolean bold(CardDetail.Kind kind) {
        return kind == CardDetail.Kind.TITLE || kind == CardDetail.Kind.SECTION || kind == CardDetail.Kind.TOTAL;
    }
}

package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.model.Ids;
import cardistry.ui.CardText;
import java.util.ArrayList;
import java.util.List;

/**
 * Le deck (touche D, n'importe où pendant une partie) : cartes regroupées par type, ×N pour les
 * doublons, valeurs de base, rareté, pouvoir ou effet de terrain. ↑/↓ font défiler.
 */
final class DeckScreen {

    static final String HELP = "↑/↓ défiler · D, Échap ou Entrée fermer";

    private DeckScreen() {
    }

    /** Une ligne de l'écran : titre de groupe ou carte. */
    private record Line(String title, CardText.DeckEntry entry) {
    }

    private static List<Line> lines(GameState state) {
        List<Line> lines = new ArrayList<>();
        for (CardText.DeckGroup group : CardText.deckGroups(state.deck())) {
            if (!lines.isEmpty()) {
                lines.add(new Line("", null));
            }
            lines.add(new Line(group.title(), null));
            for (CardText.DeckEntry entry : group.entries()) {
                lines.add(new Line(null, entry));
            }
        }
        return lines;
    }

    /** Défilement maximal pour la taille d'écran de {@code p}. */
    static int maxScroll(Painter p, GameState state) {
        return Math.max(0, lines(state).size() - visibleRows(p));
    }

    private static int visibleRows(Painter p) {
        return p.rows() - 4;
    }

    /** {@code piles} : en combat, affiche aussi la pioche et la défausse. */
    static void draw(Painter p, GameState state, int scroll, boolean piles, long seed) {
        String subtitle = state.deck().size() + " cartes" + (piles ? " · Pioche " + state.drawPile().size()
                + " · Défausse " + state.discardPile().size() : "") + " · Seed " + seed;
        p.frame("Ton deck", subtitle, HELP);
        List<Line> lines = lines(state);
        int rows = visibleRows(p);
        int first = Math.max(0, Math.min(scroll, lines.size() - rows));
        int width = p.cols() - 7; // le texte s'arrête juste avant la colonne des flèches ▲/▼
        for (int i = 0; i < rows && first + i < lines.size(); i++) {
            Line line = lines.get(first + i);
            int row = 2 + i;
            if (line.entry() == null) {
                p.text(3, row, line.title(), Palette.ACCENT, true);
                continue;
            }
            cardistry.model.card.Card card = line.entry().card();
            int col = 4;
            if (p.hasImages() && p.image(col, row, 2, 1, "cards/" + Ids.slug(card.name()) + ".png", null, 1f)) {
                col += 3;
            }
            String count = line.entry().count() > 1 ? "×" + line.entry().count() + " " : "";
            p.text(col, row, count, Palette.TEXT, true);
            col += count.length();
            String name = Painter.fit(card.name(), Math.max(1, width - col + 4));
            p.text(col, row, name, Palette.element(card.element()), true);
            col += name.length();
            String rest = CardText.summary(card).substring(card.name().length());
            p.text(col, row, Painter.fit(rest, Math.max(0, width + 4 - col)), Palette.TEXT);
        }
        if (first > 0) {
            p.text(p.cols() - 3, 2, "▲", Palette.ACCENT);
        }
        if (first + rows < lines.size()) {
            p.text(p.cols() - 3, p.rows() - 3, "▼", Palette.ACCENT);
        }
    }
}

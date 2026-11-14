package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.story.Story;
import cardistry.story.StoryChoice;
import cardistry.ui.Format;
import java.util.List;

/** Événement : le texte et les choix, puis ce qui s'est passé. */
final class StoryScreen {

    static final String HELP = "↑/↓ choisir · Entrée valider · Q quitter";
    private static final int MARGIN = 6;

    private StoryScreen() {
    }

    static void choices(Painter p, GameState state, Story story, int selected, long seed) {
        p.frame("Événement", state.choomies() + " Choomies · Seed " + seed, HELP);
        int row = header(p, state, story.title(), story.text());
        for (int i = 0; i < story.choices().size(); i++) {
            StoryChoice choice = story.choices().get(i);
            boolean available = choice.isAvailable(state);
            boolean isSelected = i == selected;
            p.text(MARGIN - 2, row, isSelected ? "▶" : " ", Palette.ACCENT, true);
            String label = "[" + (i + 1) + "] " + choice.label();
            p.text(MARGIN, row, label, available ? (isSelected ? Palette.ACCENT : Palette.TEXT) : Palette.DIM,
                    isSelected && available);
            String detail = available ? choice.hint() : "(" + choice.requirement() + ")";
            if (!detail.isEmpty()) {
                p.text(MARGIN + label.length() + 2, row, Painter.fit(detail, p.cols() - MARGIN - label.length() - 4),
                        Palette.DIM);
            }
            row += 2;
        }
    }

    static void result(Painter p, GameState state, GameEvent.StoryResolved result, long seed) {
        p.frame("Événement", state.choomies() + " Choomies · Seed " + seed, "Entrée continuer");
        int row = header(p, state, result.title(), "Tu choisis : " + result.choice());
        for (String line : result.lines()) {
            for (String part : Format.wrap(line, p.cols() - 2 * MARGIN)) {
                p.text(MARGIN, row++, part, Palette.ACCENT, true);
            }
        }
        row++;
        p.text(MARGIN, row, "PV " + Format.num(state.player().hp()) + "/" + Format.num(state.maxHp()) + " · Clopes "
                + state.cigarettes() + "/" + state.cigaretteMax() + " · Deck " + state.deck().size() + " cartes",
                Palette.DIM);
    }

    /** Titre et texte ; renvoie la première ligne libre. */
    private static int header(Painter p, GameState state, String title, String text) {
        p.centered(3, "?  " + title + "  ?", Palette.INFO, true);
        int row = 6;
        List<String> lines = Format.wrap(text, p.cols() - 2 * MARGIN);
        for (String line : lines) {
            p.text(MARGIN, row++, line, Palette.TEXT);
        }
        return row + 2;
    }
}

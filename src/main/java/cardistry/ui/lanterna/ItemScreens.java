package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.items.Collectible;
import cardistry.items.Item;
import cardistry.items.Tag;
import cardistry.items.Trinket;
import cardistry.ui.Format;
import cardistry.ui.ItemKeys;
import java.util.List;
import java.util.stream.Collectors;

/** Écrans de récompense et d'inventaire. */
final class ItemScreens {

    private static final int CARD_HEIGHT = 17;
    private static final int MAX_CARD_WIDTH = 26;

    private ItemScreens() {
    }

    /** Choix d'une récompense : les propositions côte à côte, la sélectionnée en double trait. */
    static void reward(Painter p, GameState state, List<Collectible> options, int selected, long seed) {
        p.frame("Récompense", inventorySummary(state), "←/→ choisir · Entrée prendre · S refuser · Q quitter");
        p.centered(2, "Adversaire vaincu ! Choisis une récompense.", Palette.ACCENT, true);
        drawCards(p, options, selected, 4);
        Collectible chosen = options.get(selected);
        if (isFull(state, chosen)) {
            p.centered(5 + CARD_HEIGHT, Painter.fit("Tes " + (chosen instanceof Trinket ? "trinkets" : "objets")
                    + " sont au complet : la prendre remplacera un de tes items.", p.cols() - 4), Palette.WARNING, false);
        }
    }

    /** Choix de l'item à remplacer quand l'inventaire est plein. */
    static void replace(Painter p, GameState state, Collectible incoming, int selected, long seed) {
        p.frame("Remplacer", "Seed " + seed, "←/→ choisir · Entrée remplacer · Échap revenir");
        p.centered(2, "Que remplace " + incoming.name() + " ?", Palette.ACCENT, true);
        List<? extends Collectible> current = incoming instanceof Trinket ? state.trinkets() : state.items();
        drawCards(p, List.copyOf(current), selected, 4);
    }

    /** Inventaire détaillé. */
    static void inventory(Painter p, GameState state) {
        p.frame("Inventaire", "", "Appuie sur une touche pour revenir");
        int row = 2;
        p.text(3, row++, "Objets " + state.items().size() + "/" + state.itemSlots()
                + "  (action gratuite : " + ItemKeys.label(state.itemSlots()) + " en combat)", Palette.TEXT, true);
        for (int i = 0; i < state.items().size(); i++) {
            Item item = state.items().get(i);
            row = line(p, row, "[" + Character.toUpperCase(ItemKeys.key(i)) + "] ", item);
        }
        if (state.items().isEmpty()) {
            p.text(5, row++, "aucun", Palette.DIM);
        }
        row++;
        p.text(3, row++, "Trinkets " + state.trinkets().size() + "/" + state.trinketSlots(), Palette.TEXT, true);
        for (Trinket trinket : state.trinkets()) {
            row = line(p, row, "• ", trinket);
        }
        if (state.trinkets().isEmpty()) {
            p.text(5, row, "aucun", Palette.DIM);
        }
    }

    private static int line(Painter p, int row, String prefix, Collectible c) {
        if (row >= p.rows() - 1) {
            return row;
        }
        p.text(5, row, prefix, Palette.DIM);
        int col = 5 + prefix.length();
        p.text(col, row, c.name(), Palette.rarity(c.rarity()), true);
        col += c.name().length();
        String rest = "  " + c.rarity().displayName() + " · " + tags(c) + " — " + c.description();
        List<String> lines = Format.wrap(rest.strip(), Math.max(10, p.cols() - col - 3));
        p.text(col + 2, row, lines.get(0), Palette.TEXT);
        for (int i = 1; i < lines.size() && row + i < p.rows() - 1; i++) {
            p.text(col + 2, row + i, lines.get(i), Palette.TEXT);
        }
        return row + lines.size();
    }

    private static void drawCards(Painter p, List<Collectible> cards, int selected, int top) {
        int count = cards.size();
        int width = Math.min(MAX_CARD_WIDTH, (p.cols() - 4 - 2 * (count - 1)) / Math.max(1, count));
        int total = count * width + 2 * (count - 1);
        int left = Math.max(2, (p.cols() - total) / 2);
        for (int i = 0; i < count; i++) {
            boolean isSelected = i == selected;
            drawCard(p, left + i * (width + 2), isSelected ? top - 1 : top, width, cards.get(i), isSelected);
        }
    }

    private static void drawCard(Painter p, int col, int row, int width, Collectible c, boolean selected) {
        int inner = width - 2;
        p.box(col, row, width, CARD_HEIGHT, Palette.rarity(c.rarity()), selected);
        int last = row + CARD_HEIGHT - 3; // dernière ligne de texte libre (la suivante = tags)
        int r = row + 1;
        p.text(col + 1, r++, Painter.fit(c.kindName() + " · " + c.rarity().displayName(), inner),
                Palette.rarity(c.rarity()));
        for (String line : Format.wrap(c.name(), inner)) {
            if (r < row + 4) {
                p.text(col + 1, r++, line, Palette.TEXT, true);
            }
        }
        r = Math.max(r, row + 4);
        for (String line : Format.wrap(c.description(), inner)) {
            if (r <= last) {
                p.text(col + 1, r++, line, Palette.TEXT);
            }
        }
        if (!c.flavor().isEmpty()) {
            r++;
            for (String line : Format.wrap("« " + c.flavor() + " »", inner)) {
                if (r <= last) {
                    p.text(col + 1, r++, line, Palette.DIM);
                }
            }
        }
        p.text(col + 1, row + CARD_HEIGHT - 2, Painter.fit(tags(c), inner), Palette.DIM);
    }

    static String tags(Collectible c) {
        return c.tags().stream().sorted().map(Tag::displayName).collect(Collectors.joining(", "));
    }

    static boolean isFull(GameState state, Collectible c) {
        return c instanceof Trinket
                ? state.trinkets().size() >= state.trinketSlots()
                : state.items().size() >= state.itemSlots();
    }

    private static String inventorySummary(GameState state) {
        return "Objets " + state.items().size() + "/" + state.itemSlots()
                + " · Trinkets " + state.trinkets().size() + "/" + state.trinketSlots();
    }
}

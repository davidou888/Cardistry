package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.core.Shop;
import cardistry.core.shop.ShopOffer;
import cardistry.items.Collectible;
import cardistry.ui.Format;
import com.googlecode.lanterna.TextColor;

/**
 * Shop : 5 cartes en haut, puis 2 objets et 2 trinkets, puis les services (reroll, retrait). Les
 * prix trop chers sont grisés.
 */
final class ShopScreen {

    /** Réplique d'accueil provisoire (le vendeur et sa réplique viendront plus tard). */
    static final String GREETING = "« Entre, entre ! Tout est à vendre... enfin, presque. »";
    static final String HELP = "←/→ ↑/↓ · Entrée acheter · R reroll · T retirer · Échap partir";
    private static final int CARD_TOP = 4;

    private ShopScreen() {
    }

    /** Index des articles par rangée : cartes, objets, trinkets. */
    static java.util.List<java.util.List<Integer>> rows(Shop shop) {
        java.util.List<java.util.List<Integer>> rows = java.util.List.of(new java.util.ArrayList<>(),
                new java.util.ArrayList<>(), new java.util.ArrayList<>());
        for (int i = 0; i < shop.offers().size(); i++) {
            ShopOffer offer = shop.offers().get(i);
            int row = offer instanceof ShopOffer.CardOffer ? 0
                    : ((ShopOffer.CollectibleOffer) offer).collectible() instanceof cardistry.items.Item ? 1 : 2;
            rows.get(row).add(i);
        }
        return rows;
    }

    static void draw(Painter p, GameState state, Shop shop, int selected, int removalPrice, long seed, String toast) {
        p.frame("Shop", state.choomies() + " Choomies · Seed " + seed, HELP);
        p.centered(2, GREETING, Palette.DIM, false);
        java.util.List<java.util.List<Integer>> rows = rows(shop);
        int width = Math.min(14, (p.cols() - 4 - 2 * (Shop.CARDS - 1)) / Shop.CARDS);
        int total = Shop.CARDS * width + 2 * (Shop.CARDS - 1);
        int left = Math.max(2, (p.cols() - total) / 2);
        for (int k = 0; k < rows.get(0).size(); k++) {
            int i = rows.get(0).get(k);
            ShopOffer.CardOffer offer = (ShopOffer.CardOffer) shop.offers().get(i);
            int col = left + k * (width + 2);
            boolean isSelected = i == selected;
            if (shop.isSold(i)) {
                p.box(col, CARD_TOP + 1, width, 6, Palette.DIM, false);
                p.text(col + (width - 5) / 2, CARD_TOP + 3, "vendu", Palette.DIM);
            } else {
                int top = isSelected ? CARD_TOP : CARD_TOP + 1;
                if (p.hasImages()) {
                    CombatWindowScreen.card(p, col, top, width, 6, i + 1, offer.card(), isSelected, false);
                } else {
                    CombatScreen.drawCard(p, col, top, width, i + 1, offer.card(), isSelected, false);
                }
            }
            String price = shop.isSold(i) ? "" : offer.price() + " Ch.";
            p.text(col + (width - price.length()) / 2, CARD_TOP + 7, price, priceColor(state, offer), isSelected);
        }
        int row = CARD_TOP + 9;
        row = collectibles(p, state, shop, selected, "Objets", rows.get(1), row);
        row = collectibles(p, state, shop, selected, "Trinkets", rows.get(2), row);
        if (selected >= 0 && selected < shop.offers().size()) {
            p.text(2, row, Painter.fit(describe(shop.offers().get(selected)), p.cols() - 4), Palette.TEXT);
        }
        if (toast != null) {
            p.text(2, p.rows() - 5, Painter.fit("★ " + toast, p.cols() - 4), Palette.ACCENT, true);
        }
        String reroll = "[R] Reroll : " + shop.rerollPrice();
        String removal = "[T] Retirer une carte : " + removalPrice;
        int services = p.rows() - 3;
        p.text(2, services, reroll, state.choomies() >= shop.rerollPrice() ? Palette.INFO : Palette.DIM, true);
        p.text(4 + reroll.length(), services, removal, state.choomies() >= removalPrice
                && state.deck().size() > cardistry.core.CombatEngine.MIN_DECK_SIZE ? Palette.INFO : Palette.DIM, true);
        p.text(6 + reroll.length() + removal.length(), services, "Deck " + state.deck().size() + " cartes",
                Palette.DIM);
    }

    private static int collectibles(Painter p, GameState state, Shop shop, int selected, String title,
            java.util.List<Integer> indices, int row) {
        p.text(3, row, title, Palette.TEXT, true);
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            ShopOffer.CollectibleOffer offer = (ShopOffer.CollectibleOffer) shop.offers().get(index);
            Collectible c = offer.collectible();
            boolean isSelected = index == selected;
            int col = 13 + i * 33;
            p.text(col - 2, row, isSelected ? "▶" : " ", Palette.ACCENT, true);
            if (shop.isSold(index)) {
                p.text(col, row, Painter.fit(c.name() + " — vendu", 30), Palette.DIM);
                continue;
            }
            int icon = 0;
            if (p.hasImages()) { // fenêtre : icône encadrée à la couleur de rareté, sur 2 lignes
                CombatWindowScreen.slot(p, col, row, c);
                icon = CombatWindowScreen.SLOT_COLS + 1;
                p.text(col + icon, row + 1, Painter.fit(c.rarity().displayName(), 22 - icon), Palette.DIM);
            }
            String name = Painter.fit(c.name(), 22 - icon);
            p.text(col + icon, row, name, Palette.rarity(c.rarity()), isSelected);
            p.text(col + 24, row, offer.price() + " Ch.", priceColor(state, offer), isSelected);
        }
        return row + 2;
    }

    static TextColor priceColor(GameState state, ShopOffer offer) {
        return state.choomies() >= offer.price() ? Palette.ACCENT : Palette.DIM;
    }

    /** Ligne de détail de l'article sélectionné. */
    static String describe(ShopOffer offer) {
        return switch (offer) {
            case ShopOffer.CardOffer card -> "Carte " + card.card().name() + " (" + Format.cardKind(card.card())
                    + ") : ajoutée à ton deck · " + card.price() + " Choomies";
            case ShopOffer.CollectibleOffer c -> c.collectible().kindName() + " " + c.collectible().rarity().displayName()
                    + " : " + c.collectible().description() + " · " + c.price() + " Choomies";
        };
    }
}

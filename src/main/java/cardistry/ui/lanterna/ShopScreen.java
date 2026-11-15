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
 *
 * <p>Le marchand fume. Dans une fenêtre d'au moins {@link #MERCHANT_MIN_COLS}×{@link #MERCHANT_MIN_ROWS},
 * son sprite est à gauche des cartes et la fumée monte de sa clope (dessin au pixel près, voir
 * {@link #smoke(int)}) ; ailleurs (terminal, petite fenêtre), un petit fumeur en texte précède sa
 * réplique.
 */
final class ShopScreen {

    /** Réplique d'accueil provisoire (le vendeur et sa réplique viendront plus tard). */
    static final String GREETING = "« Entre, entre ! Tout est à vendre... enfin, presque. »";
    static final String HELP = "←/→ ↑/↓ · Entrée · V détail · R reroll · T retirer · P partir · Échap";
    static final String LEAVE = "[P] Partir";
    private static final int CARD_TOP = 4;

    // --- Marchand ---
    static final String MERCHANT = "marchand";
    static final int MERCHANT_MIN_COLS = 100;
    static final int MERCHANT_MIN_ROWS = 30;
    /** Rectangle du sprite : 64 px agrandis ×2 (133×140 unités pour 128). */
    static final int MERCHANT_COLS = 19;
    static final int MERCHANT_ROWS = 10;
    private static final int MERCHANT_COL = 2;
    private static final int MERCHANT_TOP = 2;
    private static final int MERCHANT_SCALE = 2;
    /** Point de départ de la fumée dans le sprite : juste au-dessus du bout de la clope (art/tools/npcs.lua). */
    static final int CIGARETTE_X = 51;
    static final int CIGARETTE_Y = 26;
    /** Le marchand tire sur sa clope (frame 2 : œil fermé, braise vive) pendant 3 pas sur 18. */
    private static final int DRAG_PERIOD = 18;

    // --- Fumée ---
    static final int SMOKE_PUFFS = 14;
    /** Durée de vie d'une volute, en pas d'animation. */
    static final int SMOKE_LIFE = 27;
    private static final String[] WISPS = {"~  ", "~~ ", " ~~", "  ~", "   "};

    /** Une volute : décalage par rapport au bout de la clope (unités du calque), côté et opacité. */
    record Puff(int dx, int dy, int size, float alpha) {
    }

    /**
     * Volutes au pas {@code tick} : chacune monte, ondule, grossit et s'efface en {@link #SMOKE_LIFE}
     * pas, puis repart de la clope avec une autre trajectoire. Pure (aucun hasard) : testable.
     */
    static java.util.List<Puff> smoke(int tick) {
        java.util.List<Puff> puffs = new java.util.ArrayList<>();
        for (int i = 0; i < SMOKE_PUFFS; i++) {
            int time = tick + i * SMOKE_LIFE / SMOKE_PUFFS;
            int age = Math.floorMod(time, SMOKE_LIFE);
            int cycle = Math.floorDiv(time, SMOKE_LIFE);
            int seed = Math.floorMod((i * 31 + cycle * 17) * 0x9E3779B1, 1000);
            double phase = seed / 1000.0 * Math.PI * 2;
            double drift = 0.15 + (seed % 7) / 12.0; // vers la droite, plus ou moins
            double dx = Math.sin(age * 0.32 + phase) * (1 + age / 7.0) + age * drift;
            double dy = -age * 2.5;
            float alpha = 0.75f * (1 - age / (float) SMOKE_LIFE);
            puffs.add(new Puff((int) Math.round(dx), (int) Math.round(dy), 2 + age / 5, alpha));
        }
        return puffs;
    }

    /** {@code true} si le sprite du marchand est affiché (fenêtre assez grande, image présente). */
    static boolean showsMerchant(Painter p) {
        return p.hasImages() && p.cols() >= MERCHANT_MIN_COLS && p.rows() >= MERCHANT_MIN_ROWS
                && cardistry.ui.window.Images.npc(MERCHANT, 0).isPresent();
    }

    private static void drawMerchant(Painter p, int tick) {
        int frame = Math.floorMod(tick, DRAG_PERIOD) >= DRAG_PERIOD - 3 ? 1 : 0;
        p.image(MERCHANT_COL, MERCHANT_TOP, MERCHANT_COLS, MERCHANT_ROWS,
                cardistry.ui.window.Images.npc(MERCHANT, frame).orElseThrow(), null, 1f);
        // Le sprite (64 px × 2) est centré dans son rectangle de cellules.
        int unitsPerCol = cardistry.ui.window.ImageLayer.UNITS_PER_COL;
        int unitsPerRow = cardistry.ui.window.ImageLayer.UNITS_PER_ROW;
        int originX = MERCHANT_COL * unitsPerCol + (MERCHANT_COLS * unitsPerCol - 64 * MERCHANT_SCALE) / 2;
        int originY = MERCHANT_TOP * unitsPerRow + (MERCHANT_ROWS * unitsPerRow - 64 * MERCHANT_SCALE) / 2;
        int x = originX + CIGARETTE_X * MERCHANT_SCALE;
        int y = originY + CIGARETTE_Y * MERCHANT_SCALE;
        for (Puff puff : smoke(tick)) {
            int top = y + puff.dy() - puff.size();
            if (top > unitsPerRow) { // jamais sur le cadre
                p.fill(x + puff.dx(), top, puff.size(), puff.size(), Palette.TEXT, puff.alpha());
            }
        }
    }

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
        draw(p, state, shop, selected, removalPrice, seed, toast, 0);
    }

    /** @param tick pas d'animation de la fumée (0 = image fixe) */
    static void draw(Painter p, GameState state, Shop shop, int selected, int removalPrice, long seed, String toast,
            int tick) {
        draw(p, state, shop, selected, removalPrice, seed, toast, tick, 0);
    }

    /** {@code pageTick} : page de la description de l'article si elle ne tient pas sur une ligne. */
    static void draw(Painter p, GameState state, Shop shop, int selected, int removalPrice, long seed, String toast,
            int tick, int pageTick) {
        p.frame("Shop", state.choomies() + " Choomies · Seed " + seed, HELP);
        java.util.List<java.util.List<Integer>> rows = rows(shop);
        boolean merchant = showsMerchant(p);
        // Avec le marchand, les cartes se serrent à sa droite.
        int start = merchant ? MERCHANT_COL + MERCHANT_COLS + 2 : 2;
        int room = p.cols() - 2 - start;
        int width = Math.min(14, (room - 2 * (Shop.CARDS - 1)) / Shop.CARDS);
        int total = Shop.CARDS * width + 2 * (Shop.CARDS - 1);
        int left = start + Math.max(0, (room - total) / 2);
        if (merchant) {
            drawMerchant(p, tick);
            p.text(left, 2, Painter.fit(GREETING, p.cols() - 2 - left), Palette.DIM);
        } else {
            // Pas de sprite : un petit fumeur en texte, dont la fumée s'éloigne.
            String smoker = "(-_-)y-" + WISPS[Math.floorMod(tick / 3, WISPS.length)] + " ";
            int col = Math.max(1, (p.cols() - smoker.length() - GREETING.length()) / 2);
            p.text(col, 2, smoker, Palette.TEXT);
            p.text(col + smoker.length(), 2, GREETING, Palette.DIM);
        }
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
        // L'espace en plus (grand écran) est réparti entre les rangées au lieu de rester en bas.
        int gap = Math.max(0, (p.rows() - 24) / 4);
        int step = (p.hasImages() ? 2 : 1) + 1 + gap;
        int row = CARD_TOP + 9 + gap;
        collectibles(p, state, shop, selected, "Objets", rows.get(1), row);
        collectibles(p, state, shop, selected, "Trinkets", rows.get(2), row + step);
        int detail = Math.min(p.rows() - 6, row + 2 * step + gap);
        if (selected >= 0 && selected < shop.offers().size()) {
            p.text(2, detail, Painter.paged(describe(shop.offers().get(selected)), p.cols() - 4, pageTick),
                    Palette.TEXT);
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
        p.text(p.cols() - 2 - LEAVE.length(), services, LEAVE, Palette.ACCENT, true);
    }

    private static void collectibles(Painter p, GameState state, Shop shop, int selected, String title,
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
    }

    static TextColor priceColor(GameState state, ShopOffer offer) {
        return state.choomies() >= offer.price() ? Palette.ACCENT : Palette.DIM;
    }

    /** Ligne de détail de l'article sélectionné. */
    static String describe(ShopOffer offer) {
        return switch (offer) {
            case ShopOffer.CardOffer card -> cardistry.ui.CardText.summary(card.card()) + " · " + card.price()
                    + " Choomies";
            case ShopOffer.CollectibleOffer c -> c.collectible().kindName() + " " + c.collectible().rarity().displayName()
                    + " : " + c.collectible().description() + " · " + c.price() + " Choomies";
        };
    }
}

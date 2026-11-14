package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.core.Curse;
import cardistry.core.GameState;
import cardistry.core.TemporaryEffect;
import cardistry.items.Collectible;
import cardistry.items.Gauge;
import cardistry.items.Item;
import cardistry.items.Trinket;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.MoveEffect;
import cardistry.ui.Format;
import cardistry.ui.ItemKeys;
import cardistry.ui.window.ImageLayer;
import cardistry.ui.window.Images;
import com.googlecode.lanterna.TextColor;
import java.util.ArrayList;
import java.util.List;

/**
 * Écran de combat de la fenêtre (calque d'images) : joueur à gauche, adversaire à droite, la main en
 * petites cartes illustrées, une barre d'icônes, et le journal fermé par défaut (touche J).
 *
 * <pre>
 *  ligne 0          cadre : titre, tour, Choomies, seed
 *  ligne 1          lieu à gauche, terrain à droite
 *  lignes 2..s-1    arène : sprite + état du joueur | intent, état + sprite de l'adversaire
 *  ligne s          séparateur « Ta main »
 *  s+1..s+h+1       cartes (h = 6, ou 8 à partir de 32 lignes ; la sélection est surélevée)
 *  s+h+2            détail de la carte sélectionnée
 *  s+h+3, s+h+4     barre : objets, trinkets, clopes
 *  s+h+5            dernière action (ou notification)
 *  dernière         aide clavier
 * </pre>
 * Le terminal ({@code --terminal}, {@code --no-images}) garde {@link CombatScreen}.
 */
final class CombatWindowScreen {

    static final String HELP = "←/→ Entrée · 1-7 · P pioche · W/E/R objets · F/G clopes · J journal · I · Q";
    static final String HELP_UNDO = "←/→ 1-7 · P pioche · W/E/R objets · F/G clopes · J journal · Ctrl+Z · Q";
    static final String PLAYER_IMAGE = "player/player.png";
    /** Largeur d'un emplacement d'icône (objet, trinket, paquet), sur 2 lignes. */
    static final int SLOT_COLS = 4;
    private static final int SLOT_ROWS = 2;
    private static final int MAX_INFO_WIDTH = 24;
    private static final int MAX_CARD_WIDTH = 16;
    /** En dessous, la carte n'a pas la place pour une illustration : dessin en texte. */
    private static final int MIN_ART_CARD_WIDTH = 9;

    private CombatWindowScreen() {
    }

    /** Géométrie de l'écran pour une taille de grille. */
    record Layout(int cols, int rows, int cardHeight, int separator, int top, int half, int spriteRows,
            int spriteCols, int spriteTop, int infoWidth) {

        static Layout of(int cols, int rows) {
            int cardHeight = rows >= 32 ? 8 : 6;
            int separator = rows - (cardHeight + 7);
            int top = 2;
            int arenaRows = separator - top;
            int half = (cols - 4) / 2;
            int spriteRows = Math.max(3, Math.min(arenaRows - 2, (half - 24) / 2));
            int infoWidth = Math.min(MAX_INFO_WIDTH, half - spriteRows * 2 - 4);
            int spriteTop = top + 1 + (arenaRows - 1 - spriteRows) / 2;
            return new Layout(cols, rows, cardHeight, separator, top, half, spriteRows, spriteRows * 2, spriteTop,
                    infoWidth);
        }

        int playerCol() {
            return 3;
        }

        int enemyCol() {
            return cols - 3 - spriteCols;
        }

        int detailRow() {
            return separator + cardHeight + 2;
        }

        int barRow() {
            return separator + cardHeight + 3;
        }

        int lastActionRow() {
            return rows - 2;
        }
    }

    static void draw(Painter p, GameState state, ViewState view, long seed) {
        Layout l = Layout.of(p.cols(), p.rows());
        p.frame("Cardistry", "Tour " + state.turn() + " · " + state.choomies() + " Choomies · Seed " + seed,
                Painter.fit(state.hasTrinket(cardistry.items.ItemRegistry.CTRL_Z) ? HELP_UNDO : HELP, p.cols() - 6));
        drawHeader(p, state, view);
        p.separator(l.separator(), "Ta main " + state.hand().size() + "/" + state.handSize() + (state.usesDeck()
                ? " · Pioche " + state.drawPile().size() + " · Défausse " + state.discardPile().size() : ""));
        String undo = CombatScreen.undoStatus(state);
        if (!undo.isEmpty()) {
            p.text(p.cols() - undo.length() - 4, l.separator(), " " + undo + " ",
                    state.undoUsedThisCombat() ? Palette.DIM : Palette.SECRET, !state.undoUsedThisCombat());
        }
        drawBar(p, state, l);
        drawLastAction(p, view, l);
        if (view.journalOpen) {
            drawJournal(p, view, l);
            return; // le panneau couvre l'arène et la main : aucune image dessous
        }
        drawPlayer(p, state, view, l);
        drawEnemy(p, state, view, l);
        drawHand(p, state, view, l);
        drawDetail(p, state, view, l);
        drawFloating(p, view, l);
    }

    /** Ligne 1 : lieu à gauche, terrain à droite. */
    private static void drawHeader(Painter p, GameState state, ViewState view) {
        p.text(2, 1, Format.location(state), Palette.DIM);
        TerrainCard terrain = state.terrain();
        String name = terrain.name() + " (" + terrain.element().displayName() + ")";
        int col = p.cols() - 2 - name.length();
        p.text(col - 10, 1, "Terrain : ", Palette.DIM);
        p.text(col, 1, name, view.terrainFlash != null ? view.terrainFlash : Palette.element(terrain.element()), true);
    }

    // --- Joueur ---

    private static void drawPlayer(Painter p, GameState state, ViewState view, Layout l) {
        if (!p.image(l.playerCol(), l.spriteTop(), l.spriteCols(), l.spriteRows(), PLAYER_IMAGE, view.playerFlash,
                1f)) {
            p.text(l.playerCol() + Math.max(0, (l.spriteCols() - 5) / 2), l.spriteTop() + l.spriteRows() / 2, "(toi)",
                    Palette.DIM);
        }
        int col = l.playerCol() + l.spriteCols() + 2;
        int row = l.spriteTop();
        p.text(col, row, "TOI", Palette.TEXT, true);
        double hp = view.playerHpShown != null ? view.playerHpShown : state.player().hp();
        boolean overMax = hp > state.maxHp();
        TextColor barColor = view.playerFlash != null ? view.playerFlash
                : overMax ? Palette.ACCENT : Palette.health(hp / state.maxHp());
        String value = num(Math.max(0, hp)) + "/" + num(state.maxHp());
        int barWidth = Math.max(4, l.infoWidth() - value.length() - 1);
        p.text(col, row + 1, Painter.bar(hp, state.maxHp(), barWidth), barColor, view.playerFlash != null || overMax);
        p.text(col + barWidth + 1, row + 1, value, overMax ? Palette.ACCENT : Palette.TEXT, overMax);

        List<StatusLine> lines = statusLines(state, l.infoWidth());
        int first = row + 2;
        int capacity = l.separator() - first;
        for (int i = 0; i < lines.size() && i < capacity; i++) {
            boolean overflow = i == capacity - 1 && lines.size() > capacity;
            if (overflow) {
                p.text(col, first + i, "+" + (lines.size() - i) + " autres", Palette.DIM);
            } else {
                p.text(col, first + i, lines.get(i).text(), lines.get(i).color(), lines.get(i).bold());
            }
        }
    }

    record StatusLine(String text, TextColor color, boolean bold) {
    }

    /** État du joueur, une ligne par élément : Ivresse, malédictions (Gueule de bois), effets en cours. */
    static List<StatusLine> statusLines(GameState state, int width) {
        List<StatusLine> lines = new ArrayList<>();
        if (state.drunkenness() > 0) {
            boolean over = state.drunkenness() >= state.hangoverThreshold();
            lines.add(new StatusLine("Ivresse " + state.drunkenness() + "/" + state.hangoverThreshold(),
                    over ? Palette.DAMAGE : Palette.WARNING, true));
        }
        for (Curse curse : state.activeCurses()) {
            lines.add(new StatusLine(Painter.fit(curse.displayName() + " ("
                    + curse.description(state.activeIntensity(curse)) + ")", width), Palette.DAMAGE, false));
        }
        for (Curse curse : state.pendingCurses()) {
            lines.add(new StatusLine(Painter.fit(curse.displayName() + " à venir", width), Palette.DIM, false));
        }
        for (TemporaryEffect effect : state.effects()) {
            String text = effect.describe();
            if (text.length() > width) {
                text = CombatScreen.compact(effect);
            }
            lines.add(new StatusLine(Painter.fit(text, width), effect.isActive() ? Palette.INFO : Palette.DIM, false));
        }
        return lines;
    }

    // --- Adversaire ---

    /** Chemin de l'image de l'adversaire pour l'animation en cours, s'il en a une. */
    private static java.util.Optional<String> enemyImage(Enemy enemy, ViewState view) {
        boolean idle = view.spriteAnimation.equals(cardistry.ui.sprite.Sprite.IDLE);
        int idleFrames = Math.max(1, Images.idleFrames(enemy.spriteId()));
        int frame = idle ? Math.floorMod(view.idleFrame, idleFrames) : view.spriteFrame;
        return Images.enemy(enemy.spriteId(), view.spriteAnimation, frame);
    }

    private static void drawEnemy(Painter p, GameState state, ViewState view, Layout l) {
        Enemy enemy = state.currentEnemy();
        drawIntent(p, state, enemy, l);
        TextColor color = Palette.element(enemy.element());
        double hp = view.enemyHpShown != null ? view.enemyHpShown : enemy.hp();
        String value = num(Math.max(0, hp)) + "/" + num(enemy.maxHp());
        java.util.Optional<String> image = enemyImage(enemy, view);
        if (image.isPresent()) {
            if (!view.enemyHidden) {
                p.image(l.enemyCol() + view.enemyOffsetX, l.spriteTop(), l.spriteCols(), l.spriteRows(), image.get(),
                        view.enemyFlash, view.enemyDim ? 0.5f : 1f);
            }
            int width = l.infoWidth();
            int col = l.enemyCol() - 2 - width;
            int row = l.spriteTop();
            String name = Painter.fit(enemy.name().toUpperCase(), width);
            p.text(col + width - name.length(), row, name, color, true);
            int barWidth = Math.max(4, width - value.length() - 1);
            p.text(col + width - barWidth - 1 - value.length(), row + 1, value, Palette.TEXT);
            p.text(col + width - barWidth, row + 1, Painter.bar(hp, enemy.maxHp(), barWidth), Palette.DAMAGE);
            String element = "■ " + enemy.element().displayName();
            p.text(col + width - element.length(), row + 2, element, color);
            return;
        }
        // Pas d'image : nom et PV sur deux lignes à droite, sprite ASCII dessous.
        int right = p.cols() - 3;
        String name = Painter.fit(enemy.name().toUpperCase() + " · " + enemy.element().displayName(), l.half());
        p.text(right - name.length(), l.top() + 1, name, color, true);
        int barWidth = Math.min(20, l.half() - value.length() - 1);
        p.text(right - barWidth - 1 - value.length(), l.top() + 2, value, Palette.TEXT);
        p.text(right - barWidth, l.top() + 2, Painter.bar(hp, enemy.maxHp(), barWidth), Palette.DAMAGE);
        if (view.enemyHidden) {
            return;
        }
        cardistry.ui.sprite.Sprite sprite = cardistry.ui.sprite.Sprites.of(enemy);
        int col = Math.max(2 + l.half(), right - sprite.width()) + view.enemyOffsetX;
        SpriteArt.draw(p, CombatScreen.frameFor(sprite, view), col, l.top() + 3, l.separator() - 1,
                view.enemyDim ? Palette.DIM : color, view.enemyFlash, p.cols() - 1);
    }

    /** Icône de l'attaque annoncée : épée si elle frappe, « ! » sinon. */
    static String intentIcon(Enemy enemy) {
        boolean hits = enemy.peekIntent().effects().stream().anyMatch(e -> e instanceof MoveEffect.Hit);
        return hits ? "ui/intent-attack.png" : "ui/intent-special.png";
    }

    /** Ligne au-dessus de l'adversaire : icône + attaque annoncée + multiplicateur du terrain. */
    private static void drawIntent(Painter p, GameState state, Enemy enemy, Layout l) {
        double mult = state.enemyAttackMultiplier();
        String note = mult != 1.0 ? " (×" + num(mult) + ")" : "";
        String text = Painter.fit(cardistry.ui.IntentText.intent(state, enemy), Math.max(1, l.half() - 3 - note.length()));
        int col = p.cols() - 3 - text.length() - note.length();
        p.image(col - 3, l.top(), 2, 1, intentIcon(enemy), null, 1f);
        p.text(col, l.top(), text, Palette.TEXT);
        if (!note.isEmpty()) {
            p.text(col + text.length(), l.top(), note, mult > 1.0 ? Palette.DAMAGE : Palette.HEAL);
        }
    }

    // --- Main ---

    private static void drawHand(Painter p, GameState state, ViewState view, Layout l) {
        int slots = state.handSize();
        int width = Math.min(MAX_CARD_WIDTH, (p.cols() - 4 - (slots - 1)) / slots);
        int total = slots * width + (slots - 1);
        int left = 2 + Math.max(0, (p.cols() - 4 - total) / 2);
        List<Card> hand = state.hand();
        for (int i = 0; i < slots; i++) {
            int col = left + i * (width + 1);
            if (i < hand.size()) {
                boolean selected = i == view.selected && !view.confirmingQuit;
                card(p, col, l.separator() + (selected ? 1 : 2), width, l.cardHeight(), i + 1, hand.get(i), selected,
                        i == view.highlightSlot);
            } else {
                p.box(col, l.separator() + 2, width, l.cardHeight(), Palette.DIM, false);
                p.text(col + 1, l.separator() + 2, String.valueOf(i + 1), Palette.DIM);
            }
        }
    }

    /** Icône de type d'une carte (sort, soin, terrain, carte morte). */
    static String typeIcon(Card card) {
        return switch (card) {
            case SpellCard s -> "ui/type-spell.png";
            case HealCard h -> "ui/type-heal.png";
            case TerrainCard t -> "ui/type-terrain.png";
            case DeadCard d -> "ui/type-dead.png";
        };
    }

    static String typeName(Card card) {
        return switch (card) {
            case SpellCard s -> "sort";
            case HealCard h -> "soin";
            case TerrainCard t -> "terrain";
            case DeadCard d -> "bloquée";
        };
    }

    /** Valeur affichée sur la carte : dégâts, « +PV », rien pour un terrain ou une carte morte. */
    static String value(Card card) {
        return switch (card) {
            case SpellCard s -> num(s.damage());
            case HealCard h -> "+" + num(h.amount());
            case TerrainCard t -> "";
            case DeadCard d -> "";
        };
    }

    /**
     * Une carte : cadre à la couleur de l'élément, illustration ({@code images/cards/<id>.png}, ou
     * l'icône de type en grand à la couleur de l'élément), icône de type, valeur, nom.
     */
    static void card(Painter p, int col, int row, int width, int height, int number, Card card, boolean selected,
            boolean highlight) {
        if (width < MIN_ART_CARD_WIDTH) {
            CombatScreen.drawCard(p, col, row, width, number, card, selected, highlight);
            return;
        }
        TextColor element = Palette.element(card.element());
        TextColor frame = highlight ? Palette.ACCENT : element;
        TextColor back = TextColor.ANSI.BLACK;
        p.box(col, row, width, height, frame, selected);
        for (int r = row + 1; r < row + height - 1; r++) {
            p.text(col + 1, r, " ".repeat(width - 2), Palette.TEXT, false, back);
        }
        p.text(col + 1, row, String.valueOf(number), selected ? Palette.ACCENT : Palette.TEXT, true);

        int artRows = height >= 8 ? 3 : 2;
        int artCols = artRows * 2;
        String art = Images.card(cardistry.model.Ids.slug(card.name())).orElse(null);
        if (art != null) {
            p.image(col + 1, row + 1, artCols, artRows, art, null, 1f);
        } else {
            p.image(col + 1, row + 1, artCols, artRows, typeIcon(card), element, 1f);
        }
        int side = col + 1 + artCols + 1;
        p.image(side, row + 1, 2, 1, typeIcon(card), null, 1f);
        p.text(side, row + 2, Painter.fit(value(card), Math.max(1, col + width - 1 - side)),
                TextColor.ANSI.WHITE_BRIGHT, true, back);

        int inner = width - 2;
        List<String> name = Format.wrap(card.name(), inner);
        if (name.size() > 2) {
            name = List.of(name.get(0), Painter.fit(name.get(1) + "…", inner));
        }
        for (int i = 0; i < name.size(); i++) {
            p.text(col + 1, row + 1 + artRows + i, Painter.fit(name.get(i), inner), Palette.TEXT, selected, back);
        }
        if (height >= 8) {
            String elementName = card.element() == null ? "null" : card.element().displayName();
            String label = typeName(card) + " · " + elementName;
            p.text(col + 1, row + height - 2, label.length() <= inner ? label : Painter.fit(elementName, inner),
                    element, false, back);
        }
    }

    private static void drawDetail(Painter p, GameState state, ViewState view, Layout l) {
        List<Card> hand = state.hand();
        if (hand.isEmpty()) {
            p.text(2, l.detailRow(), "Ta main est vide : appuie sur P pour piocher.", Palette.DIM);
            return;
        }
        Card card = hand.get(Math.min(view.selected, hand.size() - 1));
        p.text(2, l.detailRow(), Painter.fit(CombatScreen.describe(state, card), p.cols() - 4), Palette.TEXT);
    }

    // --- Barre : objets, trinkets, clopes ---

    /**
     * Emplacement d'icône de {@link #SLOT_COLS} × 2 cellules : fond sombre, cadre d'un pixel à la couleur
     * de rareté, icône au centre ({@code images/items/<id>.png}, sinon {@code fallback}). {@code null} :
     * emplacement vide, cadre en tirets.
     */
    static void slot(Painter p, int col, int row, Collectible content, String fallback) {
        int x = col * ImageLayer.UNITS_PER_COL;
        int y = row * ImageLayer.UNITS_PER_ROW;
        int w = SLOT_COLS * ImageLayer.UNITS_PER_COL;
        int h = SLOT_ROWS * ImageLayer.UNITS_PER_ROW;
        p.fill(x, y, w, h, TextColor.ANSI.BLACK);
        if (content == null) {
            for (int i = 0; i < w; i += 4) {
                p.fill(x + i, y, 2, 1, Palette.DIM);
                p.fill(x + i, y + h - 1, 2, 1, Palette.DIM);
            }
            for (int i = 0; i < h; i += 4) {
                p.fill(x, y + i, 1, 2, Palette.DIM);
                p.fill(x + w - 1, y + i, 1, 2, Palette.DIM);
            }
            return;
        }
        TextColor rarity = Palette.rarity(content.rarity());
        p.fill(x, y, w, 1, rarity);
        p.fill(x, y + h - 1, w, 1, rarity);
        p.fill(x, y, 1, h, rarity);
        p.fill(x + w - 1, y, 1, h, rarity);
        p.stamp(x + w / 2, y + h / 2, w - 4, Images.item(content.id()).orElse(fallback), 1f);
    }

    /** Texte à droite du paquet : « 5/20 clopes », la jauge d'un trinket si elle tient, et les touches. */
    private static void drawBar(Painter p, GameState state, Layout l) {
        int row = l.barRow();
        boolean wide = p.cols() >= 100;
        String count = state.cigarettes() + "/" + Math.max(1, state.cigaretteMax()) + (wide ? " clopes" : "");
        String keys = wide ? "F fumer · G rouler" : "F · G";
        String gauge = state.trinkets().stream().flatMap(t -> t.gauge().stream()).findFirst()
                .map(CombatWindowScreen::gaugeText).orElse("");
        int itemSlots = state.itemSlots();
        int trinketSlots = state.trinketSlots();
        int fixed = 4 + 2 + trinketSlots * SLOT_COLS + 2 + SLOT_COLS + 1; // marges, séparateurs, trinkets, paquet
        int packText = Math.max(count.length(), keys.length());
        // La jauge se met à côté des clopes si les noms d'objets gardent au moins 6 caractères.
        boolean showGauge = !gauge.isEmpty()
                && (p.cols() - fixed - Math.max(packText, count.length() + 2 + gauge.length())) / itemSlots - 6 >= 6;
        if (showGauge) {
            packText = Math.max(packText, count.length() + 2 + gauge.length());
        }
        int nameWidth = Math.max(3, (p.cols() - fixed - packText) / Math.max(1, itemSlots) - 6);
        int packCol = p.cols() - 2 - packText - 1 - SLOT_COLS;

        int col = 2;
        for (int i = 0; i < itemSlots && col + SLOT_COLS + 4 + nameWidth <= packCol - trinketSlots * SLOT_COLS; i++) {
            Item item = i < state.items().size() ? state.items().get(i) : null;
            slot(p, col, row, item, "ui/item.png");
            if (item != null) {
                p.text(col + SLOT_COLS + 1, row, Painter.fit(item.name(), nameWidth), Palette.rarity(item.rarity()), true);
            } else {
                p.text(col + SLOT_COLS + 1, row, "·", Palette.DIM);
            }
            p.text(col + SLOT_COLS + 1, row + 1, "[" + Character.toUpperCase(ItemKeys.key(i)) + "]", Palette.DIM);
            col += SLOT_COLS + 1 + nameWidth + 1;
        }
        p.text(col, row, "│", Palette.DIM);
        p.text(col, row + 1, "│", Palette.DIM);
        col += 2;
        for (int i = 0; i < trinketSlots && col + SLOT_COLS <= packCol - 1; i++) {
            Trinket trinket = i < state.trinkets().size() ? state.trinkets().get(i) : null;
            slot(p, col, row, trinket, "ui/trinket.png");
            col += SLOT_COLS;
        }
        p.text(col, row, "│", Palette.DIM);
        p.text(col, row + 1, "│", Palette.DIM);

        boolean empty = state.cigarettes() == 0;
        p.image(packCol, row, SLOT_COLS, SLOT_ROWS, "ui/pack.png", null, empty ? 0.4f : 1f);
        int textCol = packCol + SLOT_COLS + 1;
        p.text(textCol, row, count, empty ? Palette.WARNING : Palette.TEXT, true);
        if (showGauge) {
            p.text(textCol + count.length() + 2, row, gauge, Palette.ACCENT);
        }
        p.text(textCol, row + 1, keys, Palette.DIM);
    }

    /** « Honoré [###..] ». */
    static String gaugeText(Gauge gauge) {
        return gauge.label() + " [" + "#".repeat(gauge.value()) + ".".repeat(gauge.max() - gauge.value()) + "]";
    }

    // --- Dernière action et journal ---

    /** Une seule ligne discrète : la notification en cours, sinon la dernière ligne du journal. */
    private static void drawLastAction(Painter p, ViewState view, Layout l) {
        String hint = view.journalOpen ? "J fermer" : "J journal";
        p.text(p.cols() - 2 - hint.length(), l.lastActionRow(), hint, Palette.DIM);
        int width = p.cols() - 4 - hint.length() - 2;
        if (view.toast != null) {
            p.text(2, l.lastActionRow(), Painter.fit("★ " + view.toast, width), Palette.ACCENT, true);
        } else if (!view.log.isEmpty()) {
            p.text(2, l.lastActionRow(), Painter.fit("› " + view.log.get(view.log.size() - 1).text(), width),
                    Palette.DIM);
        }
    }

    /** Lignes de journal visibles dans le panneau. */
    static int journalCapacity(int rows) {
        Layout l = Layout.of(80, rows);
        return l.barRow() - 1 - l.top() - 1;
    }

    /** Panneau du journal par-dessus l'arène et la main ; {@code view.journalScroll} lignes en arrière. */
    private static void drawJournal(Painter p, ViewState view, Layout l) {
        int top = l.top();
        int bottom = l.barRow() - 1;
        int width = p.cols() - 2;
        p.box(1, top, width, bottom - top + 1, Palette.ACCENT, true);
        for (int r = top + 1; r < bottom; r++) {
            p.text(2, r, " ".repeat(width - 2), Palette.TEXT, false, TextColor.ANSI.BLACK);
        }
        p.text(4, top, " Journal ", Palette.ACCENT, true);
        String help = " ↑/↓ défiler · J fermer ";
        p.text(1 + width - help.length() - 2, bottom, help, Palette.DIM);
        int capacity = bottom - top - 1;
        List<ViewState.LogLine> log = view.log;
        int maxScroll = Math.max(0, log.size() - capacity);
        int scroll = Math.max(0, Math.min(view.journalScroll, maxScroll));
        int end = log.size() - scroll;
        int start = Math.max(0, end - capacity);
        for (int i = start; i < end; i++) {
            ViewState.LogLine line = log.get(i);
            boolean latest = i == log.size() - 1;
            p.text(4, top + 1 + i - start, Painter.fit(line.text(), width - 6),
                    line.color(), latest, TextColor.ANSI.BLACK);
        }
        if (scroll > 0) {
            p.text(4, bottom, " ↓ " + scroll + " plus récentes ", Palette.DIM);
        }
    }

    // --- Textes flottants ---

    private static void drawFloating(Painter p, ViewState view, Layout l) {
        for (ViewState.Floating f : view.floating) {
            int center = f.onEnemy() ? l.enemyCol() + l.spriteCols() / 2 : l.playerCol() + l.spriteCols() / 2;
            p.text(center - f.text().length() / 2, Math.max(l.top(), l.spriteTop() + 1 - f.rise()), f.text(), f.color(),
                    true);
        }
    }
}

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
 *  lignes 2..s-1    arène : intent de l'adversaire, les deux sprites face à face (états du joueur
 *                   à côté du sien), puis une barre de PV sous chaque sprite
 *  ligne s          séparateur « Ta main »
 *  s+1..s+h+1       cartes (h = 6, ou 8 à partir de 36 lignes ; la sélection est surélevée)
 *  s+h+2            détail de la carte sélectionnée
 *  s+h+3, s+h+4     barre : objets, trinkets, clopes
 *  s+h+5            dernière action (ou notification)
 *  dernière         aide clavier
 * </pre>
 * Le terminal ({@code --terminal}, {@code --no-images}) garde {@link CombatScreen}.
 */
final class CombatWindowScreen {

    static final String HELP = "←/→ · 1-7 jouer · P pioche · W/E/R objets · F/G clopes · J journal · I · Q";
    static final String HELP_UNDO = "←/→ 1-7 · P pioche · W/E/R objets · F/G clopes · J journal · Ctrl+Z · Q";
    static final String PLAYER_IMAGE = "player/player.png";
    /** Largeur d'un emplacement d'icône (objet, trinket, paquet), sur 2 lignes. */
    static final int SLOT_COLS = 4;
    private static final int SLOT_ROWS = 2;
    static final String PLAYER_NAME = "TOI";
    private static final int MAX_HP_BAR = 20;
    /** Largeur minimale gardée à côté du sprite du joueur pour ses états (Ivresse, effets). */
    private static final int MIN_STATUS_WIDTH = 14;
    /** À partir de cette hauteur d'écran, les cartes gagnent 2 lignes ; en dessous, la place va aux sprites. */
    static final int TALL_CARD_ROWS = 36;
    private static final int MAX_CARD_WIDTH = 16;
    /** En dessous, la carte n'a pas la place pour une illustration : dessin en texte. */
    private static final int MIN_ART_CARD_WIDTH = 9;

    private CombatWindowScreen() {
    }

    /** Géométrie de l'écran pour une taille de grille. */
    record Layout(int cols, int rows, int cardHeight, int separator, int top, int half, int spriteRows,
            int spriteCols, int spriteTop, int statusWidth) {

        static Layout of(int cols, int rows) {
            int cardHeight = rows >= TALL_CARD_ROWS ? 8 : 6;
            int separator = rows - (cardHeight + 7);
            int top = 2;
            int arenaRows = separator - top;
            int half = (cols - 4) / 2;
            // L'arène : une ligne d'intent, le sprite (carré : 2 colonnes par ligne), une ligne de PV.
            int spriteRows = Math.max(3, Math.min(arenaRows - 2, (half - MIN_STATUS_WIDTH - 3) / 2));
            int spriteTop = top + 1 + (arenaRows - 2 - spriteRows) / 2;
            return new Layout(cols, rows, cardHeight, separator, top, half, spriteRows, spriteRows * 2, spriteTop,
                    half - spriteRows * 2 - 3);
        }

        int playerCol() {
            return 3;
        }

        int enemyCol() {
            return cols - 3 - spriteCols;
        }

        /** Ligne des PV, juste sous les deux sprites. */
        int hpRow() {
            return spriteTop + spriteRows;
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

    /** Largeur d'une barre de PV : ce qui reste de la moitié d'écran après le nom et la valeur. */
    static int hpBarWidth(Layout l, int nameLength, int valueLength) {
        return Math.max(6, Math.min(MAX_HP_BAR, l.half() - 2 - nameLength - valueLength - 2));
    }

    private static void drawPlayer(Painter p, GameState state, ViewState view, Layout l) {
        String image = state.hero().map(MenuScreens::heroImage).orElse(PLAYER_IMAGE);
        String name = Format.playerLabel(state);
        if (!p.image(l.playerCol(), l.spriteTop(), l.spriteCols(), l.spriteRows(), image, view.playerFlash, 1f)) {
            p.text(l.playerCol() + Math.max(0, (l.spriteCols() - 5) / 2), l.spriteTop() + l.spriteRows() / 2, "(toi)",
                    Palette.DIM);
        }
        // Sous le sprite : « TOI ████████ 82/100 ».
        double hp = view.playerHpShown != null ? view.playerHpShown : state.player().hp();
        boolean overMax = hp > state.maxHp();
        TextColor barColor = view.playerFlash != null ? view.playerFlash
                : overMax ? Palette.ACCENT : Palette.health(hp / state.maxHp());
        String value = num(Math.max(0, hp)) + "/" + num(state.maxHp());
        List<cardistry.core.status.Status> statuses = state.statuses(cardistry.core.status.StatusTarget.PLAYER).all();
        int badges = StatusBadges.width(p, statuses);
        int barWidth = hpBarWidth(l, name.length(), value.length() + (badges > 0 ? badges + 1 : 0));
        int col = l.playerCol();
        p.text(col, l.hpRow(), name, Palette.TEXT, true);
        col += name.length() + 1;
        p.text(col, l.hpRow(), Painter.bar(hp, state.maxHp(), barWidth), barColor, view.playerFlash != null || overMax);
        p.text(col + barWidth + 1, l.hpRow(), value, overMax ? Palette.ACCENT : Palette.TEXT, overMax);
        int badgeCol = col + barWidth + 2 + value.length();
        StatusBadges.draw(p, badgeCol, l.hpRow(), statuses, Math.max(0, 2 + l.half() - badgeCol));

        // À côté du sprite : Ivresse, Gueule de bois, effets en cours.
        int statusCol = l.playerCol() + l.spriteCols() + 2;
        List<StatusLine> lines = statusLines(state, l.statusWidth());
        int capacity = l.spriteRows();
        for (int i = 0; i < lines.size() && i < capacity; i++) {
            boolean overflow = i == capacity - 1 && lines.size() > capacity;
            if (overflow) {
                p.text(statusCol, l.spriteTop() + i, "+" + (lines.size() - i) + " autres", Palette.DIM);
            } else {
                p.text(statusCol, l.spriteTop() + i, lines.get(i).text(), lines.get(i).color(), lines.get(i).bold());
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
            lines.add(new StatusLine(Painter.fit(Format.drunkenness(state), width),
                    over ? Palette.DAMAGE : Palette.WARNING, true));
        }
        for (Curse curse : state.activeCurses()) {
            lines.add(new StatusLine(Painter.fit(curse.displayName() + " ("
                    + state.describeCurse(curse, state.activeIntensity(curse)) + ")", width), Palette.DAMAGE, false));
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
            // Sous le sprite, en miroir du joueur : « 21/28 ████████ CADDIE ÉNERVÉ ».
            enemyHp(p, state, l, l.hpRow(), enemyLabel(enemy, l, value), value, hp, enemy.maxHp(), color);
            return;
        }
        // Pas d'image : PV sous l'intent, puis le sprite ASCII (trop large pour la zone carrée).
        enemyHp(p, state, l, l.top() + 1, enemyLabel(enemy, l, value), value, hp, enemy.maxHp(), color);
        if (view.enemyHidden) {
            return;
        }
        cardistry.ui.sprite.Sprite sprite = cardistry.ui.sprite.Sprites.of(enemy);
        int right = p.cols() - 3;
        int col = Math.max(2 + l.half(), right - sprite.width()) + view.enemyOffsetX;
        SpriteArt.draw(p, CombatScreen.frameFor(sprite, view), col, l.top() + 2, l.separator() - 1,
                view.enemyDim ? Palette.DIM : color, view.enemyFlash, p.cols() - 1);
    }

    /** « CADDIE ÉNERVÉ · neutre » ; l'élément est omis s'il ne laisse pas la place à la barre de PV. */
    static String enemyLabel(Enemy enemy, Layout l, String value) {
        String name = enemy.name().toUpperCase();
        String full = name + " · " + enemy.element().displayName();
        return full.length() <= l.half() - 2 - value.length() - 6 - 2 ? full : name;
    }

    /** « [statuts] 21/28 ████████ NOM », aligné à droite sur la ligne {@code row}. */
    private static void enemyHp(Painter p, GameState state, Layout l, int row, String name, String value, double hp,
            double max, TextColor color) {
        int right = p.cols() - 3;
        List<cardistry.core.status.Status> statuses = state.statuses(cardistry.core.status.StatusTarget.ENEMY).all();
        int badges = StatusBadges.width(p, statuses);
        int extra = badges > 0 ? badges + 1 : 0;
        String shown = Painter.fit(name, Math.max(4, l.half() - 2 - value.length() - extra - 6 - 2));
        int barWidth = hpBarWidth(l, shown.length(), value.length() + extra);
        p.text(right - shown.length(), row, shown, color, true);
        int barCol = right - shown.length() - 1 - barWidth;
        p.text(barCol, row, Painter.bar(hp, max, barWidth), Palette.DAMAGE);
        int valueCol = barCol - 1 - value.length();
        p.text(valueCol, row, value, Palette.TEXT);
        if (badges > 0) {
            int room = Math.max(0, valueCol - 1 - (p.cols() - l.half() - 2));
            int width = Math.min(badges, room);
            StatusBadges.draw(p, valueCol - 1 - width, row, statuses, width);
        }
    }

    /** Icône de l'attaque annoncée : épée si elle frappe, « ! » sinon ; « Zz » s'il dort (Fons). */
    static String intentIcon(GameState state, Enemy enemy) {
        if (state.currentEnemy() == enemy
                && state.statuses(cardistry.core.status.StatusTarget.ENEMY).has(cardistry.core.status.StatusType.FONS)) {
            return "status/" + cardistry.core.status.StatusType.FONS.id() + ".png";
        }
        boolean hits = enemy.peekIntent().effects().stream().anyMatch(e -> e instanceof MoveEffect.Hit);
        return hits ? "ui/intent-attack.png" : "ui/intent-special.png";
    }

    /** Ligne au-dessus de l'adversaire : icône + attaque annoncée + multiplicateur du terrain. */
    private static void drawIntent(Painter p, GameState state, Enemy enemy, Layout l) {
        double mult = state.enemyAttackMultiplier();
        String note = mult != 1.0 ? " (×" + num(mult) + ")" : "";
        String text = Painter.fit(cardistry.ui.IntentText.intent(state, enemy), Math.max(1, l.half() - 3 - note.length()));
        int col = p.cols() - 3 - text.length() - note.length();
        p.image(col - 3, l.top(), 2, 1, intentIcon(state, enemy), null, 1f);
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
        List<String> name = nameLines(card.name(), inner);
        for (int i = 0; i < name.size(); i++) {
            p.text(col + 1, row + 1 + artRows + i, name.get(i), Palette.TEXT, selected, back);
        }
        if (height >= 8) {
            String elementName = card.element() == null ? "null" : card.element().displayName();
            String label = typeName(card) + " · " + elementName;
            p.text(col + 1, row + height - 2, label.length() <= inner ? label : Painter.fit(elementName, inner),
                    element, false, back);
        }
    }

    /**
     * Nom d'une carte sur 2 lignes au plus, sans jamais couper un mot par un tiret : un mot trop long
     * ou un nom trop long est abrégé par « … » (« Caïpiri… »).
     */
    static List<String> nameLines(String name, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        if (lines.size() > 2) {
            lines = new ArrayList<>(List.of(lines.get(0), String.join(" ", lines.subList(1, lines.size()))));
        }
        lines.replaceAll(text -> Painter.fit(text, width));
        return lines;
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
     * de rareté, icône au centre ({@link #icon}). {@code null} : emplacement vide, cadre en tirets. Le
     * même dessin sert en combat, dans les récompenses et au shop.
     */
    static void slot(Painter p, int col, int row, Collectible content) {
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
        p.stamp(x + w / 2, y + h / 2, w - 4, icon(content), 1f);
    }

    /** Icône d'un objet ou trinket : {@code images/items/<id>.png}, sinon le placeholder de sa catégorie. */
    static String icon(Collectible content) {
        return Images.item(content.id()).orElse(content instanceof Item ? "ui/item.png" : "ui/trinket.png");
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
            slot(p, col, row, item);
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
            slot(p, col, row, trinket);
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

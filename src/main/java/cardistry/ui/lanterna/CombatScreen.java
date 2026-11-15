package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.core.Curse;
import cardistry.core.GameState;
import cardistry.core.TemporaryEffect;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.items.Item;
import cardistry.items.Trinket;
import cardistry.ui.Format;
import cardistry.ui.ItemKeys;
import com.googlecode.lanterna.TextColor;
import java.util.List;

/**
 * Écran de combat (80×24 minimum).
 *
 * <pre>
 *  ligne 0        cadre : titre, tour, seed
 *  lignes 1-10    adversaire (infos à gauche, sprite à droite), terrain, joueur (PV, objets, trinkets)
 *  ligne 11       séparateur « Ta main »
 *  lignes 12-18   cartes (la carte sélectionnée est surélevée d'une ligne)
 *  ligne 19       détail de la carte sélectionnée
 *  ligne 20       séparateur « Journal »
 *  lignes 21..    journal (s'agrandit avec le terminal)
 *  dernière       aide clavier
 * </pre>
 */
final class CombatScreen {

    static final String HELP = "←/→ 1-7 · P piocher · W/E/R objets · I · D deck · V détail · Q quitter";
    /** Aide avec le trinket Ctrl+Z équipé. */
    static final String HELP_UNDO = "←/→ 1-7 · P pioche · W/E/R objets · I inv. · Ctrl+Z annuler · Q quitter";
    private static final int SPRITE_WIDTH = 30;
    private static final int BAR_WIDTH = 20;
    private static final int CARD_HEIGHT = 6;
    private static final int MAX_CARD_WIDTH = 16;

    private CombatScreen() {
    }

    static void draw(Painter p, GameState state, ViewState view, long seed) {
        p.frame("Cardistry", "Tour " + state.turn() + " · " + state.choomies() + " Choomies · Seed " + seed,
                state.hasTrinket(cardistry.items.ItemRegistry.CTRL_Z) ? HELP_UNDO : HELP);
        drawEnemy(p, state, view);
        drawEffects(p, state);
        drawSprite(p, state, view);
        drawTerrainAndPlayer(p, state, view);
        p.separator(11, "Ta main " + state.hand().size() + "/" + state.handSize() + (state.usesDeck()
                ? " · Pioche " + state.drawPile().size() + " · Défausse " + state.discardPile().size() : ""));
        drawUndoStatus(p, state);
        drawHand(p, state, view);
        drawDetail(p, state, view);
        p.separator(20, "Journal");
        if (view.toast != null) {
            p.text(2, 20, Painter.fit(" ★ " + view.toast + " ", p.cols() - 4), Palette.ACCENT, true);
        }
        int packCol = drawPack(p, state);
        drawLog(p, view, packCol);
        drawFloating(p, view);
    }

    private static void drawEnemy(Painter p, GameState state, ViewState view) {
        Enemy enemy = state.currentEnemy();
        p.text(2, 1, cardistry.ui.Format.location(state), Palette.DIM);
        p.text(2, 2, enemy.name().toUpperCase(), Palette.element(enemy.element()), true);
        p.text(3 + enemy.name().length(), 2, "· " + enemy.element().displayName(), Palette.element(enemy.element()));

        double hp = view.enemyHpShown != null ? view.enemyHpShown : enemy.hp();
        hpLine(p, 3, hp, enemy.maxHp(), Palette.DAMAGE, null,
                state.statuses(cardistry.core.status.StatusTarget.ENEMY).all());

        double mult = state.enemyAttackMultiplier();
        int width = p.cols() - SPRITE_WIDTH - 3 - 2;
        String intent = "Prochain : " + cardistry.ui.IntentText.intent(state, enemy);
        String note = mult != 1.0 ? " (×" + num(mult) + ")" : "";
        String fitted = Painter.fit(intent, Math.max(1, width - note.length()));
        p.text(2, 4, fitted, Palette.TEXT);
        if (!note.isEmpty()) {
            p.text(2 + fitted.length(), 4, note, mult > 1.0 ? Palette.DAMAGE : Palette.HEAL);
        }
    }

    /**
     * Ligne 5 : effets en cours avec leur durée, dans l'ordre d'arrivée. Format long si tout tient,
     * sinon court ; au-delà, « +N ».
     */
    private static void drawEffects(Painter p, GameState state) {
        List<TemporaryEffect> effects = state.effects();
        if (effects.isEmpty()) {
            return;
        }
        int width = p.cols() - SPRITE_WIDTH - 3 - 2;
        List<String> labels = effects.stream().map(TemporaryEffect::describe).toList();
        if (String.join(" · ", labels).length() > width) {
            labels = effects.stream().map(CombatScreen::compact).toList();
        }
        int col = 2;
        for (int i = 0; i < effects.size(); i++) {
            String label = labels.get(i);
            int remaining = effects.size() - i - 1;
            int room = width + 2 - col - (remaining > 0 ? 4 : 0);
            if (label.length() > room) {
                String more = "+" + (effects.size() - i);
                if (i == 0) {
                    p.text(col, 5, Painter.fit(label, Math.max(1, room)), effectColor(effects.get(i)));
                    more = remaining > 0 ? "+" + remaining : "";
                    col += Math.max(1, room) + 1;
                }
                p.text(col, 5, more, Palette.DIM);
                return;
            }
            p.text(col, 5, label, effectColor(effects.get(i)));
            col += label.length();
            if (remaining > 0) {
                p.text(col, 5, " · ", Palette.DIM);
                col += 3;
            }
        }
    }

    /** « Space cake (dans 3) », « Space cake (2 tours) », « Shot de Jäger ». */
    static String compact(TemporaryEffect effect) {
        if (!effect.isActive()) {
            return effect.name() + " (dans " + effect.delayLeft() + ")";
        }
        if (effect.turnsLeft() != null) {
            return effect.name() + " (" + effect.turnsLeft() + (effect.turnsLeft() > 1 ? " tours)" : " tour)");
        }
        return effect.name();
    }

    private static TextColor effectColor(TemporaryEffect effect) {
        return effect.isActive() ? Palette.INFO : Palette.DIM;
    }

    private static void drawSprite(Painter p, GameState state, ViewState view) {
        if (view.enemyHidden) {
            return;
        }
        Enemy enemy = state.currentEnemy();
        int areaLeft = p.cols() - SPRITE_WIDTH - 2;
        if (drawSpriteImage(p, enemy, view, areaLeft)) {
            return;
        }
        cardistry.ui.sprite.Sprite sprite = cardistry.ui.sprite.Sprites.of(enemy);
        cardistry.ui.sprite.Sprite.Frame frame = frameFor(sprite, view);
        int col = areaLeft + Math.max(0, (SPRITE_WIDTH - sprite.width()) / 2) + view.enemyOffsetX;
        int top = 1 + Math.max(0, (10 - sprite.height()) / 2);
        TextColor base = view.enemyDim ? Palette.DIM : Palette.element(enemy.element());
        SpriteArt.draw(p, frame, col, top, 10, base, view.enemyFlash, p.cols() - 1);
    }

    /**
     * Image PNG de l'adversaire (fenêtre seulement), dans la zone du sprite : la frame de
     * l'animation en cours, en blanc pendant le clignotement des dégâts, à demi transparente quand il
     * meurt.
     *
     * @return {@code false} s'il n'y a pas d'image (dessiner le sprite ASCII)
     */
    static boolean drawSpriteImage(Painter p, Enemy enemy, ViewState view, int areaLeft) {
        if (!p.hasImages()) {
            return false;
        }
        boolean idle = view.spriteAnimation.equals(cardistry.ui.sprite.Sprite.IDLE);
        int idleFrames = Math.max(1, cardistry.ui.window.Images.idleFrames(enemy.spriteId()));
        int frame = idle ? Math.floorMod(view.idleFrame, idleFrames) : view.spriteFrame;
        return cardistry.ui.window.Images.enemy(enemy.spriteId(), view.spriteAnimation, frame)
                .map(path -> p.image(areaLeft + view.enemyOffsetX, 1, SPRITE_WIDTH, 10, path, view.enemyFlash,
                        view.enemyDim ? 0.5f : 1f))
                .orElse(false);
    }

    /** Frame à dessiner : celle de l'animation en cours si le sprite l'a, sinon la frame idle. */
    static cardistry.ui.sprite.Sprite.Frame frameFor(cardistry.ui.sprite.Sprite sprite, ViewState view) {
        List<cardistry.ui.sprite.Sprite.Frame> frames = sprite.frames(view.spriteAnimation);
        if (!frames.isEmpty()) {
            return frames.get(Math.min(view.spriteFrame, frames.size() - 1));
        }
        List<cardistry.ui.sprite.Sprite.Frame> idle = sprite.idle();
        return idle.get(Math.floorMod(view.idleFrame, idle.size()));
    }

    private static void drawTerrainAndPlayer(Painter p, GameState state, ViewState view) {
        TerrainCard terrain = state.terrain();
        p.text(2, 6, "Terrain", Palette.DIM);
        TextColor terrainColor = view.terrainFlash != null ? view.terrainFlash : Palette.element(terrain.element());
        p.text(11, 6, Format.terrainLabel(state), terrainColor, true);

        p.text(2, 7, Format.playerLabel(state), Palette.TEXT, true);
        drawDrunkennessAndCurses(p, state);
        double hp = view.playerHpShown != null ? view.playerHpShown : state.player().hp();
        hpLine(p, 8, hp, state.maxHp(), Palette.health(hp / state.maxHp()), view.playerFlash,
                state.statuses(cardistry.core.status.StatusTarget.PLAYER).all());
        drawItems(p, state);
        drawTrinkets(p, state);
    }

    /** Ligne 7, après « TOI » : Ivresse (si non nulle) et malédictions. */
    private static void drawDrunkennessAndCurses(Painter p, GameState state) {
        int col = 2 + Format.playerLabel(state).length() + 1;
        int limit = p.cols() - SPRITE_WIDTH - 3;
        if (state.drunkenness() > 0) {
            // Ligne étroite : le palier sans son pourcentage (il est dans le journal et les règles).
            String tier = state.drunkTier().displayName();
            String text = "Ivresse " + state.drunkenness() + "/" + state.hangoverThreshold()
                    + (tier.isEmpty() ? "" : " " + tier);
            boolean over = state.drunkenness() >= state.hangoverThreshold();
            p.text(col, 7, text, over ? Palette.DAMAGE : Palette.WARNING, true);
            col += text.length() + 2;
        }
        for (Curse curse : state.activeCurses()) {
            String text = Painter.fit(curse.displayName() + " (" + state.describeCurse(curse, state.activeIntensity(curse)) + ")",
                    Math.max(1, limit - col));
            p.text(col, 7, text, Palette.DAMAGE);
            col += text.length() + 2;
        }
        for (Curse curse : state.pendingCurses()) {
            if (col < limit - 4) {
                p.text(col, 7, Painter.fit(curse.displayName() + " à venir", limit - col), Palette.DIM);
            }
        }
    }

    /** Ligne 9 : [W] Maté  [E] ·  [R] · — chaque emplacement a la même largeur. */
    private static void drawItems(Painter p, GameState state) {
        int limit = p.cols() - SPRITE_WIDTH - 3;
        int slots = state.itemSlots();
        int slotWidth = Math.max(6, (limit - 2) / Math.max(1, slots));
        for (int i = 0; i < slots; i++) {
            int col = 2 + i * slotWidth;
            if (col + 5 > limit) {
                break;
            }
            String key = "[" + Character.toUpperCase(ItemKeys.key(i)) + "] ";
            p.text(col, 9, key, Palette.DIM);
            if (i < state.items().size()) {
                Item item = state.items().get(i);
                String name = Painter.fit(item.name(), slotWidth - key.length() - 1);
                p.text(col + key.length(), 9, name, Palette.rarity(item.rarity()), true);
            } else {
                p.text(col + key.length(), 9, "·", Palette.DIM);
            }
        }
    }

    /** Ligne 10 : trinkets équipés, colorés par rareté. */
    /** « Ctrl+Z : disponible / utilisé » à droite du séparateur de la main, si le trinket est équipé. */
    static String undoStatus(GameState state) {
        if (!state.hasTrinket(cardistry.items.ItemRegistry.CTRL_Z)) {
            return "";
        }
        return "Ctrl+Z : " + (state.undoUsedThisCombat() ? "utilisé" : "disponible");
    }

    private static void drawUndoStatus(Painter p, GameState state) {
        String status = undoStatus(state);
        if (!status.isEmpty()) {
            p.text(p.cols() - status.length() - 4, 11, " " + status + " ",
                    state.undoUsedThisCombat() ? Palette.DIM : Palette.SECRET, !state.undoUsedThisCombat());
        }
    }

    private static void drawTrinkets(Painter p, GameState state) {
        String label = "Trinkets " + state.trinkets().size() + "/" + state.trinketSlots();
        p.text(2, 10, label, Palette.DIM);
        int col = 3 + label.length();
        int limit = p.cols() - SPRITE_WIDTH - 3;
        List<Trinket> trinkets = state.trinkets();
        for (int i = 0; i < trinkets.size(); i++) {
            int room = limit - col;
            if (room < 4) {
                p.text(limit - 1, 10, "…", Palette.DIM);
                break;
            }
            Trinket trinket = trinkets.get(i);
            String name = Painter.fit(trinket.name(), Math.min(room, 28));
            p.text(col, 10, name, Palette.rarity(trinket.rarity()));
            col += name.length() + 2;
        }
        if (trinkets.isEmpty()) {
            p.text(col, 10, "aucun", Palette.DIM);
        }
    }

    /** Barre et « PV/max » ; au-dessus du max (Ross), barre et PV en doré. */
    /** « PV ████████ 82/100 Int3 Fon1 » : barre, valeur, puis les statuts. */
    private static void hpLine(Painter p, int row, double hp, double max, TextColor barColor, TextColor flash,
            List<cardistry.core.status.Status> statuses) {
        boolean overMax = hp > max;
        p.text(2, row, "PV", Palette.DIM);
        TextColor color = flash != null ? flash : overMax ? Palette.ACCENT : barColor;
        p.text(6, row, Painter.bar(hp, max, BAR_WIDTH), color, flash != null || overMax);
        String current = num(Math.max(0, hp));
        p.text(7 + BAR_WIDTH, row, current, overMax ? Palette.ACCENT : Palette.TEXT, overMax);
        String maxText = "/" + num(max);
        p.text(7 + BAR_WIDTH + current.length(), row, maxText, Palette.TEXT);
        int col = 8 + BAR_WIDTH + current.length() + maxText.length();
        StatusBadges.draw(p, col, row, statuses, Math.max(0, p.cols() - SPRITE_WIDTH - 3 - col));
    }

    // --- Main ---

    static int cardWidth(int cols, int slots) {
        return Math.min(MAX_CARD_WIDTH, (cols - 4 - (slots - 1)) / slots);
    }

    private static void drawHand(Painter p, GameState state, ViewState view) {
        int slots = Math.max(state.handSize(), state.hand().size()); // main au-delà du max : rien de caché
        int width = cardWidth(p.cols(), slots);
        int total = slots * width + (slots - 1);
        int left = 2 + Math.max(0, (p.cols() - 4 - total) / 2);
        List<Card> hand = state.hand();
        for (int i = 0; i < slots; i++) {
            int col = left + i * (width + 1);
            if (i < hand.size()) {
                boolean selected = i == view.selected && !view.confirmingQuit;
                drawCard(p, col, selected ? 12 : 13, width, i + 1, hand.get(i), selected, i == view.highlightSlot);
            } else {
                drawEmptySlot(p, col, 13, width, i + 1);
            }
        }
    }

    static void drawCard(Painter p, int col, int row, int width, int number, Card card,
            boolean selected, boolean highlight) {
        TextColor color = highlight ? Palette.ACCENT : Palette.element(card.element());
        p.box(col, row, width, CARD_HEIGHT, color, selected);
        p.text(col + 1, row, String.valueOf(number), selected ? Palette.ACCENT : Palette.TEXT, true);

        int inner = width - 2;
        List<String> name = Format.wrap(card.name(), inner);
        if (name.size() > 2) {
            name = List.of(name.get(0), Painter.fit(name.get(1) + "…", inner));
        }
        for (int i = 0; i < name.size(); i++) {
            p.text(col + 1, row + 1 + i, name.get(i), Palette.TEXT, selected);
        }
        String element = card.element() == null ? "null" : card.element().displayName();
        p.text(col + 1, row + 3, Painter.fit(element, inner), Palette.element(card.element()));
        if (card.power().isPresent()) {
            p.text(col + width - 2, row, "★", Palette.ACCENT, true);
        }
        String value = switch (card) {
            case SpellCard s -> s.damageLabel().orElse(num(s.damage())) + " dgt";
            case HealCard h -> "+" + num(h.amount()) + " PV";
            case TerrainCard t -> "terrain";
            case DeadCard d -> "bloquée";
        };
        p.text(col + 1, row + 4, Painter.fit(value, inner), Palette.DIM);
    }

    private static void drawEmptySlot(Painter p, int col, int row, int width, int number) {
        p.box(col, row, width, CARD_HEIGHT, Palette.DIM, false);
        p.text(col + 1, row, String.valueOf(number), Palette.DIM);
        p.text(col + Math.max(1, (width - 4) / 2), row + 2, "vide", Palette.DIM);
    }

    private static void drawDetail(Painter p, GameState state, ViewState view) {
        List<Card> hand = state.hand();
        if (hand.isEmpty()) {
            p.text(2, 19, "Ta main est vide : appuie sur P pour piocher.", Palette.DIM);
            return;
        }
        Card card = hand.get(Math.min(view.selected, hand.size() - 1));
        p.text(2, 19, Painter.fit(describe(state, card), p.cols() - 4), Palette.TEXT);
    }

    /** Effet de la carte si on la joue maintenant, multiplicateurs compris. */
    static String describe(GameState state, Card card) {
        String power = card.power().map(p -> " · ★ " + p.text()).orElse("");
        return effectOf(state, card) + power;
    }

    private static String effectOf(GameState state, Card card) {
        Enemy enemy = state.currentEnemy();
        String head = card.name() + " · ";
        return switch (card) {
            case SpellCard s when s.hasDamageRule() || s.isUnmultiplied() -> {
                double mult = s.isUnmultiplied() ? 1.0 : state.spellMultiplier(s);
                String damage = s.damageLabel().orElse(num(s.damage()));
                yield head + "sortilège " + s.element().displayName() + " : " + damage
                        + (s.isUnmultiplied() ? " (non multipliés)" : mult != 1.0 ? " ×" + num(mult) : "")
                        + " dégâts à " + enemy.name();
            }
            case SpellCard s when s.damage() <= 0 -> head + "sortilège " + s.element().displayName() + " : aucun dégât";
            case SpellCard s -> {
                double mult = state.spellMultiplier(s);
                yield head + "sortilège " + s.element().displayName() + " : " + num(s.damage())
                        + (mult != 1.0 ? " ×" + num(mult) : "") + " = " + num(cardistry.model.Combatant.round(s.damage() * mult))
                        + " dégâts à " + enemy.name();
            }
            case HealCard h -> {
                double mult = state.healMultiplier(h);
                double missing = Math.max(0, state.maxHp() - state.player().hp());
                double healed = Math.min(h.amount() * mult, missing);
                yield head + "soin " + h.element().displayName() + " : " + num(h.amount())
                        + (mult != 1.0 ? " ×" + num(mult) : "") + " → +" + num(healed) + " PV"
                        + (healed < h.amount() * mult ? " (PV max)" : "");
            }
            case DeadCard d -> head + "injouable jusqu'à la fin du combat (NullPointerException)";
            case TerrainCard t -> {
                double now = state.enemyAttackMultiplier();
                double after = ElementRules.terrainAttackMultiplier(t.element(), enemy.element());
                yield head + "terrain " + t.element().displayName() + " : attaque de " + enemy.name()
                        + " ×" + num(now) + " → ×" + num(after) + t.effectText().map(e -> " · " + e).orElse("");
            }
        };
    }

    // --- Journal et effets ---

    /**
     * Paquet de clopes en bas à droite (2 lignes) :
     * <pre>
     *  ┌╥╥╥──┐ 12/20 clopes   Honoré [###..]
     *  │CLOPE│ F fumer · G rouler
     * </pre>
     *
     * @return colonne où commence le paquet (le journal s'arrête avant)
     */
    static int drawPack(Painter p, GameState state) {
        int count = state.cigarettes();
        int max = Math.max(1, state.cigaretteMax());
        int sticks = count == 0 ? 0 : (int) Math.ceil(5.0 * count / max);
        String top = "┌" + "╥".repeat(sticks) + "─".repeat(5 - sticks) + "┐";
        String info = count + "/" + max + " clopes";
        String gauge = state.trinkets().stream().flatMap(t -> t.gauge().stream()).findFirst()
                .map(g -> "   " + g.label() + " [" + "#".repeat(g.value()) + ".".repeat(g.max() - g.value()) + "]")
                .orElse("");
        int width = Math.max(top.length() + 1 + info.length() + gauge.length(), top.length() + 1 + 18);
        int col = p.cols() - 2 - width;
        int row = p.rows() - 3;
        p.text(col, row, top, count == 0 ? Palette.DIM : Palette.TEXT);
        p.text(col + top.length() + 1, row, info, count == 0 ? Palette.WARNING : Palette.TEXT, true);
        if (!gauge.isEmpty()) {
            p.text(col + top.length() + 1 + info.length(), row, gauge, Palette.ACCENT);
        }
        p.text(col, row + 1, "│CLOPE│", Palette.TEXT);
        p.text(col + top.length() + 1, row + 1, "F fumer · G rouler", Palette.DIM);
        return col;
    }

    private static void drawLog(Painter p, ViewState view, int packCol) {
        int first = 21;
        int last = p.rows() - 2;
        int capacity = last - first + 1;
        List<ViewState.LogLine> log = view.log;
        int start = Math.max(0, log.size() - capacity);
        for (int i = start; i < log.size(); i++) {
            ViewState.LogLine line = log.get(i);
            boolean latest = i == log.size() - 1;
            int row = first + (i - start);
            int width = row >= p.rows() - 3 ? packCol - 4 : p.cols() - 4; // le paquet occupe les 2 dernières lignes
            p.text(2, row, Painter.fit(line.text(), Math.max(1, width)), latest ? line.color() : Palette.DIM, latest);
        }
    }

    private static void drawFloating(Painter p, ViewState view) {
        for (ViewState.Floating f : view.floating) {
            if (f.onEnemy()) {
                int col = p.cols() - SPRITE_WIDTH / 2 - 2 - f.text().length() / 2;
                p.text(col, Math.max(1, 5 - f.rise()), f.text(), f.color(), true);
            } else {
                p.text(9 + BAR_WIDTH + 8, Math.max(1, 8 - f.rise()), f.text(), f.color(), true);
            }
        }
    }
}

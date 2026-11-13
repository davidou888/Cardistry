package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.ui.Format;
import com.googlecode.lanterna.TextColor;
import java.util.List;

/**
 * Écran de combat (80×24 minimum).
 *
 * <pre>
 *  ligne 0        cadre : titre, tour, seed
 *  lignes 1-10    adversaire (infos à gauche, sprite à droite), terrain, joueur
 *  ligne 11       séparateur « Ta main »
 *  lignes 12-18   cartes (la carte sélectionnée est surélevée d'une ligne)
 *  ligne 19       détail de la carte sélectionnée
 *  ligne 20       séparateur « Journal »
 *  lignes 21..    journal (s'agrandit avec le terminal)
 *  dernière       aide clavier
 * </pre>
 */
final class CombatScreen {

    static final String HELP = "←/→ choisir · Entrée jouer · 1-7 jouer · P piocher · Q quitter";
    private static final int SPRITE_WIDTH = 30;
    private static final int BAR_WIDTH = 20;
    private static final int CARD_HEIGHT = 6;
    private static final int MAX_CARD_WIDTH = 16;

    private CombatScreen() {
    }

    static void draw(Painter p, GameState state, ViewState view, long seed) {
        p.frame("Cardistry", "Tour " + state.turn() + " · Seed " + seed,
                view.confirmingQuit ? "" : HELP);
        drawEnemy(p, state, view);
        drawSprite(p, state, view);
        drawTerrainAndPlayer(p, state, view);
        p.separator(11, "Ta main " + state.hand().size() + "/" + state.handSize());
        drawHand(p, state, view);
        drawDetail(p, state, view);
        p.separator(20, "Journal");
        drawLog(p, view);
        drawFloating(p, view);
        if (view.confirmingQuit) {
            p.text(2, p.rows() - 1, " Quitter la partie ? [O]ui / [N]on ", Palette.WARNING, true);
        }
    }

    private static void drawEnemy(Painter p, GameState state, ViewState view) {
        Enemy enemy = state.currentEnemy();
        int index = state.enemies().indexOf(enemy) + 1;
        p.text(2, 1, "Adversaire " + index + "/" + state.enemies().size(), Palette.DIM);
        p.text(2, 2, enemy.name().toUpperCase(), Palette.element(enemy.element()), true);
        p.text(3 + enemy.name().length(), 2, "· " + enemy.element().displayName(), Palette.element(enemy.element()));

        double hp = view.enemyHpShown != null ? view.enemyHpShown : enemy.hp();
        hpLine(p, 3, hp, enemy.maxHp(), Palette.DAMAGE, null);

        double mult = state.enemyAttackMultiplier();
        String attack = "Attaque " + num(enemy.attack());
        if (mult != 1.0) {
            attack += " ×" + num(mult) + " = " + num(enemy.attack() * mult);
        }
        p.text(2, 4, attack, Palette.TEXT);
        if (mult > 1.0) {
            p.text(3 + attack.length(), 4, "(renforcé par le terrain)", Palette.DAMAGE);
        } else if (mult < 1.0) {
            p.text(3 + attack.length(), 4, "(affaibli par le terrain)", Palette.HEAL);
        }
    }

    private static void drawSprite(Painter p, GameState state, ViewState view) {
        if (view.enemyHidden) {
            return;
        }
        Enemy enemy = state.currentEnemy();
        List<String> sprite = Resources.enemySprite(enemy.name());
        int width = sprite.stream().mapToInt(String::length).max().orElse(0);
        int areaLeft = p.cols() - SPRITE_WIDTH - 2;
        int col = areaLeft + Math.max(0, (SPRITE_WIDTH - width) / 2) + view.enemyOffsetX;
        int top = 1 + Math.max(0, (10 - sprite.size()) / 2);
        TextColor color = view.enemyFlash != null ? view.enemyFlash
                : view.enemyDim ? Palette.DIM : Palette.element(enemy.element());
        for (int i = 0; i < sprite.size() && top + i <= 10; i++) {
            // Ne pas déborder sur le cadre de droite.
            String line = sprite.get(i);
            int maxLen = p.cols() - 1 - col;
            if (maxLen > 0) {
                p.text(col, top + i, Painter.fit(line, maxLen), color, view.enemyFlash != null);
            }
        }
    }

    private static void drawTerrainAndPlayer(Painter p, GameState state, ViewState view) {
        TerrainCard terrain = state.terrain();
        p.text(2, 6, "Terrain", Palette.DIM);
        TextColor terrainColor = view.terrainFlash != null ? view.terrainFlash : Palette.element(terrain.element());
        p.text(11, 6, terrain.name() + " (" + terrain.element().displayName() + ")", terrainColor, true);

        p.text(2, 8, "TOI", Palette.TEXT, true);
        double hp = view.playerHpShown != null ? view.playerHpShown : state.player().hp();
        hpLine(p, 9, hp, state.maxHp(), Palette.health(hp / state.maxHp()), view.playerFlash);
    }

    private static void hpLine(Painter p, int row, double hp, double max, TextColor barColor, TextColor flash) {
        p.text(2, row, "PV", Palette.DIM);
        p.text(6, row, Painter.bar(hp, max, BAR_WIDTH), flash != null ? flash : barColor, flash != null);
        p.text(7 + BAR_WIDTH, row, num(Math.max(0, hp)) + "/" + num(max), Palette.TEXT);
    }

    // --- Main ---

    static int cardWidth(int cols, int slots) {
        return Math.min(MAX_CARD_WIDTH, (cols - 4 - (slots - 1)) / slots);
    }

    private static void drawHand(Painter p, GameState state, ViewState view) {
        int slots = state.handSize();
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

    private static void drawCard(Painter p, int col, int row, int width, int number, Card card,
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
        p.text(col + 1, row + 3, Painter.fit(card.element().displayName(), inner), Palette.element(card.element()));
        String value = switch (card) {
            case SpellCard s -> num(s.damage()) + " dgt";
            case HealCard h -> "+" + num(h.amount()) + " PV";
            case TerrainCard t -> "terrain";
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
        Enemy enemy = state.currentEnemy();
        String head = card.name() + " · ";
        return switch (card) {
            case SpellCard s -> {
                double mult = state.spellMultiplier(s);
                yield head + "sortilège " + s.element().displayName() + " : " + num(s.damage())
                        + (mult != 1.0 ? " ×" + num(mult) : "") + " = " + num(s.damage() * mult)
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
            case TerrainCard t -> {
                double now = state.enemyAttackMultiplier();
                double after = ElementRules.terrainAttackMultiplier(t.element(), enemy.element());
                yield head + "terrain " + t.element().displayName() + " : attaque de " + enemy.name()
                        + " ×" + num(now) + " → ×" + num(after);
            }
        };
    }

    // --- Journal et effets ---

    private static void drawLog(Painter p, ViewState view) {
        int first = 21;
        int last = p.rows() - 2;
        int capacity = last - first + 1;
        List<ViewState.LogLine> log = view.log;
        int start = Math.max(0, log.size() - capacity);
        for (int i = start; i < log.size(); i++) {
            ViewState.LogLine line = log.get(i);
            boolean latest = i == log.size() - 1;
            p.text(2, first + (i - start), Painter.fit(line.text(), p.cols() - 4),
                    latest ? line.color() : Palette.DIM, latest);
        }
    }

    private static void drawFloating(Painter p, ViewState view) {
        for (ViewState.Floating f : view.floating) {
            if (f.onEnemy()) {
                int col = p.cols() - SPRITE_WIDTH / 2 - 2 - f.text().length() / 2;
                p.text(col, Math.max(1, 5 - f.rise()), f.text(), f.color(), true);
            } else {
                p.text(9 + BAR_WIDTH + 8, Math.max(1, 9 - f.rise()), f.text(), f.color(), true);
            }
        }
    }
}

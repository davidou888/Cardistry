package cardistry.ui.lanterna;

import cardistry.core.GameState;
import cardistry.ui.StatSheet;
import java.util.List;

/**
 * Fiche des 6 stats : le joueur (touche C, n'importe où pendant une partie) ou l'adversaire (touche
 * A, en combat). Chaque stat : sa valeur, son effet, et le détail de ses bonus.
 */
final class StatsScreen {

    private StatsScreen() {
    }

    static void draw(Painter p, GameState state, boolean enemy, long seed) {
        String name = enemy ? state.currentEnemy().name() : state.playerName();
        p.frame("Fiche de " + name, "Seed " + seed, (enemy ? "A" : "C") + " / Échap / Entrée : fermer");
        List<StatSheet.Row> rows = enemy ? StatSheet.enemy(state) : StatSheet.player(state);
        int top = Math.max(2, p.rows() / 2 - rows.size() - 2);
        String head = enemy
                ? name + " · " + state.currentEnemy().element().displayName() + " · PV " + cardistry.ui.Format.num(state.currentEnemy().hp())
                        + "/" + cardistry.ui.Format.num(state.currentEnemy().maxHp())
                : name + " · PV " + cardistry.ui.Format.num(state.player().hp()) + "/" + cardistry.ui.Format.num(state.maxHp());
        p.centered(top, head, Palette.ACCENT, true);
        int width = p.cols() - 6;
        int row = top + 2;
        for (StatSheet.Row r : rows) {
            String effect = enemy ? "" : " : " + r.stat().effectText(); // les effets sont écrits pour toi
            p.text(3, row, Painter.fit(String.format("%-7s", r.head()) + r.stat().displayName() + effect, width),
                    Palette.TEXT, true);
            String detail = r.detail();
            p.text(10, row + 1, Painter.fit(detail.isEmpty() ? "—" : detail, width - 7), Palette.DIM);
            row += 2;
        }
        if (enemy) {
            p.text(3, row + 1, Painter.fit("Ses coups : + sa FOR, − ta DEF ; 3 % de critique par CHA.", width),
                    Palette.DIM);
            p.text(3, row + 2, Painter.fit("Tes sorts : − sa DEF. Statuts : − 5 % par sa RES (5 : un tour de moins).",
                    width), Palette.DIM);
        }
    }
}

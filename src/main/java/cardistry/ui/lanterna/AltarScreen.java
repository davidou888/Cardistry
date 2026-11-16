package cardistry.ui.lanterna;


import com.googlecode.lanterna.TextColor;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.model.Element;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.ui.Format;
import java.util.List;


final class AltarScreen {
static void draw(Painter p, GameState state, int altarStat, int altarButton, long seed) {
    p.frame("Autel", "Niveau " + state.level() + " · Seed " + seed,
            "↑/↓ statistique · ←/→ action · Entrée valider · Échap pause");
    List<cardistry.core.stats.StatType> stats = cardistry.core.stats.StatType.characterStats();

    // Problème 2 : on s'adapte à la hauteur de l'écran
    boolean compact = p.rows() < 30;   // 80×24 : pas de ligne vide entre les stats
    int gap = compact ? 1 : 2;
    int row = 2;                       // on avance ligne par ligne au lieu de calculer top + 5 + …

    // Ambiance
    p.centered(row, "Un petit autel, au centre un plant de weed et un briquet...", Palette.ACCENT, true);
    row += 2;

    // Problème 3 : la grande barre de vie + l'aperçu
    int hp = state.player().hp();
    double max = state.maxHp();
    double cost = cardistry.core.CombatEngine.ALTAR_MAX_HP_COST;
    double newMax = max - cost;
    double newHp = hp > max ? hp - cost : Math.min(hp, newMax);   // même règle que sacrifice()
    boolean canSacrifice = newMax >= 10;                            // mets la même limite que dans ton moteur
    int barWidth = Math.max(10, p.cols() - 20);
    p.centered(row++, Painter.bar(hp, max, barWidth), Palette.health(hp / max), false);
    p.centered(row++, "PV " + num(hp) + "/" + num(max) + "   ->   " + num(newHp) + "/" + num(newMax),
            Palette.TEXT, true);
    row++;
    p.centered(row, "Sacrifier votre santé contre des statistiques ?", Palette.TEXT, false);
    row += 2;

    // Tableau des stats (ton code, avec « +1 » sur la ligne choisie)
    String[] lines = new String[stats.size()];
    for (int i = 0; i < stats.size(); i++) {
        cardistry.core.stats.StatType stat = stats.get(i);
        lines[i] = "[" + (i + 1) + "] " + stat.abbreviation() + " " + num(state.playerStats().get(stat)) + "  "
                + stat.displayName() + " : " + stat.effectText();
    }
    int width = java.util.Arrays.stream(lines).mapToInt(String::length).max().orElse(0) + 8;
    int col = Math.max(2, (p.cols() - width) / 2);
    for (int i = 0; i < lines.length; i++) {
        boolean isSelected = i == altarStat;
        String line = (isSelected ? "> " : "  ") + lines[i] + (isSelected && canSacrifice ? "   +1" : "");
        p.text(col, row, Painter.fit(line, p.cols() - col - 2),
                isSelected ? Palette.ACCENT : Palette.TEXT, isSelected);
        row += gap;
    }

    // Problème 1 : les boutons placés l'un APRÈS l'autre, puis centrés ensemble
    row = Math.min(row + 1, p.rows() - 3);     // jamais sous le cadre
    String sacrifice = (altarButton == 1 ? "> " : "  ") + "[ Pomme de terre ]";
    String leave = (altarButton == 0 ? "> " : "  ") + "[ Partir (en toussant...) ]";
    int space = 6;
    int start = Math.max(2, (p.cols() - (sacrifice.length() + space + leave.length())) / 2);
    com.googlecode.lanterna.TextColor sacrificeColor = !canSacrifice ? Palette.DIM
            : altarButton == 1 ? Palette.ACCENT : Palette.TEXT;
    p.text(start, row, sacrifice, sacrificeColor, altarButton == 1);
    p.text(start + sacrifice.length() + space, row, leave,
            altarButton == 0 ? Palette.ACCENT : Palette.TEXT, altarButton == 0);
    }
}

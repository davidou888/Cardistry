package cardistry.ui;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiche des 6 stats (FOR, INT, ESP, DEF, RES, CHA) du joueur ou de l'adversaire, avec le détail de
 * chaque bonus (personnage, progression, trinkets, terrain, combat…). Partagée par les trois
 * affichages (touche C pour le joueur, A pour l'adversaire).
 */
public final class StatSheet {

    /** Un bonus : d'où il vient et ce qu'il apporte (« +2 », « ×2 »). */
    public record Part(String label, String value) {
    }

    /** Une stat : sa valeur finale et ce qui la compose. */
    public record Row(StatType stat, double total, List<Part> parts) {

        /** « FOR 5 » */
        public String head() {
            return stat.abbreviation() + " " + (total < 0 ? "−" + num(-total) : num(total));
        }

        /** « personnage +3, Donut Musclé +2, terrain Dojo +1 », ou vide. */
        public String detail() {
            return String.join(", ", parts.stream().map(p -> p.label() + " " + p.value()).toList());
        }
    }

    private StatSheet() {
    }

    /** Les 6 stats du joueur, avec la provenance de chaque bonus. */
    public static List<Row> player(GameState state) {
        return rows(state, state.playerStats());
    }

    /** Les 6 stats de l'adversaire actuel (base de son registre, niveau, malus de combat). */
    public static List<Row> enemy(GameState state) {
        return rows(state, state.currentEnemy().stats());
    }

    private static List<Row> rows(GameState state, Stats stats) {
        List<Row> rows = new ArrayList<>();
        for (StatType stat : StatType.characterStats()) {
            List<Part> parts = new ArrayList<>();
            if (stats.base(stat) != 0) {
                parts.add(new Part("base", signed(stats.base(stat))));
            }
            for (StatModifier m : stats.modifiers()) {
                if (m.stat() != stat || m.value() == 0) {
                    continue;
                }
                String value = switch (m.kind()) {
                    case FLAT -> signed(m.value());
                    case PERCENT -> (m.value() >= 0 ? "+" : "−") + num(Math.abs(m.value() * 100)) + " %";
                    case FINAL -> "×" + num(m.value());
                };
                parts.add(new Part(label(state, m.source()), value));
            }
            rows.add(new Row(stat, stats.get(stat), parts));
        }
        return rows;
    }

    /** « +2 » ou « −1 ». */
    private static String signed(double value) {
        return (value >= 0 ? "+" : "−") + num(Math.abs(value));
    }

    /** Nom lisible d'une source de modificateur. */
    static String label(GameState state, String source) {
        if (source.startsWith("hero:")) {
            return "personnage";
        }
        if (source.startsWith("trinket:")) {
            String id = source.substring("trinket:".length()).replace(":dynamique", "");
            return state.trinkets().stream().filter(t -> t.id().equals(id)).map(t -> t.name()).findFirst().orElse(id);
        }
        if (source.startsWith("effet:")) {
            return state.effects().stream().filter(e -> e.modifierSource().equals(source))
                    .map(e -> e.name()).findFirst().orElse("effet");
        }
        return switch (source) {
            case cardistry.core.CombatEngine.PROGRESSION_SOURCE -> "progression";
            case cardistry.core.CombatEngine.BOSS_BONUS_SOURCE -> "boss";
            case cardistry.core.CombatEngine.COMBAT_BONUS_SOURCE -> "ce combat";
            case cardistry.core.CombatEngine.TERRAIN_SOURCE -> "terrain " + state.terrain().name();
            case "ivresse" -> "Ivresse";
            default -> source;
        };
    }
}

package cardistry.core.stats;

/**
 * Un modificateur de stat, apporté par une source (trinket, objet, effet…).
 *
 * @param source identifiant de ce qui l'a apporté, pour pouvoir le retirer
 */
public record StatModifier(StatType stat, Kind kind, double value, String source) {

    public enum Kind {
        /** Ajouté à la base : +2 taille de main. */
        FLAT,
        /** Pourcentage du total : 0.25 pour +25 %, -0.1 pour -10 %. Les pourcentages s'additionnent. */
        PERCENT,
        /** Multiplicateur final, appliqué après tout le reste : 2 pour ×2. Les finaux se multiplient. */
        FINAL
    }

    public static StatModifier flat(StatType stat, double value, String source) {
        return new StatModifier(stat, Kind.FLAT, value, source);
    }

    /** {@code multiply(HEAL_EFFICIENCY, 2, …)} = ×2 final, après les plats et les pourcentages. */
    public static StatModifier multiply(StatType stat, double factor, String source) {
        return new StatModifier(stat, Kind.FINAL, factor, source);
    }

    /** {@code percent(FIRE_DAMAGE, 0.25, …)} = +25 % de dégâts feu. */
    public static StatModifier percent(StatType stat, double percent, String source) {
        return new StatModifier(stat, Kind.PERCENT, percent, source);
    }
}

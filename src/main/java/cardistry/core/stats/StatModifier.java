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
        PERCENT
    }

    public static StatModifier flat(StatType stat, double value, String source) {
        return new StatModifier(stat, Kind.FLAT, value, source);
    }

    /** {@code percent(FIRE_DAMAGE, 0.25, …)} = +25 % de dégâts feu. */
    public static StatModifier percent(StatType stat, double percent, String source) {
        return new StatModifier(stat, Kind.PERCENT, percent, source);
    }
}

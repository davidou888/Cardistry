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
        /** Multiplie le total : ×1.25 pour +25 %. */
        MULTIPLY
    }

    public static StatModifier flat(StatType stat, double value, String source) {
        return new StatModifier(stat, Kind.FLAT, value, source);
    }

    public static StatModifier multiply(StatType stat, double factor, String source) {
        return new StatModifier(stat, Kind.MULTIPLY, factor, source);
    }
}

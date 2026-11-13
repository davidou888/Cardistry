package cardistry.core.stats;

import cardistry.model.Element;

/**
 * Stats modifiables du joueur, avec leur valeur de base.
 *
 * <p>Branchées dans le moteur : {@link #MAX_HP} (PV de départ et plafond des soins), {@link #HAND_SIZE},
 * {@link #TRINKET_SLOTS}, {@link #ITEM_SLOTS}, {@link #FIRE_DAMAGE}, {@link #WATER_DAMAGE},
 * {@link #NATURE_DAMAGE}, {@link #HEAL_EFFICIENCY}. Les autres sont déclarées pour les objets et
 * trinkets à venir, mais pas encore lues par le moteur.
 */
public enum StatType {
    /** Valeur de base provisoire : à fixer (voir GAME_DESIGN.md). */
    MAX_HP(100),
    HAND_SIZE(7),
    /** Emplacements de trinkets. */
    TRINKET_SLOTS(5),
    /** Emplacements d'objets. */
    ITEM_SLOTS(3),
    CARDS_PER_DRAW(1),
    /** Multiplicateur de l'or gagné. */
    GOLD_GAIN(1.0),
    /** Multiplicateur des dégâts des sorts de feu. */
    FIRE_DAMAGE(1.0),
    WATER_DAMAGE(1.0),
    NATURE_DAMAGE(1.0),
    /** Multiplicateur des soins reçus. */
    HEAL_EFFICIENCY(1.0),
    /** Dégâts retirés à chaque attaque subie. */
    DAMAGE_REDUCTION(0);

    private final double defaultBase;

    StatType(double defaultBase) {
        this.defaultBase = defaultBase;
    }

    public double defaultBase() {
        return defaultBase;
    }

    /** Stat du multiplicateur de dégâts pour un élément. */
    public static StatType damageMultiplier(Element element) {
        return switch (element) {
            case FIRE -> FIRE_DAMAGE;
            case WATER -> WATER_DAMAGE;
            case NATURE -> NATURE_DAMAGE;
        };
    }
}

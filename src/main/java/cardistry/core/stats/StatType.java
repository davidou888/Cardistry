package cardistry.core.stats;

import cardistry.model.Element;

/**
 * Stats modifiables du joueur, avec leur valeur de base.
 *
 * <p>Branchées dans le moteur : {@link #MAX_HP} (PV de départ et plafond des soins), {@link #HAND_SIZE},
 * {@link #TRINKET_SLOTS}, {@link #ITEM_SLOTS}, {@link #FIRE_DAMAGE}, {@link #WATER_DAMAGE},
 * {@link #NATURE_DAMAGE}, {@link #HEAL_EFFICIENCY}, les soins par élément, {@link #CARDS_PER_DRAW},
 * {@link #DAMAGE_REDUCTION}, {@link #ENEMY_DAMAGE}, {@link #HANGOVER_THRESHOLD}, {@link #LUCKY_ROLLS}. {@link #GOLD_GAIN} attend le shop.
 */
public enum StatType {
    /** Valeur de base provisoire : à fixer (voir GAME_DESIGN.md). */
    MAX_HP(100),
    HAND_SIZE(7),
    /** Cartes proposées à chaque pioche en combat (on en garde une) ; 1 = pioche normale. */
    DRAW_CHOICES(1),
    /** Cartes distribuées au début de chaque combat (sans dépasser la taille de main). */
    STARTING_HAND(5),
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
    /** Multiplicateur de tous les soins reçus (cartes, objets, trinkets). */
    HEAL_EFFICIENCY(1.0),
    /** Multiplicateur des cartes de soin de feu (en plus de {@link #HEAL_EFFICIENCY}). */
    FIRE_HEAL(1.0),
    WATER_HEAL(1.0),
    NATURE_HEAL(1.0),
    /** Dégâts retirés à chaque attaque adverse subie (jamais sous 0). */
    DAMAGE_REDUCTION(0),
    /** Multiplicateur des dégâts infligés par l'adversaire. */
    ENEMY_DAMAGE(1.0),
    /** Ivresse à partir de laquelle on attrape une Gueule de bois. */
    HANGOVER_THRESHOLD(3),
    /** Si &gt; 0 : les soins ne sont plus plafonnés par les PV max. */
    UNCAPPED_HEALING(0),
    /** Taille du paquet de clopes. */
    CIGARETTE_MAX(20),
    /** Chance (0 à 1) qu'une clope fumée se transforme en Petit pétou. */
    CIGARETTE_TRANSFORM_CHANCE(0),
    /** Si &gt; 0 : les tirages de chance en faveur du joueur sont faits deux fois (voir {@code core.Luck}). */
    LUCKY_ROLLS(0);

    private final double defaultBase;

    StatType(double defaultBase) {
        this.defaultBase = defaultBase;
    }

    public double defaultBase() {
        return defaultBase;
    }

    /** Stat du multiplicateur des cartes de soin d'un élément. */
    public static StatType healMultiplier(Element element) {
        return switch (element) {
            case FIRE -> FIRE_HEAL;
            case WATER -> WATER_HEAL;
            case NATURE -> NATURE_HEAL;
        };
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

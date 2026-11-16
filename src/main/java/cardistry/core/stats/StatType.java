package cardistry.core.stats;

import cardistry.model.Element;

/**
 * Stats modifiables du joueur, avec leur valeur de base.
 *
 * <p>Branchées dans le moteur : {@link #MAX_HP} (PV de départ et plafond des soins), {@link #HAND_SIZE},
 * {@link #TRINKET_SLOTS}, {@link #ITEM_SLOTS}, {@link #FIRE_DAMAGE}, {@link #WATER_DAMAGE},
 * {@link #NATURE_DAMAGE} et les dégâts des autres éléments, {@link #HEAL_EFFICIENCY}, les soins par élément, {@link #CARDS_PER_DRAW},
 * les 6 stats de personnage ({@link #STRENGTH} à {@link #LUCK}), {@link #ENEMY_DAMAGE}, {@link #HANGOVER_THRESHOLD}, {@link #HANGOVER_SEVERITY}, {@link #LUCKY_ROLLS}, {@link #CHOOMIES_GAIN}.
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
    /** Multiplicateur des Choomies gagnés en combat. */
    CHOOMIES_GAIN(1.0),
    /** Multiplicateur des dégâts des sorts de feu. */
    FIRE_DAMAGE(1.0),
    WATER_DAMAGE(1.0),
    NATURE_DAMAGE(1.0),
    NEUTRAL_DAMAGE(1.0),
    SPIRIT_DAMAGE(1.0),
    ALGEBRAIC_DAMAGE(1.0),
    GEOMETRIC_DAMAGE(1.0),
    GLITCH_DAMAGE(1.0),
    /** Multiplicateur de tous les soins reçus (cartes, objets, trinkets). */
    HEAL_EFFICIENCY(1.0),
    /** Multiplicateur des cartes de soin de feu (en plus de {@link #HEAL_EFFICIENCY}). */
    FIRE_HEAL(1.0),
    WATER_HEAL(1.0),
    NATURE_HEAL(1.0),
    NEUTRAL_HEAL(1.0),
    SPIRIT_HEAL(1.0),
    ALGEBRAIC_HEAL(1.0),
    GEOMETRIC_HEAL(1.0),
    GLITCH_HEAL(1.0),
    // --- Les 6 stats de personnage (joueur et adversaires, voir core.CombatFormula) ---
    /** FOR : +1 dégât sur les sorts feu, eau, plante, neutre ; +1 sur chaque coup d'un adversaire. */
    STRENGTH(0),
    /** INT : +1 dégât sur les sorts algébriques et géométriques ; +3 % de chance d'infliger un statut. */
    INTELLIGENCE(0),
    /** ESP : +1 dégât sur les sorts spirituels ; +1 PV sur chaque soin à PV fixes. */
    MIND(0),
    /** DEF : −1 dégât subi par coup (minimum 1). */
    DEFENSE(0),
    /** RES : −5 % de chance de subir un statut ; à 5 ou plus, statuts subis −1 tour (minimum 1). */
    RESISTANCE(0),
    /** CHA : +3 % de coup critique sur les sorts (et les coups d'un adversaire). */
    LUCK(0),
    /** Multiplicateur des dégâts infligés par l'adversaire. */
    ENEMY_DAMAGE(1.0),
    /** Ivresse à partir de laquelle on attrape une Gueule de bois. */
    HANGOVER_THRESHOLD(3),
    /** Multiplicateur du malus de Gueule de bois (×2 pour Louis). */
    HANGOVER_SEVERITY(1.0),
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
            case NEUTRAL -> NEUTRAL_HEAL;
            case SPIRIT -> SPIRIT_HEAL;
            case ALGEBRAIC -> ALGEBRAIC_HEAL;
            case GEOMETRIC -> GEOMETRIC_HEAL;
            case GLITCH -> GLITCH_HEAL;
        };
    }

    /** Stat du multiplicateur de dégâts pour un élément. */
    public static StatType damageMultiplier(Element element) {
        return switch (element) {
            case FIRE -> FIRE_DAMAGE;
            case WATER -> WATER_DAMAGE;
            case NATURE -> NATURE_DAMAGE;
            case NEUTRAL -> NEUTRAL_DAMAGE;
            case SPIRIT -> SPIRIT_DAMAGE;
            case ALGEBRAIC -> ALGEBRAIC_DAMAGE;
            case GEOMETRIC -> GEOMETRIC_DAMAGE;
            case GLITCH -> GLITCH_DAMAGE;
        };
    }

    /** Les 6 stats de personnage, dans l'ordre d'affichage (FOR, INT, ESP, DEF, RES, CHA). */
    public static java.util.List<StatType> characterStats() {
        return java.util.List.of(STRENGTH, INTELLIGENCE, MIND, DEFENSE, RESISTANCE, LUCK);
    }

    /** Abréviation affichée d'une stat de personnage (FOR, INT…), ou le nom de l'enum sinon. */
    public String abbreviation() {
        return switch (this) {
            case STRENGTH -> "FOR";
            case INTELLIGENCE -> "INT";
            case MIND -> "ESP";
            case DEFENSE -> "DEF";
            case RESISTANCE -> "RES";
            case LUCK -> "CHA";
            default -> name();
        };
    }

    /** Effet d'un point d'une stat de personnage, affiché au joueur (vide pour les autres stats). */
    public String effectText() {
        return switch (this) {
            case STRENGTH -> "+1 dégât des sorts feu, eau, plante, neutre";
            case INTELLIGENCE -> "+1 dégât algébrique/géométrique, +3 % de statut";
            case MIND -> "+1 dégât des sorts spirituels, +1 PV par soin";
            case DEFENSE -> "−1 dégât par coup subi";
            case RESISTANCE -> "−5 % de subir un statut ; à 5 : statuts −1 tour";
            case LUCK -> "+3 % de coup critique (×1,5) sur tes sorts";
            default -> "";
        };
    }

    /** Nom affiché d'une stat de personnage (Force, Intelligence…), ou le nom de l'enum sinon. */
    public String displayName() {
        return switch (this) {
            case STRENGTH -> "Force";
            case INTELLIGENCE -> "Intelligence";
            case MIND -> "Esprit";
            case DEFENSE -> "Défense";
            case RESISTANCE -> "Résistance";
            case LUCK -> "Chance";
            default -> name();
        };
    }

    /**
     * Les stats de dégâts de tous les éléments (neutre compris), pour les bonus « +X % de dégâts »
     * qui touchent tous les sorts (Ivresse, P'tit Sat, sets).
     */
    public static java.util.List<StatType> allDamageMultipliers() {
        return java.util.Arrays.stream(Element.values()).map(StatType::damageMultiplier).toList();
    }
}

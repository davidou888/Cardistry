package cardistry.items;

/**
 * Tirage unique de NullPointerException au début de chaque combat, du plus au moins favorable au
 * joueur (CLAUDE.md garde le plus favorable de deux tirages).
 */
public enum NullPointerRoll {
    /** L'adversaire devient null : mort sur le coup. */
    ENEMY_NULL,
    /** Rien ne se passe. */
    NOTHING,
    /** Une carte de la main devient « NO CARDS ». */
    DEAD_CARD;

    /** Chance que l'adversaire devienne null (jamais le boss : rien à la place). */
    public static final double ENEMY_NULL_CHANCE = 0.05;
    /** Chance qu'une carte devienne « NO CARDS ». */
    public static final double DEAD_CARD_CHANCE = 0.10;

    /** Résultat pour un tirage {@code roll} dans [0, 1) ; {@code boss} = dernier adversaire. */
    public static NullPointerRoll of(double roll, boolean boss) {
        if (roll < ENEMY_NULL_CHANCE) {
            return boss ? NOTHING : ENEMY_NULL;
        }
        if (roll < ENEMY_NULL_CHANCE + DEAD_CARD_CHANCE) {
            return DEAD_CARD;
        }
        return NOTHING;
    }

    /** {@code > 0} si {@code a} est plus favorable au joueur que {@code b}. */
    public static int compareFavor(NullPointerRoll a, NullPointerRoll b) {
        return Integer.compare(b.ordinal(), a.ordinal());
    }
}

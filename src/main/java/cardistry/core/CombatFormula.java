package cardistry.core;

import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Combatant;
import cardistry.model.Element;

/**
 * Toutes les formules de combat liées aux 6 stats (FOR, INT, ESP, DEF, RES, CHA), pour le joueur
 * comme pour les adversaires. Pures et testées ({@code CombatFormulaTest}) : le moteur leur passe les
 * valeurs, elles ne tirent rien au hasard.
 *
 * <ul>
 *   <li>Dégâts d'un sort ou d'un coup : {@code (base + stat offensive) × multiplicateurs × critique − DEF},
 *       au moins {@link #MIN_DAMAGE} si le coup a des dégâts (un sort à 0 reste à 0).</li>
 *   <li>Soin : {@code (base + ESP) × multiplicateurs}.</li>
 *   <li>Chance de statut : {@code chance + 3 % × INT du lanceur − 5 % × RES de la cible}, entre 0 et 100 %.</li>
 *   <li>Durée d'un statut subi : −1 tour à partir de {@link #RES_DURATION_THRESHOLD} RES (minimum 1).</li>
 * </ul>
 */
public final class CombatFormula {

    /** Multiplicateur d'un coup critique. */
    public static final double CRIT_MULTIPLIER = 1.5;
    /** Chance de critique par point de CHA. */
    public static final double CRIT_PER_LUCK = 0.03;
    /** Chance de statut en plus par point d'INT du lanceur. */
    public static final double STATUS_PER_INTELLIGENCE = 0.03;
    /** Chance de statut en moins par point de RES de la cible. */
    public static final double STATUS_PER_RESISTANCE = 0.05;
    /** RES à partir de laquelle les statuts subis durent un tour de moins. */
    public static final int RES_DURATION_THRESHOLD = 5;
    /** Dégâts minimum d'un coup qui a des dégâts, après la DEF. */
    public static final int MIN_DAMAGE = 1;
    /** DEF ignorée par un sort qui « ignore la DEF » (toute la DEF positive). */
    public static final double ALL_DEFENSE = Double.POSITIVE_INFINITY;

    private CombatFormula() {
    }

    /**
     * Stat offensive d'un sort de l'élément {@code element} : FOR (feu, eau, plante, neutre), INT
     * (algébrique, géométrique), ESP (spirituel). GLITCH : {@code null}, la meilleure des trois (voir
     * {@link #offense}).
     */
    public static StatType offensiveStat(Element element) {
        return switch (element) {
            case FIRE, WATER, NATURE, NEUTRAL -> StatType.STRENGTH;
            case ALGEBRAIC, GEOMETRIC -> StatType.INTELLIGENCE;
            case SPIRIT -> StatType.MIND;
            case GLITCH -> null;
        };
    }

    /** Valeur de la stat offensive du lanceur pour un sort de {@code element} (GLITCH : la meilleure). */
    public static double offense(Element element, Stats caster) {
        StatType stat = offensiveStat(element);
        if (stat != null) {
            return caster.get(stat);
        }
        return Math.max(caster.get(StatType.STRENGTH),
                Math.max(caster.get(StatType.INTELLIGENCE), caster.get(StatType.MIND)));
    }

    /**
     * DEF qui s'applique encore après {@code pierce} points ignorés. Une DEF négative (malus de
     * terrain) n'est jamais ignorée : elle ajoute des dégâts.
     */
    public static double effectiveDefense(double defense, double pierce) {
        return defense <= 0 ? defense : Math.max(0, defense - pierce);
    }

    /**
     * Dégâts d'un sort ou d'un coup, arrondis à l'entier.
     *
     * @param base       dégâts de base (0 ou moins : aucun dégât, pas de minimum)
     * @param offense    stat offensive du lanceur (FOR, INT ou ESP)
     * @param multiplier produit des multiplicateurs (élément, terrain, statuts, %, rejeu…)
     * @param critical   coup critique (×{@link #CRIT_MULTIPLIER})
     * @param defense    DEF de la cible
     * @param pierce     points de DEF ignorés ({@link #ALL_DEFENSE} : toute)
     */
    public static int damage(double base, double offense, double multiplier, boolean critical, double defense,
            double pierce) {
        if (base <= 0) {
            return 0;
        }
        double raw = (base + offense) * multiplier * (critical ? CRIT_MULTIPLIER : 1.0);
        return Math.max(MIN_DAMAGE, Combatant.round(raw - effectiveDefense(defense, pierce)));
    }

    /** Soin d'un nombre fixe de PV : {@code (base + ESP) × multiplicateurs} (0 si la base est nulle). */
    public static double heal(double base, double mind, double multiplier) {
        if (base <= 0) {
            return 0;
        }
        return Math.max(0, base + mind) * multiplier;
    }

    /** Chance de coup critique selon la CHA, entre 0 et 1. */
    public static double critChance(double luck) {
        return clamp(luck * CRIT_PER_LUCK);
    }

    /** Chance qu'un statut soit posé, entre 0 et 1 (la RES compte aussi contre les statuts « à coup sûr »). */
    public static double statusChance(double chance, double casterIntelligence, double targetResistance) {
        return clamp(chance + casterIntelligence * STATUS_PER_INTELLIGENCE
                - targetResistance * STATUS_PER_RESISTANCE);
    }

    /** Durée d'un statut subi selon la RES de la cible : −1 tour à partir de 5 RES, jamais sous 1. */
    public static int statusTurns(int turns, double targetResistance) {
        return targetResistance >= RES_DURATION_THRESHOLD ? Math.max(1, turns - 1) : turns;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}

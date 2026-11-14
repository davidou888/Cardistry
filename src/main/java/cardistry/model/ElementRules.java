package cardistry.model;

/**
 * Multiplicateurs liés aux éléments. Toutes les tables du jeu sont ici, et nulle part ailleurs.
 *
 * <p>Cycle : Feu bat Nature, Nature bat Eau, Eau bat Feu.
 *
 * <h2>Sortilège → dégâts infligés à l'adversaire</h2>
 * <pre>
 *   Sort \ Adversaire |  FEU  |  EAU  | NATURE
 *  -------------------+-------+-------+--------
 *   FEU               |  ×1   | ×0.5  |  ×1.5
 *   EAU               | ×1.5  |  ×1   |  ×0.5
 *   NATURE            | ×0.5  | ×1.5  |  ×1
 * </pre>
 * ×1.5 si le sort bat l'adversaire, ×0.5 si l'adversaire bat le sort, ×1 sinon (même élément).
 *
 * <h2>Soin → PV rendus, selon le terrain</h2>
 * <pre>
 *   Soin \ Terrain |  FEU  |  EAU  | NATURE
 *  ----------------+-------+-------+--------
 *   FEU            | ×1.5  | ×0.75 |  ×1
 *   EAU            |  ×1   | ×1.5  | ×0.75
 *   NATURE         | ×0.75 |  ×1   |  ×1.5
 * </pre>
 * ×1.5 dans un terrain du même élément, ×0.75 si le terrain bat le soin, ×1 sinon.
 *
 * <h2>Terrain → attaque de l'adversaire</h2>
 * Le terrain multiplie l'<b>attaque</b> de l'adversaire (pas les dégâts qu'il reçoit).
 * <pre>
 *   Terrain \ Adversaire |  FEU  |  EAU  | NATURE
 *  ----------------------+-------+-------+--------
 *   FEU                  | ×1.25 |  ×1   | ×0.75
 *   EAU                  | ×0.75 | ×1.25 |  ×1
 *   NATURE               |  ×1   | ×0.75 | ×1.25
 * </pre>
 * ×1.25 si l'adversaire est sur un terrain de son élément (renforcé), ×0.75 si le terrain bat
 * son élément (affaibli), ×1 sinon.
 */
public final class ElementRules {

    public static final double ADVANTAGE = 1.5;
    public static final double DISADVANTAGE = 0.5;

    public static final double HEAL_SAME_TERRAIN = 1.5;
    public static final double HEAL_HOSTILE_TERRAIN = 0.75;

    public static final double TERRAIN_STRENGTHENS = 1.25;
    public static final double TERRAIN_WEAKENS = 0.75;

    public static final double NEUTRAL = 1.0;

    private ElementRules() {
    }

    /** Multiplicateur des dégâts d'un sortilège contre un adversaire. */
    public static double spellMultiplier(Element spell, Element enemy) {
        if (spell.beats(enemy)) {
            return ADVANTAGE;
        } else if (enemy.beats(spell)) {
            return DISADVANTAGE;
        }
        return NEUTRAL;
    }

    /** Multiplicateur d'un soin selon le terrain. */
    public static double healMultiplier(Element heal, Element terrain) {
        if (heal == terrain) {
            return HEAL_SAME_TERRAIN;
        } else if (terrain.beats(heal)) {
            return HEAL_HOSTILE_TERRAIN;
        }
        return NEUTRAL;
    }

    /** Multiplicateur de l'attaque d'un adversaire selon le terrain. */
    public static double terrainAttackMultiplier(Element terrain, Element enemy) {
        if (enemy == Element.NEUTRAL) {
            return NEUTRAL; // les terrains n'ont aucun effet sur un adversaire neutre
        }
        if (terrain == enemy) {
            return TERRAIN_STRENGTHENS;
        } else if (terrain.beats(enemy)) {
            return TERRAIN_WEAKENS;
        }
        return NEUTRAL;
    }
}

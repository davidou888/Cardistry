package cardistry.model;

/** Multiplicateurs liés aux éléments. Toutes les tables du jeu sont ici. */
public final class ElementRules {

    private ElementRules() {
    }

    /** Multiplicateur des dégâts d'un sortilège contre un adversaire. */
    public static double spellMultiplier(Element spell, Element enemy) {
        if (spell.beats(enemy)) {
            return 1.5;
        } else if (spell == enemy) {
            return 0.5;
        }
        return 1.0;
    }

    /** Multiplicateur d'un soin selon le terrain. */
    public static double healMultiplier(Element heal, Element terrain) {
        if (terrain.beats(heal)) {
            return 0.75;
        } else if (heal == terrain) {
            return 1.5;
        }
        return 1.0;
    }

    /** Multiplicateur de l'attaque d'un adversaire selon le terrain. */
    public static double terrainAttackMultiplier(Element terrain, Element enemy) {
        if (terrain.beats(enemy)) {
            return 0.75;
        } else if (terrain == enemy) {
            return 1.25;
        }
        return 1.0;
    }
}

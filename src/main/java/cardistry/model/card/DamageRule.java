package cardistry.model.card;

import cardistry.core.EffectContext;

/**
 * Dégâts de base d'un sort calculés au moment où il est joué, <b>avant</b> le coup (Matrix, Moindre
 * carré, Pyromane…). Les multiplicateurs s'appliquent ensuite, sauf pour un sort non multiplié.
 */
@FunctionalInterface
public interface DamageRule {

    double damage(EffectContext ctx);
}

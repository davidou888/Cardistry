package cardistry.model.enemy;

import cardistry.model.Enemy;

/**
 * Choisit la prochaine attaque d'un adversaire (son intent), à partir de son propre état : nombre
 * d'attaques déjà faites, utilisations de chaque attaque, PV. Déterministe : l'aléatoire d'une
 * attaque (Gamble, coup raté) est tiré quand elle est exécutée.
 */
@FunctionalInterface
public interface EnemyBehavior {

    EnemyMove next(Enemy self);

    /** Toujours la même attaque. */
    static EnemyBehavior always(EnemyMove move) {
        return self -> move;
    }

    /** {@code special} toutes les {@code period} attaques (la période-ième, la 2×période-ième…), sinon {@code normal}. */
    static EnemyBehavior every(int period, EnemyMove special, EnemyMove normal) {
        return self -> (self.actions() + 1) % period == 0 ? special : normal;
    }
}

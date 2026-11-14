package cardistry.model.enemy;

import java.util.List;

/**
 * Une attaque d'adversaire : nom, texte affiché (intent, journal) et effets, dans l'ordre. Sans
 * effet, l'adversaire ne fait rien (« Clarence te regarde. »).
 *
 * @param id identifiant stable (compteur d'utilisations, tests)
 */
public record EnemyMove(String id, String name, String description, List<MoveEffect> effects) {

    public EnemyMove {
        effects = List.copyOf(effects);
    }

    public static EnemyMove of(String id, String name, String description, MoveEffect... effects) {
        return new EnemyMove(id, name, description, List.of(effects));
    }

    /** Attaque simple de {@code damage} (adversaires de test, attaque de base). */
    public static EnemyMove basicAttack(double damage) {
        return of("attaque", "Attaque", "Attaque", MoveEffect.Hit.of(damage));
    }

    /** {@code true} si l'attaque inflige des dégâts à coup sûr ou peut en infliger. */
    public boolean isAttack() {
        return effects.stream().anyMatch(MoveEffect::isAttack);
    }

    /** Même attaque avec d'autres effets (ex. dégâts qui montent). */
    public EnemyMove withEffects(MoveEffect... newEffects) {
        return new EnemyMove(id, name, description, List.of(newEffects));
    }
}

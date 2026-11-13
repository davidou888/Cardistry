package cardistry.core.event;

/**
 * Points d'accroche pour les trinkets et objets : on n'implémente que ce dont on a besoin.
 *
 * <p>Les hooks sont appelés <b>après</b> l'événement, de façon synchrone. Ils réagissent
 * (ajouter un modificateur de stat, déclencher un effet…) ; pour modifier une valeur comme
 * « +25 % de dégâts feu », passer par un {@link cardistry.core.stats.StatModifier}.
 *
 * <p>À ajouter avec les fonctionnalités correspondantes : {@code onShopEnter}, {@code onItemBought}…
 */
public interface GameHooks {

    default void onGameStart(GameEvent.GameStarted event) {
    }

    default void onTurnStart(GameEvent.TurnStarted event) {
    }

    default void onCardPlayed(GameEvent.CardPlayed event) {
    }

    default void onCardDrawn(GameEvent.CardDrawn event) {
    }

    default void onDamageDealt(GameEvent.DamageDealt event) {
    }

    default void onHeal(GameEvent.Healed event) {
    }

    default void onTerrainChanged(GameEvent.TerrainChanged event) {
    }

    default void onEnemyAttack(GameEvent.EnemyAttacked event) {
    }

    default void onEnemyKilled(GameEvent.EnemyKilled event) {
    }

    default void onCombatStart(GameEvent.CombatStarted event) {
    }

    default void onHandDealt(GameEvent.HandDealt event) {
    }

    default void onGameWon(GameEvent.GameWon event) {
    }

    default void onGameLost(GameEvent.GameLost event) {
    }
}

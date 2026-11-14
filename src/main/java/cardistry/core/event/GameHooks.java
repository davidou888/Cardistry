package cardistry.core.event;

import cardistry.core.EffectContext;

/**
 * Points d'accroche pour les trinkets : on n'implémente que ce dont on a besoin.
 *
 * <p>Les hooks sont appelés <b>après</b> l'événement, de façon synchrone, avec un
 * {@link EffectContext} pour agir (dégâts, soins, pioche…). Les événements qu'ils provoquent sont
 * livrés après l'événement en cours (voir {@link EventBus}). Pour modifier une valeur comme
 * « +25 % de dégâts feu », passer plutôt par un {@link cardistry.core.stats.StatModifier}.
 *
 */
public interface GameHooks {

    default void onGameStart(GameEvent.GameStarted event, EffectContext ctx) {
    }

    default void onCombatStart(GameEvent.CombatStarted event, EffectContext ctx) {
    }

    default void onHandDealt(GameEvent.HandDealt event, EffectContext ctx) {
    }

    default void onTurnStart(GameEvent.TurnStarted event, EffectContext ctx) {
    }

    /** Tour où le joueur joue vraiment (pas les tours sautés). */
    default void onPlayerTurnStart(GameEvent.PlayerTurnStarted event, EffectContext ctx) {
    }

    default void onCardPlayed(GameEvent.CardPlayed event, EffectContext ctx) {
    }

    default void onCardDrawn(GameEvent.CardDrawn event, EffectContext ctx) {
    }

    default void onCardReplaced(GameEvent.CardReplaced event, EffectContext ctx) {
    }

    default void onItemUsed(GameEvent.ItemUsed event, EffectContext ctx) {
    }

    default void onDamageDealt(GameEvent.DamageDealt event, EffectContext ctx) {
    }

    /** Un sortilège de la main a touché l'adversaire (après {@link #onDamageDealt}). */
    default void onSpellHit(GameEvent.SpellHit event, EffectContext ctx) {
    }

    default void onHeal(GameEvent.Healed event, EffectContext ctx) {
    }

    default void onTerrainChanged(GameEvent.TerrainChanged event, EffectContext ctx) {
    }

    default void onEnemyAttack(GameEvent.EnemyAttacked event, EffectContext ctx) {
    }

    default void onEnemyKilled(GameEvent.EnemyKilled event, EffectContext ctx) {
    }

    default void onDrunkennessChanged(GameEvent.DrunkennessChanged event, EffectContext ctx) {
    }

    default void onCurseGained(GameEvent.CurseGained event, EffectContext ctx) {
    }

    default void onTurnSkipped(GameEvent.PlayerTurnSkipped event, EffectContext ctx) {
    }

    default void onEnemyTurnSkipped(GameEvent.EnemyTurnSkipped event, EffectContext ctx) {
    }

    default void onAttackCancelled(GameEvent.AttackCancelled event, EffectContext ctx) {
    }

    default void onCigaretteSmoked(GameEvent.CigaretteSmoked event, EffectContext ctx) {
    }

    default void onCigarettesChanged(GameEvent.CigarettesChanged event, EffectContext ctx) {
    }

    default void onRewardOffered(GameEvent.RewardOffered event, EffectContext ctx) {
    }

    default void onRewardTaken(GameEvent.RewardTaken event, EffectContext ctx) {
    }

    default void onShopEnter(GameEvent.ShopEntered event, EffectContext ctx) {
    }

    default void onItemBought(GameEvent.ItemBought event, EffectContext ctx) {
    }

    default void onGameWon(GameEvent.GameWon event, EffectContext ctx) {
    }

    default void onGameLost(GameEvent.GameLost event, EffectContext ctx) {
    }
}

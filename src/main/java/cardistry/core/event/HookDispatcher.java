package cardistry.core.event;

import cardistry.core.EffectContext;

/** Traduit un {@link GameEvent} en appel de la méthode correspondante de {@link GameHooks}. */
public final class HookDispatcher {

    private HookDispatcher() {
    }

    public static void dispatch(GameHooks hook, GameEvent event, EffectContext ctx) {
        switch (event) {
            case GameEvent.GameStarted e -> hook.onGameStart(e, ctx);
            case GameEvent.CombatStarted e -> hook.onCombatStart(e, ctx);
            case GameEvent.HandDealt e -> hook.onHandDealt(e, ctx);
            case GameEvent.TurnStarted e -> hook.onTurnStart(e, ctx);
            case GameEvent.CardPlayed e -> hook.onCardPlayed(e, ctx);
            case GameEvent.CardDrawn e -> hook.onCardDrawn(e, ctx);
            case GameEvent.ItemUsed e -> hook.onItemUsed(e, ctx);
            case GameEvent.DamageDealt e -> hook.onDamageDealt(e, ctx);
            case GameEvent.Healed e -> hook.onHeal(e, ctx);
            case GameEvent.TerrainChanged e -> hook.onTerrainChanged(e, ctx);
            case GameEvent.EnemyAttacked e -> hook.onEnemyAttack(e, ctx);
            case GameEvent.EnemyKilled e -> hook.onEnemyKilled(e, ctx);
            case GameEvent.GameWon e -> hook.onGameWon(e, ctx);
            case GameEvent.GameLost e -> hook.onGameLost(e, ctx);
            case GameEvent.ActionRejected e -> {
                // Concerne seulement l'UI.
            }
        }
    }
}

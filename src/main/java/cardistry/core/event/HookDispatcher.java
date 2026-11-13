package cardistry.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Abonné au bus : traduit chaque {@link GameEvent} en appel de {@link GameHooks}. */
public final class HookDispatcher implements Consumer<GameEvent> {

    private final List<GameHooks> hooks = new ArrayList<>();

    public void register(GameHooks hook) {
        hooks.add(hook);
    }

    public void unregister(GameHooks hook) {
        hooks.remove(hook);
    }

    @Override
    public void accept(GameEvent event) {
        for (GameHooks hook : List.copyOf(hooks)) {
            dispatch(hook, event);
        }
    }

    private static void dispatch(GameHooks hook, GameEvent event) {
        switch (event) {
            case GameEvent.GameStarted e -> hook.onGameStart(e);
            case GameEvent.TurnStarted e -> hook.onTurnStart(e);
            case GameEvent.CardPlayed e -> hook.onCardPlayed(e);
            case GameEvent.CardDrawn e -> hook.onCardDrawn(e);
            case GameEvent.DamageDealt e -> hook.onDamageDealt(e);
            case GameEvent.Healed e -> hook.onHeal(e);
            case GameEvent.TerrainChanged e -> hook.onTerrainChanged(e);
            case GameEvent.EnemyAttacked e -> hook.onEnemyAttack(e);
            case GameEvent.EnemyKilled e -> hook.onEnemyKilled(e);
            case GameEvent.CombatStarted e -> hook.onCombatStart(e);
            case GameEvent.HandDealt e -> hook.onHandDealt(e);
            case GameEvent.GameWon e -> hook.onGameWon(e);
            case GameEvent.GameLost e -> hook.onGameLost(e);
            case GameEvent.ActionRejected e -> {
                // Concerne seulement l'UI.
            }
        }
    }
}

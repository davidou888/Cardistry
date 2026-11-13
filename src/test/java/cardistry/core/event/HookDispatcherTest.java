package cardistry.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.core.EffectContext;
import cardistry.items.ItemRegistry;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HookDispatcherTest {

    /** Enregistre le nom de chaque hook appelé. */
    private static final class Recorder implements GameHooks {
        final List<String> calls = new ArrayList<>();

        @Override public void onGameStart(GameEvent.GameStarted e, EffectContext ctx) { calls.add("onGameStart"); }
        @Override public void onTurnStart(GameEvent.TurnStarted e, EffectContext ctx) { calls.add("onTurnStart"); }
        @Override public void onCardPlayed(GameEvent.CardPlayed e, EffectContext ctx) { calls.add("onCardPlayed"); }
        @Override public void onCardDrawn(GameEvent.CardDrawn e, EffectContext ctx) { calls.add("onCardDrawn"); }
        @Override public void onItemUsed(GameEvent.ItemUsed e, EffectContext ctx) { calls.add("onItemUsed"); }
        @Override public void onDamageDealt(GameEvent.DamageDealt e, EffectContext ctx) { calls.add("onDamageDealt"); }
        @Override public void onHeal(GameEvent.Healed e, EffectContext ctx) { calls.add("onHeal"); }
        @Override public void onTerrainChanged(GameEvent.TerrainChanged e, EffectContext ctx) { calls.add("onTerrainChanged"); }
        @Override public void onEnemyAttack(GameEvent.EnemyAttacked e, EffectContext ctx) { calls.add("onEnemyAttack"); }
        @Override public void onEnemyKilled(GameEvent.EnemyKilled e, EffectContext ctx) { calls.add("onEnemyKilled"); }
        @Override public void onCombatStart(GameEvent.CombatStarted e, EffectContext ctx) { calls.add("onCombatStart"); }
        @Override public void onHandDealt(GameEvent.HandDealt e, EffectContext ctx) { calls.add("onHandDealt"); }
        @Override public void onGameWon(GameEvent.GameWon e, EffectContext ctx) { calls.add("onGameWon"); }
        @Override public void onGameLost(GameEvent.GameLost e, EffectContext ctx) { calls.add("onGameLost"); }
    }

    @Test
    void chaqueEvenementAppelleLeBonHook() {
        Enemy enemy = new Enemy("Hydre", Element.FIRE, 20, 5);
        Player player = new Player(100);
        SpellCard card = new SpellCard("Boule de feu", Element.FIRE, 10);
        TerrainCard terrain = new TerrainCard("EPFL", Element.FIRE);
        Recorder recorder = new Recorder();

        List.of(
                new GameEvent.GameStarted(1),
                new GameEvent.CombatStarted(enemy, 1, 3),
                new GameEvent.HandDealt(List.of(card)),
                new GameEvent.TurnStarted(1),
                new GameEvent.CardPlayed(card),
                new GameEvent.CardDrawn(card),
                new GameEvent.ItemUsed(ItemRegistry.items().get(0)),
                new GameEvent.DamageDealt(enemy, 10, 1),
                new GameEvent.Healed(player, 10, 1),
                new GameEvent.TerrainChanged(terrain, terrain),
                new GameEvent.EnemyAttacked(enemy),
                new GameEvent.EnemyKilled(enemy),
                new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.HAND_FULL),
                new GameEvent.GameWon(),
                new GameEvent.GameLost()
        ).forEach(event -> HookDispatcher.dispatch(recorder, event, null));

        assertEquals(List.of(
                "onGameStart", "onCombatStart", "onHandDealt", "onTurnStart", "onCardPlayed", "onCardDrawn",
                "onItemUsed", "onDamageDealt", "onHeal", "onTerrainChanged", "onEnemyAttack", "onEnemyKilled",
                "onGameWon", "onGameLost"), recorder.calls);
    }
}

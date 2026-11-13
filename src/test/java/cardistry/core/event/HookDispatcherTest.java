package cardistry.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        @Override public void onGameStart(GameEvent.GameStarted e) { calls.add("onGameStart"); }
        @Override public void onTurnStart(GameEvent.TurnStarted e) { calls.add("onTurnStart"); }
        @Override public void onCardPlayed(GameEvent.CardPlayed e) { calls.add("onCardPlayed"); }
        @Override public void onCardDrawn(GameEvent.CardDrawn e) { calls.add("onCardDrawn"); }
        @Override public void onDamageDealt(GameEvent.DamageDealt e) { calls.add("onDamageDealt"); }
        @Override public void onHeal(GameEvent.Healed e) { calls.add("onHeal"); }
        @Override public void onTerrainChanged(GameEvent.TerrainChanged e) { calls.add("onTerrainChanged"); }
        @Override public void onEnemyAttack(GameEvent.EnemyAttacked e) { calls.add("onEnemyAttack"); }
        @Override public void onEnemyKilled(GameEvent.EnemyKilled e) { calls.add("onEnemyKilled"); }
        @Override public void onCombatStart(GameEvent.CombatStarted e) { calls.add("onCombatStart"); }
        @Override public void onHandDealt(GameEvent.HandDealt e) { calls.add("onHandDealt"); }
        @Override public void onGameWon(GameEvent.GameWon e) { calls.add("onGameWon"); }
        @Override public void onGameLost(GameEvent.GameLost e) { calls.add("onGameLost"); }
    }

    @Test
    void chaqueEvenementAppelleLeBonHook() {
        Enemy enemy = new Enemy("Hydre", Element.FIRE, 20, 5);
        Player player = new Player(100);
        SpellCard card = new SpellCard("Boule de feu", Element.FIRE, 10);
        TerrainCard terrain = new TerrainCard("EPFL", Element.FIRE);

        HookDispatcher dispatcher = new HookDispatcher();
        Recorder recorder = new Recorder();
        dispatcher.register(recorder);

        List.of(
                new GameEvent.GameStarted(1),
                new GameEvent.TurnStarted(1),
                new GameEvent.CardPlayed(card),
                new GameEvent.CardDrawn(card),
                new GameEvent.DamageDealt(enemy, 10, 1),
                new GameEvent.Healed(player, 10, 1),
                new GameEvent.TerrainChanged(terrain, terrain),
                new GameEvent.EnemyAttacked(enemy),
                new GameEvent.EnemyKilled(enemy),
                new GameEvent.CombatStarted(enemy, 1, 3),
                new GameEvent.HandDealt(List.of(card)),
                new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.HAND_FULL),
                new GameEvent.GameWon(),
                new GameEvent.GameLost()
        ).forEach(dispatcher);

        assertEquals(List.of(
                "onGameStart", "onTurnStart", "onCardPlayed", "onCardDrawn", "onDamageDealt", "onHeal",
                "onTerrainChanged", "onEnemyAttack", "onEnemyKilled", "onCombatStart", "onHandDealt", "onGameWon",
                "onGameLost"), recorder.calls);
    }

    @Test
    void unHookDesinscritNEstPlusAppele() {
        HookDispatcher dispatcher = new HookDispatcher();
        Recorder recorder = new Recorder();
        dispatcher.register(recorder);
        dispatcher.unregister(recorder);

        dispatcher.accept(new GameEvent.TurnStarted(1));

        assertEquals(List.of(), recorder.calls);
    }

    @Test
    void branchéSurLeBus() {
        EventBus bus = new EventBus();
        HookDispatcher dispatcher = new HookDispatcher();
        Recorder recorder = new Recorder();
        dispatcher.register(recorder);
        bus.subscribe(dispatcher);

        bus.publish(new GameEvent.TurnStarted(1));

        assertEquals(List.of("onTurnStart"), recorder.calls);
    }
}

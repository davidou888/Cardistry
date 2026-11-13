package cardistry.core;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.stats.StatType;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.Optional;

/**
 * Règles du combat. N'affiche rien : chaque changement d'état est publié sur le bus.
 *
 * <p>Déroulement d'un tour : {@link #startTurn()}, puis {@link #submit(Action)} jusqu'à ce
 * qu'une action soit acceptée ; l'adversaire réagit dans la foulée.
 */
public final class CombatEngine {

    private final GameState state;
    private final CardPool pool;
    private final Rng rng;
    private final EventBus bus;

    public CombatEngine(GameState state, CardPool pool, Rng rng, EventBus bus) {
        this.state = state;
        this.pool = pool;
        this.rng = rng;
        this.bus = bus;
    }

    public GameState state() {
        return state;
    }

    public void start() {
        bus.publish(new GameEvent.GameStarted(rng.seed()));
    }

    public void startTurn() {
        state.nextTurn();
        bus.publish(new GameEvent.TurnStarted(state.turn()));
    }

    public boolean isOver() {
        return state.isWon() || state.isLost();
    }

    /**
     * Joue l'action du joueur puis la réaction de l'adversaire.
     *
     * @return {@code false} si l'action est refusée (un {@link GameEvent.ActionRejected} est publié
     *     et rien d'autre ne change)
     */
    public boolean submit(Action action) {
        if (isOver()) {
            throw new IllegalStateException("La partie est terminée");
        }
        Optional<Reason> rejection = validate(action);
        if (rejection.isPresent()) {
            bus.publish(new GameEvent.ActionRejected(rejection.get()));
            return false;
        }
        switch (action) {
            case Action.DrawCard draw -> drawCard();
            case Action.PlayCard play -> playCard(play.slot());
        }
        enemyPhase();
        return true;
    }

    private Optional<Reason> validate(Action action) {
        return switch (action) {
            case Action.DrawCard draw -> state.hand().size() >= state.handSize()
                    ? Optional.of(Reason.HAND_FULL)
                    : Optional.empty();
            case Action.PlayCard play -> {
                if (play.slot() < 0 || play.slot() >= state.handSize()) {
                    yield Optional.of(Reason.INVALID_CHOICE);
                }
                if (play.slot() >= state.hand().size()) {
                    yield Optional.of(Reason.EMPTY_SLOT);
                }
                yield Optional.empty();
            }
        };
    }

    private void drawCard() {
        Card card = pool.draw(rng);
        state.addToHand(card);
        bus.publish(new GameEvent.CardDrawn(card));
    }

    private void playCard(int slot) {
        Card card = state.removeFromHand(slot);
        bus.publish(new GameEvent.CardPlayed(card));
        switch (card) {
            case SpellCard spell -> {
                Enemy enemy = state.currentEnemy();
                double mult = ElementRules.spellMultiplier(spell.element(), enemy.element())
                        * state.playerStats().get(StatType.damageMultiplier(spell.element()));
                double amount = spell.damage() * mult;
                enemy.takeDamage(amount);
                bus.publish(new GameEvent.DamageDealt(enemy, amount, mult));
            }
            case HealCard heal -> {
                Player player = state.player();
                double mult = ElementRules.healMultiplier(heal.element(), state.terrain().element())
                        * state.playerStats().get(StatType.HEAL_EFFICIENCY);
                double amount = heal.amount() * mult;
                player.heal(amount);
                bus.publish(new GameEvent.Healed(player, amount, mult));
            }
            case TerrainCard terrain -> {
                TerrainCard previous = state.terrain();
                state.setTerrain(terrain);
                bus.publish(new GameEvent.TerrainChanged(previous, terrain));
            }
        }
    }

    /**
     * Si l'adversaire est mort, le suivant arrive mais n'attaque pas ce tour-ci.
     * Sinon, l'adversaire attaque.
     */
    private void enemyPhase() {
        Enemy enemy = state.currentEnemy();
        if (enemy.isAlive()) {
            enemyAttack();
            return;
        }
        bus.publish(new GameEvent.EnemyKilled(enemy));
        if (state.hasNextEnemy()) {
            state.advanceEnemy();
            bus.publish(new GameEvent.EnemyArrived(state.currentEnemy()));
        } else {
            bus.publish(new GameEvent.GameWon());
        }
    }

    private void enemyAttack() {
        Enemy enemy = state.currentEnemy();
        Player player = state.player();
        double mult = state.enemyAttackMultiplier();
        double amount = enemy.attack() * mult;
        bus.publish(new GameEvent.EnemyAttacked(enemy));
        player.takeDamage(amount);
        bus.publish(new GameEvent.DamageDealt(player, amount, mult));
        if (!player.isAlive()) {
            bus.publish(new GameEvent.GameLost());
        }
    }
}

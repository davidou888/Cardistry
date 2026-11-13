package cardistry.core;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.event.HookDispatcher;
import cardistry.items.Item;
import cardistry.items.Trinket;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.List;
import java.util.Optional;

/**
 * Règles du combat. N'affiche rien : chaque changement d'état est publié sur le bus.
 *
 * <p>Une partie est une suite de combats, un par adversaire ; chaque combat commence avec une
 * nouvelle main de {@link GameState#STARTING_HAND} cartes. Déroulement d'un tour :
 * {@link #startTurn()}, puis {@link #submit(Action)} jusqu'à ce que le tour soit terminé
 * ({@link ActionResult#TURN_ENDED}). Jouer une carte ou piocher termine le tour et l'adversaire
 * réagit ; utiliser un objet est gratuit.
 *
 * <p>Les trinkets équipés reçoivent chaque événement (voir {@link cardistry.core.event.GameHooks}).
 */
public final class CombatEngine {

    private final GameState state;
    private final CardPool pool;
    private final Rng rng;
    private final EventBus bus;
    private final EffectContext effects = new Effects();

    public CombatEngine(GameState state, CardPool pool, Rng rng, EventBus bus) {
        this.state = state;
        this.pool = pool;
        this.rng = rng;
        this.bus = bus;
        bus.subscribe(this::dispatchToTrinkets);
    }

    public GameState state() {
        return state;
    }

    /** Lance la partie et son premier combat. */
    public void start() {
        bus.publish(new GameEvent.GameStarted(rng.seed()));
        beginCombat();
    }

    public void startTurn() {
        state.nextTurn();
        bus.publish(new GameEvent.TurnStarted(state.turn()));
    }

    public boolean isOver() {
        return state.isWon() || state.isLost();
    }

    /**
     * Joue l'action du joueur, puis, si elle termine le tour, la réaction de l'adversaire.
     *
     * @return {@link ActionResult#REJECTED} si l'action est refusée (un {@link GameEvent.ActionRejected}
     *     est publié et rien d'autre ne change)
     */
    public ActionResult submit(Action action) {
        if (isOver()) {
            throw new IllegalStateException("La partie est terminée");
        }
        Optional<Reason> rejection = validate(action);
        if (rejection.isPresent()) {
            bus.publish(new GameEvent.ActionRejected(rejection.get()));
            return ActionResult.REJECTED;
        }
        boolean endsTurn = switch (action) {
            case Action.DrawCard draw -> {
                drawCard();
                yield true;
            }
            case Action.PlayCard play -> {
                playCard(play.slot());
                yield true;
            }
            case Action.UseItem use -> {
                useItem(use.slot());
                yield false;
            }
        };
        if (resolveEnemyDeath()) {
            return ActionResult.TURN_ENDED;
        }
        if (!endsTurn) {
            return ActionResult.FREE_ACTION;
        }
        enemyAttack();
        return ActionResult.TURN_ENDED;
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
            case Action.UseItem use -> use.slot() < 0 || use.slot() >= state.items().size()
                    ? Optional.of(Reason.NO_ITEM)
                    : Optional.empty();
        };
    }

    // --- Combats ---

    /** Nouvelle main, compteur de tours du combat remis à zéro. */
    private void beginCombat() {
        state.resetCombatTurn();
        state.clearHand();
        for (int i = 0; i < GameState.STARTING_HAND; i++) {
            state.addToHand(pool.draw(rng));
        }
        bus.publish(new GameEvent.CombatStarted(state.currentEnemy(), state.combatNumber(), state.enemies().size()));
        bus.publish(new GameEvent.HandDealt(state.hand()));
    }

    /**
     * Si l'adversaire est mort, passe au combat suivant (le nouvel adversaire n'attaque pas ce
     * tour-ci) ou termine la partie.
     *
     * @return {@code true} si l'adversaire était mort
     */
    private boolean resolveEnemyDeath() {
        Enemy enemy = state.currentEnemy();
        if (enemy.isAlive()) {
            return false;
        }
        bus.publish(new GameEvent.EnemyKilled(enemy));
        if (state.hasNextEnemy()) {
            state.advanceEnemy();
            beginCombat();
        } else {
            bus.publish(new GameEvent.GameWon());
        }
        return true;
    }

    // --- Actions du joueur ---

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
                double mult = state.spellMultiplier(spell);
                damageEnemy(spell.damage() * mult, mult);
            }
            case HealCard heal -> {
                double mult = state.healMultiplier(heal);
                healPlayer(heal.amount() * mult, mult);
            }
            case TerrainCard terrain -> {
                TerrainCard previous = state.terrain();
                state.setTerrain(terrain);
                bus.publish(new GameEvent.TerrainChanged(previous, terrain));
            }
        }
    }

    private void useItem(int slot) {
        Item item = state.removeItem(slot);
        bus.publish(new GameEvent.ItemUsed(item));
        item.use(effects);
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

    // --- Primitives partagées par les cartes et les effets ---

    private void damageEnemy(double amount, double mult) {
        Enemy enemy = state.currentEnemy();
        enemy.takeDamage(amount);
        bus.publish(new GameEvent.DamageDealt(enemy, amount, mult));
    }

    /** Soigne sans dépasser les PV max ; l'événement porte les PV réellement rendus. */
    private void healPlayer(double amount, double mult) {
        Player player = state.player();
        double missing = Math.max(0, state.maxHp() - player.hp());
        double healed = Math.min(amount, missing);
        player.heal(healed);
        bus.publish(new GameEvent.Healed(player, healed, mult));
    }

    private void dispatchToTrinkets(GameEvent event) {
        List<Trinket> equipped = List.copyOf(state.trinkets());
        for (Trinket trinket : equipped) {
            HookDispatcher.dispatch(trinket.hooks(), event, effects);
        }
    }

    /** Actions offertes aux objets et trinkets. */
    private final class Effects implements EffectContext {

        @Override
        public GameState state() {
            return state;
        }

        @Override
        public Rng rng() {
            return rng;
        }

        @Override
        public void damageEnemy(double amount) {
            CombatEngine.this.damageEnemy(amount, 1.0);
        }

        @Override
        public void healPlayer(double amount) {
            CombatEngine.this.healPlayer(amount, 1.0);
        }

        @Override
        public void drawCards(int count) {
            for (int i = 0; i < count && state.hand().size() < state.handSize(); i++) {
                drawCard();
            }
        }
    }

    // --- Inventaire (récompenses, tests) ---

    /** Équipe un trinket (ses modificateurs de stats s'appliquent immédiatement). */
    public void equip(Trinket trinket) {
        state.equipTrinket(trinket);
    }

    /** Ajoute un objet à l'inventaire. */
    public void give(Item item) {
        state.addItem(item);
    }
}

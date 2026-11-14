package cardistry.run;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.PlayerDecisions;
import cardistry.core.RewardChoice;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.items.Collectible;
import cardistry.items.ItemRegistry;
import cardistry.items.LootTable;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Une partie : le moteur de combat, plus ce qui dépasse un moteur seul.
 * <ul>
 *   <li>les <b>codes</b> : secrets équipés dès le départ, pour cette partie seulement ;</li>
 *   <li>les <b>secrets débloqués</b>, qui entrent dans le pool des récompenses ;</li>
 *   <li>l'<b>annulation de tour</b> (trinket Ctrl+Z) : chaque appel au moteur et chaque décision
 *       du joueur sont enregistrés ; annuler = reconstruire un moteur neuf avec la même seed et
 *       rejouer l'historique jusqu'au début du tour précédent. Tout l'aléatoire passant par le
 *       {@link Rng} seedé, le rejeu redonne exactement le même état.</li>
 * </ul>
 * Les événements du moteur sont relayés sur le bus {@code front} (interface, déblocages), sauf
 * pendant un rejeu.
 */
public final class Run {

    /** Un appel au moteur (ou une réponse du joueur pendant cet appel), pour le rejeu. */
    private sealed interface Step {
    }

    private record StartTurn() implements Step {
    }

    private record Submit(Action action) implements Step {
    }

    private record Reward(RewardChoice choice) implements Step {
    }

    private record Decision(int index) implements Step {
    }

    /** Début d'un tour où le joueur agit : {@code historySize} étapes le précèdent. */
    private record TurnMark(int historySize, int combat, int turn) {
    }

    private final long seed;
    private final Set<String> unlockedIds;
    private final EventBus front;
    private final Supplier<GameState> newState;
    private final Supplier<CardPool> newPool;
    private final List<Secret> codes = new ArrayList<>();
    private final List<Step> history = new ArrayList<>();
    private final List<TurnMark> marks = new ArrayList<>();
    private PlayerDecisions decisions = PlayerDecisions.FIRST;
    private CombatEngine engine;
    private int undoUsedInCombat = 0;

    /** Partie standard. */
    public Run(long seed, Set<Secret> unlocked, EventBus front) {
        this(seed, unlocked, front, GameState::newGame, CardPool::new);
    }

    /** Partie sur un état et un pool donnés (tests) ; {@code newState} doit créer un état neuf à chaque appel. */
    public Run(long seed, Set<Secret> unlocked, EventBus front, Supplier<GameState> newState, Supplier<CardPool> newPool) {
        this.seed = seed;
        this.unlockedIds = unlocked.stream().map(Secret::trinketId).collect(Collectors.toUnmodifiableSet());
        this.front = front;
        this.newState = newState;
        this.newPool = newPool;
    }

    public long seed() {
        return seed;
    }

    /** Code entré avant le début : le secret est équipé pour cette partie seulement. */
    public void addCode(Secret secret) {
        if (engine != null) {
            throw new IllegalStateException("La partie a déjà commencé");
        }
        if (!codes.contains(secret)) {
            codes.add(secret);
        }
    }

    public List<Secret> codes() {
        return List.copyOf(codes);
    }

    /** Secrets débloqués au début de la partie, qui sont dans le pool des récompenses. */
    public Set<Secret> unlockedSecrets() {
        java.util.EnumSet<Secret> secrets = java.util.EnumSet.noneOf(Secret.class);
        for (Secret secret : Secret.values()) {
            if (unlockedIds.contains(secret.trinketId())) {
                secrets.add(secret);
            }
        }
        return secrets;
    }

    /** Qui répond aux décisions demandées en cours d'action (l'interface, ou un test). */
    public void setDecisions(PlayerDecisions decisions) {
        this.decisions = decisions;
    }

    /** Lance la partie (codes équipés, premier combat). */
    public void start() {
        if (engine != null) {
            throw new IllegalStateException("La partie a déjà commencé");
        }
        EventBus bus = new EventBus();
        bus.subscribe(front::publish);
        engine = build(bus);
        engine.setDecisions(new Recording());
        engine.start();
    }

    private CombatEngine build(EventBus bus) {
        CombatEngine built = new CombatEngine(newState.get(), newPool.get(), new Rng(seed), bus,
                new LootTable(unlockedIds));
        for (Secret code : codes) {
            built.equip(ItemRegistry.trinket(code.trinketId()));
        }
        return built;
    }

    // --- Délégation au moteur, avec enregistrement ---

    public GameState state() {
        return engine.state();
    }

    public boolean isOver() {
        return engine.isOver();
    }

    /** Voir {@link CombatEngine#startTurn()}. */
    public boolean startTurn() {
        history.add(new StartTurn());
        boolean playerActs = engine.startTurn();
        if (playerActs) {
            marks.add(new TurnMark(history.size(), state().combatNumber(), state().turn()));
        }
        return playerActs;
    }

    /**
     * Voir {@link CombatEngine#submit(Action)}. {@link Action.Undo} est traitée ici : si
     * l'annulation est possible, la partie revient au début du tour précédent et le résultat est
     * {@link ActionResult#FREE_ACTION} (le joueur rejoue ce tour) ; sinon {@link ActionResult#REJECTED}.
     */
    public ActionResult submit(Action action) {
        if (action instanceof Action.Undo) {
            if (!canUndo()) {
                front.publish(new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.UNDO_UNAVAILABLE));
                return ActionResult.REJECTED;
            }
            undo();
            return ActionResult.FREE_ACTION;
        }
        history.add(new Submit(action));
        return engine.submit(action);
    }

    public boolean hasPendingReward() {
        return engine.hasPendingReward();
    }

    public List<Collectible> pendingReward() {
        return engine.pendingReward();
    }

    /** Voir {@link CombatEngine#resolveReward(RewardChoice)}. */
    public boolean resolveReward(RewardChoice choice) {
        history.add(new Reward(choice));
        return engine.resolveReward(choice);
    }

    // --- Ctrl+Z ---

    /**
     * {@code true} si le joueur peut annuler son dernier tour maintenant : Ctrl+Z équipé, pas
     * encore utilisé pendant ce combat, et un tour précédent dans ce même combat.
     */
    public boolean canUndo() {
        return engine != null && !isOver() && !hasPendingReward()
                && state().hasTrinket(ItemRegistry.CTRL_Z)
                && undoUsedInCombat != state().combatNumber()
                && previousTurn().isPresent();
    }

    /** Début du tour précédent du combat en cours (le dernier repère est le tour en cours). */
    private Optional<TurnMark> previousTurn() {
        if (marks.size() < 2) {
            return Optional.empty();
        }
        TurnMark current = marks.get(marks.size() - 1);
        TurnMark previous = marks.get(marks.size() - 2);
        boolean sameCombat = current.combat() == state().combatNumber() && previous.combat() == current.combat();
        return sameCombat ? Optional.of(previous) : Optional.empty();
    }

    private void undo() {
        TurnMark target = previousTurn().orElseThrow();
        undoUsedInCombat = target.combat();
        history.subList(target.historySize(), history.size()).clear();
        marks.subList(marks.indexOf(target) + 1, marks.size()).clear();
        engine = replay();
        front.publish(new GameEvent.TurnUndone(target.turn()));
    }

    /** Moteur neuf qui a rejoué tout l'historique sans rien relayer, puis branché sur {@code front}. */
    private CombatEngine replay() {
        EventBus bus = new EventBus();
        CombatEngine replayed = build(bus);
        Iterator<Step> steps = history.iterator();
        replayed.setDecisions(new Replaying(steps));
        replayed.start();
        while (steps.hasNext()) {
            switch (steps.next()) {
                case StartTurn s -> replayed.startTurn();
                case Submit s -> replayed.submit(s.action());
                case Reward r -> replayed.resolveReward(r.choice());
                case Decision d -> throw new IllegalStateException("Décision hors d'une action");
            }
        }
        bus.subscribe(front::publish);
        replayed.setDecisions(new Recording());
        return replayed;
    }

    /** Transmet les décisions du joueur et les enregistre. */
    private final class Recording implements PlayerDecisions {

        @Override
        public int chooseDrawnCard(GameState state, List<Card> options) {
            return record(decisions.chooseDrawnCard(state, options));
        }

        @Override
        public int chooseTrinketToDiscard(GameState state) {
            return record(decisions.chooseTrinketToDiscard(state));
        }

        private int record(int index) {
            history.add(new Decision(index));
            return index;
        }
    }

    /** Redonne les décisions enregistrées, dans l'ordre. */
    private static final class Replaying implements PlayerDecisions {

        private final Iterator<Step> steps;

        Replaying(Iterator<Step> steps) {
            this.steps = steps;
        }

        @Override
        public int chooseDrawnCard(GameState state, List<Card> options) {
            return next();
        }

        @Override
        public int chooseTrinketToDiscard(GameState state) {
            return next();
        }

        private int next() {
            if (steps.hasNext() && steps.next() instanceof Decision d) {
                return d.index();
            }
            throw new IllegalStateException("Rejeu désynchronisé : décision attendue");
        }
    }
}

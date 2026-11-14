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
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.enemy.EnemyRegistry;
import cardistry.run.map.MapGenerator;
import cardistry.run.map.MapNode;
import cardistry.run.map.NodeType;
import cardistry.run.map.RunMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Une partie : la carte des 3 niveaux, le moteur de combat, et ce qui dépasse un moteur seul.
 * <ul>
 *   <li>la <b>carte</b> : à chaque niveau, une carte générée par un sous-flux de la seed ; le
 *       joueur choisit un nœud ({@link #chooseNode}), le combat se joue, puis il choisit le suivant.
 *       Le boss du niveau 3 termine la partie ;</li>
 *   <li>les <b>codes</b> : secrets équipés dès le départ, pour cette partie seulement ;</li>
 *   <li>les <b>secrets débloqués</b>, qui entrent dans le pool des récompenses ;</li>
 *   <li>l'<b>annulation de tour</b> (trinket Ctrl+Z) : chaque appel et chaque décision du joueur
 *       sont enregistrés ; annuler = reconstruire une partie neuve avec la même seed et rejouer
 *       l'historique jusqu'au début du tour précédent. Tout l'aléatoire passant par des {@link Rng}
 *       dérivés de la seed, le rejeu redonne exactement le même état.</li>
 * </ul>
 * Avec un état à adversaires fixés (tests), il n'y a pas de carte : le moteur enchaîne les combats.
 * Les événements sont relayés sur le bus {@code front} (interface, déblocages), sauf pendant un rejeu.
 */
public final class Run {

    /** Ce que la partie attend du joueur. */
    public enum Phase {
        /** Choisir le prochain nœud de la carte. */
        MAP,
        /** Jouer un tour de combat. */
        COMBAT,
        /** Choisir une récompense. */
        REWARD,
        /** Partie terminée. */
        OVER
    }

    /** Un appel (ou une réponse du joueur pendant un appel), pour le rejeu. */
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

    private record ChooseNode(int index) implements Step {
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
    private Session session;

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
        if (session != null) {
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

    /** Lance la partie (codes équipés, premier niveau ou premier combat). */
    public void start() {
        if (session != null) {
            throw new IllegalStateException("La partie a déjà commencé");
        }
        EventBus bus = new EventBus();
        bus.subscribe(front::publish);
        session = new Session(bus);
        session.engine.setDecisions(new Recording());
        session.start();
    }

    // --- État ---

    public GameState state() {
        return session.engine.state();
    }

    public boolean isOver() {
        return session.engine.isOver();
    }

    /** Ce que la partie attend maintenant. */
    public Phase phase() {
        return session.phase();
    }

    /** Carte du niveau en cours ; vide sans carte (adversaires fixés). */
    public Optional<RunMap> map() {
        return Optional.ofNullable(session.map);
    }

    /** Dernier nœud visité du niveau en cours ({@code null} au départ du niveau). */
    public MapNode position() {
        return session.position;
    }

    /** Nœuds visités dans le niveau en cours, dans l'ordre. */
    public List<MapNode> visited() {
        return List.copyOf(session.visited);
    }

    /** Nœuds où le joueur peut aller maintenant (phase {@link Phase#MAP}). */
    public List<MapNode> choices() {
        return session.map == null ? List.of() : session.map.choicesAfter(session.position);
    }

    // --- Actions, avec enregistrement ---

    /** Entre dans le nœud {@code index} de {@link #choices()}. */
    public void chooseNode(int index) {
        if (phase() != Phase.MAP) {
            throw new IllegalStateException("Pas de choix de nœud maintenant");
        }
        if (index < 0 || index >= choices().size()) {
            throw new IllegalArgumentException("Nœud inexistant : " + index);
        }
        history.add(new ChooseNode(index));
        session.chooseNode(index);
    }

    /** Voir {@link CombatEngine#startTurn()}. */
    public boolean startTurn() {
        history.add(new StartTurn());
        boolean playerActs = session.engine.startTurn();
        if (playerActs) {
            marks.add(new TurnMark(history.size(), state().combatNumber(), state().turn()));
        }
        session.settle();
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
        ActionResult result = session.engine.submit(action);
        session.settle();
        return result;
    }

    public boolean hasPendingReward() {
        return session.engine.hasPendingReward();
    }

    public List<Collectible> pendingReward() {
        return session.engine.pendingReward();
    }

    /** Voir {@link CombatEngine#resolveReward(RewardChoice)}. */
    public boolean resolveReward(RewardChoice choice) {
        history.add(new Reward(choice));
        boolean resolved = session.engine.resolveReward(choice);
        session.settle();
        return resolved;
    }

    // --- Ctrl+Z ---

    /**
     * {@code true} si le joueur peut annuler son dernier tour maintenant : Ctrl+Z équipé, pas
     * encore utilisé pendant ce combat, et un tour précédent dans ce même combat.
     */
    public boolean canUndo() {
        return session != null && phase() == Phase.COMBAT
                && state().hasTrinket(ItemRegistry.CTRL_Z)
                && !state().undoUsedThisCombat()
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
        history.subList(target.historySize(), history.size()).clear();
        marks.subList(marks.indexOf(target) + 1, marks.size()).clear();
        session = replay();
        session.engine.markUndoUsed();
        front.publish(new GameEvent.TurnUndone(target.turn()));
    }

    /** Partie neuve qui a rejoué tout l'historique sans rien relayer, puis branchée sur {@code front}. */
    private Session replay() {
        EventBus bus = new EventBus();
        Session replayed = new Session(bus);
        Iterator<Step> steps = history.iterator();
        replayed.engine.setDecisions(new Replaying(steps));
        replayed.start();
        while (steps.hasNext()) {
            switch (steps.next()) {
                case StartTurn s -> {
                    replayed.engine.startTurn();
                    replayed.settle();
                }
                case Submit s -> {
                    replayed.engine.submit(s.action());
                    replayed.settle();
                }
                case Reward r -> {
                    replayed.engine.resolveReward(r.choice());
                    replayed.settle();
                }
                case ChooseNode c -> replayed.chooseNode(c.index());
                case Decision d -> throw new IllegalStateException("Décision hors d'une action");
            }
        }
        bus.subscribe(front::publish);
        replayed.engine.setDecisions(new Recording());
        return replayed;
    }

    /** Le moteur et la progression sur la carte : tout ce qu'un rejeu reconstruit. */
    private final class Session {

        final CombatEngine engine;
        final boolean mapped;
        RunMap map;
        MapNode position;
        final List<MapNode> visited = new ArrayList<>();
        Rng encounters;
        String previousCommon;
        String previousElite;

        Session(EventBus bus) {
            GameState state = newState.get();
            engine = new CombatEngine(state, newPool.get(), new Rng(seed), bus, new LootTable(unlockedIds));
            for (Secret code : codes) {
                engine.equip(ItemRegistry.trinket(code.trinketId()));
            }
            mapped = state.isOpenEnded();
        }

        void start() {
            engine.start();
            if (mapped) {
                startLevel(1);
            }
        }

        Phase phase() {
            if (engine.isOver()) {
                return Phase.OVER;
            }
            if (engine.hasPendingReward()) {
                return Phase.REWARD;
            }
            if (engine.combatActive() || !mapped) {
                return Phase.COMBAT;
            }
            return Phase.MAP;
        }

        void startLevel(int level) {
            map = MapGenerator.generate(new Rng(seed).fork("carte-" + level), level);
            position = null;
            visited.clear();
            encounters = new Rng(seed).fork("rencontres-" + level);
            engine.startLevel(level);
        }

        void chooseNode(int index) {
            MapNode node = map.choicesAfter(position).get(index);
            position = node;
            visited.add(node);
            engine.enterNode(node.floor(), node.type().displayName());
            int level = map.level();
            switch (node.type()) {
                case COMBAT -> {
                    String id = EnemyRegistry.pickCommon(encounters, previousCommon);
                    previousCommon = id;
                    engine.startCombat(EnemyRegistry.create(id, level), false);
                }
                case ELITE -> {
                    String id = EnemyRegistry.pickElite(encounters, previousElite);
                    previousElite = id;
                    engine.startCombat(EnemyRegistry.create(id, level), false);
                }
                case BOSS -> {
                    Enemy boss = EnemyRegistry.create(EnemyRegistry.bossId(level), level);
                    engine.startCombat(boss, level == EnemyRegistry.LEVELS);
                }
                case REST, SHOP, EVENT -> {
                    // Sieste, shop et événements : à venir (lots suivants). On passe.
                }
            }
            settle();
        }

        /** Après chaque étape : le boss d'un niveau battu (récompense prise) ouvre le niveau suivant. */
        void settle() {
            if (!mapped || phase() != Phase.MAP || position == null || position.type() != NodeType.BOSS) {
                return;
            }
            startLevel(map.level() + 1);
        }
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

package cardistry.core;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.event.HookDispatcher;
import cardistry.core.stats.StatType;
import cardistry.items.Collectible;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Tag;
import cardistry.items.Trinket;
import cardistry.model.Element;
import cardistry.model.ElementRules;
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
 * <p>Après la mort d'un adversaire (sauf le dernier), le moteur propose des récompenses et attend
 * {@link #resolveReward(RewardChoice)} avant de lancer le combat suivant.
 *
 * <p>Les trinkets équipés reçoivent chaque événement (voir {@link cardistry.core.event.GameHooks}).
 */
public final class CombatEngine {

    private final GameState state;
    private final CardPool pool;
    private final Rng rng;
    private final EventBus bus;
    private final LootTable loot;
    private final EffectContext effects = new Effects();
    private List<Collectible> pendingReward = List.of();

    public CombatEngine(GameState state, CardPool pool, Rng rng, EventBus bus) {
        this(state, pool, rng, bus, new LootTable());
    }

    public CombatEngine(GameState state, CardPool pool, Rng rng, EventBus bus, LootTable loot) {
        this.state = state;
        this.pool = pool;
        this.rng = rng;
        this.bus = bus;
        this.loot = loot;
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

    /**
     * Début de tour : les effets temporaires avancent d'un tour, puis le joueur joue ou saute son
     * tour (l'adversaire attaque alors directement).
     *
     * @return {@code true} si le joueur doit choisir une action ce tour-ci
     */
    public boolean startTurn() {
        state.nextTurn();
        bus.publish(new GameEvent.TurnStarted(state.turn()));
        tickEffects();
        if (isOver() || resolveEnemyDeath()) {
            return false;
        }
        List<String> skipCauses = skipCauses();
        if (skipCauses.isEmpty()) {
            bus.publish(new GameEvent.PlayerTurnStarted(state.turn()));
            return !isOver();
        }
        bus.publish(new GameEvent.PlayerTurnSkipped(skipCauses));
        enemyAttack();
        return false;
    }

    // --- Effets temporaires ---

    private void addEffect(TemporaryEffect effect) {
        state.addEffect(effect);
        bus.publish(new GameEvent.EffectAdded(effect));
        if (effect.delayTurns() <= 0) {
            activateEffect(effect);
        }
    }

    private void activateEffect(TemporaryEffect effect) {
        state.activateEffect(effect);
        if (effect.delayTurns() > 0) {
            bus.publish(new GameEvent.EffectActivated(effect));
        }
        effect.runOnActivate(effects);
    }

    private void endEffect(TemporaryEffect effect) {
        if (state.effects().contains(effect)) {
            state.removeEffect(effect);
            bus.publish(new GameEvent.EffectEnded(effect));
        }
    }

    /** Début de tour : compte à rebours, déclenchements, durées, actions de début de tour. */
    private void tickEffects() {
        for (TemporaryEffect effect : List.copyOf(state.effects())) {
            if (!effect.isActive()) {
                if (effect.tickDelay()) {
                    activateEffect(effect);
                }
            } else if (effect.tickDuration()) {
                endEffect(effect);
                continue;
            }
            if (effect.isActive() && state.effects().contains(effect)) {
                effect.runOnTurnStart(effects);
            }
        }
    }

    /** Effets qui font sauter ce tour ; ceux à charges en consomment une. */
    private List<String> skipCauses() {
        List<String> causes = new java.util.ArrayList<>();
        for (TemporaryEffect effect : List.copyOf(state.effects())) {
            if (!effect.isActive()) {
                continue;
            }
            switch (effect.skipRule()) {
                case NEXT_TURN -> {
                    causes.add(effect.name());
                    if (effect.consumeCharge()) {
                        endEffect(effect);
                    }
                }
                case EVEN_COMBAT_TURNS -> {
                    if (state.combatTurn() % 2 == 0) {
                        causes.add(effect.name());
                    }
                }
                case NONE -> {
                }
            }
        }
        return causes;
    }

    private void consume(TemporaryEffect effect) {
        if (effect.consumeCharge()) {
            endEffect(effect);
        }
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
        if (hasPendingReward()) {
            throw new IllegalStateException("Une récompense attend d'être choisie");
        }
        Optional<Reason> rejection = validate(action);
        if (rejection.isPresent()) {
            bus.publish(new GameEvent.ActionRejected(rejection.get()));
            return ActionResult.REJECTED;
        }
        boolean endsTurn = switch (action) {
            case Action.DrawCard draw -> {
                int count = Math.max(1, state.playerStats().getInt(StatType.CARDS_PER_DRAW));
                for (int i = 0; i < count && state.hand().size() < state.handSize(); i++) {
                    drawCard();
                }
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
            case Action.Smoke smoke -> {
                smoke();
                yield false;
            }
            case Action.RollCigarette roll -> {
                state.setCigarettes(state.cigarettes() + 1);
                bus.publish(new GameEvent.CigaretteRolled());
                bus.publish(new GameEvent.CigarettesChanged(state.cigarettes(), state.cigaretteMax()));
                yield true;
            }
        };
        if (state.isLost()) {
            return ActionResult.TURN_ENDED; // mort de son propre effet (GameLost déjà publié)
        }
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
            case Action.Smoke smoke -> state.cigarettes() <= 0
                    ? Optional.of(Reason.NO_CIGARETTE)
                    : Optional.empty();
            case Action.RollCigarette roll -> state.cigarettes() >= state.cigaretteMax()
                    ? Optional.of(Reason.PACK_FULL)
                    : Optional.empty();
        };
    }

    // --- Combats ---

    /** Nouvelle main, compteur de tours du combat remis à zéro. */
    private void beginCombat() {
        state.resetCombatTurn();
        state.clearHand();
        bus.publish(new GameEvent.CombatStarted(state.currentEnemy(), state.combatNumber(), state.enemies().size()));
        for (Curse curse : state.activatePendingCurses()) {
            bus.publish(new GameEvent.CurseStarted(curse));
        }
        int dealt = Math.min(state.playerStats().getInt(StatType.STARTING_HAND), state.handSize());
        for (int i = 0; i < dealt; i++) {
            state.addToHand(drawFromPool());
        }
        bus.publish(new GameEvent.HandDealt(state.hand()));
    }

    /** Fin du combat : l'Ivresse retombe, les malédictions du combat s'arrêtent. */
    private void endCombat() {
        for (TemporaryEffect effect : List.copyOf(state.effects())) {
            if (!effect.keepsAcrossCombats()) {
                endEffect(effect);
            }
        }
        if (state.drunkenness() != 0) {
            state.setDrunkenness(0);
            bus.publish(new GameEvent.DrunkennessChanged(0, state.hangoverThreshold()));
        }
        state.setHangoverThisCombat(false);
        for (Curse curse : state.endActiveCurses()) {
            bus.publish(new GameEvent.CurseEnded(curse));
        }
    }

    /**
     * Ajoute de l'Ivresse. Au seuil, une Gueule de bois est programmée pour le combat suivant
     * (une seule par combat ; aucune pendant le dernier combat).
     */
    private void addDrunkenness(int amount) {
        int updated = Math.max(0, state.drunkenness() + amount);
        if (updated == state.drunkenness()) {
            return;
        }
        state.setDrunkenness(updated);
        bus.publish(new GameEvent.DrunkennessChanged(state.drunkenness(), state.hangoverThreshold()));
        if (amount > 0 && state.drunkenness() >= state.hangoverThreshold() && !state.hangoverThisCombat() && state.hasNextEnemy()) {
            state.setHangoverThisCombat(true);
            state.addPendingCurse(Curse.HANGOVER, 1);
            bus.publish(new GameEvent.CurseGained(Curse.HANGOVER));
        }
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
        endCombat();
        if (!state.hasNextEnemy()) {
            bus.publish(new GameEvent.GameWon());
            return true;
        }
        List<Collectible> offer = loot.offer(state, rng, REWARD_OPTIONS);
        if (offer.isEmpty()) {
            nextCombat();
        } else {
            pendingReward = offer;
            bus.publish(new GameEvent.RewardOffered(offer));
        }
        return true;
    }

    private void nextCombat() {
        state.advanceEnemy();
        beginCombat();
    }

    // --- Récompenses ---

    /** Nombre de propositions après chaque adversaire vaincu. */
    public static final int REWARD_OPTIONS = 3;

    public boolean hasPendingReward() {
        return !pendingReward.isEmpty();
    }

    /** Propositions en attente (vide s'il n'y en a pas). */
    public List<Collectible> pendingReward() {
        return pendingReward;
    }

    /**
     * Applique le choix du joueur puis lance le combat suivant.
     *
     * @return {@code false} si le choix est invalide (un {@link GameEvent.ActionRejected} est publié,
     *     l'offre reste en attente)
     */
    public boolean resolveReward(RewardChoice choice) {
        if (!hasPendingReward()) {
            throw new IllegalStateException("Aucune récompense en attente");
        }
        switch (choice) {
            case RewardChoice.Skip skip -> bus.publish(new GameEvent.RewardSkipped());
            case RewardChoice.Take take -> {
                Optional<Reason> rejection = validateReward(take);
                if (rejection.isPresent()) {
                    bus.publish(new GameEvent.ActionRejected(rejection.get()));
                    return false;
                }
                Collectible taken = pendingReward.get(take.option());
                Collectible replaced = switch (taken) {
                    case Trinket trinket -> {
                        if (take.replaceSlot() >= 0) {
                            yield state.replaceTrinket(take.replaceSlot(), trinket);
                        }
                        state.equipTrinket(trinket);
                        yield null;
                    }
                    case Item item -> {
                        Item old = take.replaceSlot() >= 0 ? state.removeItem(take.replaceSlot()) : null;
                        state.addItem(item);
                        yield old;
                    }
                };
                bus.publish(new GameEvent.RewardTaken(taken, replaced));
                publishSetChanges();
            }
        }
        pendingReward = List.of();
        nextCombat();
        return true;
    }

    private Optional<Reason> validateReward(RewardChoice.Take take) {
        if (take.option() < 0 || take.option() >= pendingReward.size()) {
            return Optional.of(Reason.INVALID_REWARD);
        }
        Collectible taken = pendingReward.get(take.option());
        int used = taken instanceof Trinket ? state.trinkets().size() : state.items().size();
        int slots = taken instanceof Trinket ? state.trinketSlots() : state.itemSlots();
        if (take.replaceSlot() < 0) {
            return used < slots ? Optional.empty() : Optional.of(Reason.INVENTORY_FULL);
        }
        return take.replaceSlot() < used ? Optional.empty() : Optional.of(Reason.INVALID_REWARD);
    }

    // --- Actions du joueur ---

    private Card drawFromPool() {
        return pool.draw(rng, state::cardWeight);
    }

    private void drawCard() {
        Card card = drawFromPool();
        state.addToHand(card);
        bus.publish(new GameEvent.CardDrawn(card));
    }

    private void playCard(int slot) {
        Card card = state.removeFromHand(slot);
        bus.publish(new GameEvent.CardPlayed(card));
        switch (card) {
            case SpellCard spell -> {
                Optional<TemporaryEffect> boost = state.nextSpellBoost();
                double mult = state.spellMultiplier(spell);
                damageEnemy(spell.damage() * mult, mult, GameEvent.DamageDealt.Source.SPELL);
                boost.ifPresent(this::consume);
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

    /** Identifiant de l'objet dont une clope transformée donne l'effet (Googz). */
    static final String PETIT_PETOU = "petit-petou";

    /**
     * Fumer : −5 PV bruts (ou, selon la stat {@link StatType#CIGARETTE_TRANSFORM_CHANCE}, l'effet de
     * l'objet Petit pétou à la place), puis la Gueule de bois perd 1 d'intensité.
     */
    private void smoke() {
        state.setCigarettes(state.cigarettes() - 1);
        bus.publish(new GameEvent.CigarettesChanged(state.cigarettes(), state.cigaretteMax()));
        double chance = state.playerStats().get(StatType.CIGARETTE_TRANSFORM_CHANCE);
        boolean transformed = chance > 0 && rng.nextDouble() < chance;
        bus.publish(new GameEvent.CigaretteSmoked(transformed));
        if (transformed) {
            cardistry.items.ItemRegistry.item(PETIT_PETOU).use(effects);
        } else {
            effects.damagePlayer(5);
        }
        if (state.player().isAlive()) {
            reduceCurse(Curse.HANGOVER);
        }
    }

    /** Retire 1 d'intensité : d'abord à la malédiction active, sinon à celle à venir. */
    private void reduceCurse(Curse curse) {
        int active = state.activeIntensity(curse);
        if (active > 0) {
            state.setActiveIntensity(curse, active - 1);
            if (active - 1 == 0) {
                bus.publish(new GameEvent.CurseEnded(curse));
            } else {
                bus.publish(new GameEvent.CurseReduced(curse, active - 1, false));
            }
            return;
        }
        int pending = state.pendingIntensity(curse);
        if (pending > 0) {
            state.setPendingIntensity(curse, pending - 1);
            bus.publish(new GameEvent.CurseReduced(curse, pending - 1, true));
        }
    }

    private void useItem(int slot) {
        Item item = state.removeItem(slot);
        bus.publish(new GameEvent.ItemUsed(item));
        item.use(effects);
        if (item.tags().contains(Tag.DRINK)) {
            addDrunkenness(1);
        }
    }

    private void enemyAttack() {
        Enemy enemy = state.currentEnemy();
        Player player = state.player();
        double mult = state.enemyAttackMultiplier();
        double amount = state.enemyAttackDamage();
        bus.publish(new GameEvent.EnemyAttacked(enemy));
        Optional<TemporaryEffect> shield = state.effects().stream()
                .filter(e -> e.isActive() && e.cancelsEnemyAttack()).findFirst();
        if (shield.isPresent()) {
            bus.publish(new GameEvent.AttackCancelled(enemy, shield.get().name()));
            consume(shield.get());
            return;
        }
        player.takeDamage(amount);
        bus.publish(new GameEvent.DamageDealt(player, amount, mult, GameEvent.DamageDealt.Source.ENEMY_ATTACK));
        if (!player.isAlive()) {
            bus.publish(new GameEvent.GameLost());
        }
    }

    // --- Primitives partagées par les cartes et les effets ---

    private void damageEnemy(double amount, double mult, GameEvent.DamageDealt.Source source) {
        Enemy enemy = state.currentEnemy();
        enemy.takeDamage(amount);
        bus.publish(new GameEvent.DamageDealt(enemy, amount, mult, source));
    }

    /**
     * Soigne sans dépasser les PV max (sauf si les soins ne sont pas plafonnés) ; l'événement porte
     * les PV réellement rendus.
     */
    private void healPlayer(double amount, double mult) {
        Player player = state.player();
        double missing = Math.max(0, state.maxHp() - player.hp());
        double healed = state.healingUncapped() ? amount : Math.min(amount, missing);
        player.heal(healed);
        bus.publish(new GameEvent.Healed(player, healed, mult));
    }

    private void dispatchToTrinkets(GameEvent event) {
        List<Trinket> equipped = List.copyOf(state.trinkets());
        for (Trinket trinket : equipped) {
            HookDispatcher.dispatch(trinket.hooks(), event, effects);
        }
        for (GameState.ActiveSetTier active : List.copyOf(state.activeSetTiers())) {
            HookDispatcher.dispatch(active.tier().hooks(), event, effects);
        }
    }

    /** Notifie les paliers de set activés / désactivés depuis le dernier changement d'inventaire. */
    private void publishSetChanges() {
        for (GameState.SetChange change : state.drainSetChanges()) {
            var set = change.tier().set();
            var tier = change.tier().tier();
            bus.publish(change.activated()
                    ? new GameEvent.SetTierActivated(set.displayName(), set.tag(), tier.count(), tier.description())
                    : new GameEvent.SetTierDeactivated(set.displayName(), set.tag(), tier.count(), tier.description()));
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
            CombatEngine.this.damageEnemy(amount, 1.0, GameEvent.DamageDealt.Source.EFFECT);
        }

        @Override
        public void elementalDamage(double amount, Element element) {
            double mult = ElementRules.spellMultiplier(element, state.currentEnemy().element())
                    * state.playerStats().get(StatType.damageMultiplier(element));
            CombatEngine.this.damageEnemy(amount * mult, mult, GameEvent.DamageDealt.Source.EFFECT);
        }

        @Override
        public void damagePlayer(double amount) {
            Player player = state.player();
            boolean wasAlive = player.isAlive();
            player.takeDamage(amount);
            bus.publish(new GameEvent.DamageDealt(player, amount, 1.0, GameEvent.DamageDealt.Source.EFFECT));
            if (wasAlive && !player.isAlive()) {
                bus.publish(new GameEvent.GameLost());
            }
        }

        @Override
        public double potency(String trinketId) {
            return state.trinkets().stream().filter(t -> t.id().equals(trinketId)).findFirst()
                    .map(state::potency).orElse(1.0);
        }

        @Override
        public int addCigarettes(int count) {
            int added = Math.max(0, Math.min(count, state.cigaretteMax() - state.cigarettes()));
            if (added > 0) {
                state.setCigarettes(state.cigarettes() + added);
                bus.publish(new GameEvent.CigarettesChanged(state.cigarettes(), state.cigaretteMax()));
            }
            return added;
        }

        @Override
        public void addEffect(TemporaryEffect effect) {
            CombatEngine.this.addEffect(effect);
        }

        @Override
        public void addDrunkenness(int amount) {
            CombatEngine.this.addDrunkenness(amount);
        }

        @Override
        public void replaceCardInHand(int slot) {
            Card replacement = drawFromPool();
            Card removed = state.replaceInHand(slot, replacement);
            bus.publish(new GameEvent.CardReplaced(removed, replacement));
        }

        @Override
        public void elementalHeal(double amount, Element element) {
            double mult = state.healMultiplier(element);
            CombatEngine.this.healPlayer(amount * mult, mult);
        }

        @Override
        public void healPlayer(double amount) {
            double mult = state.playerStats().get(StatType.HEAL_EFFICIENCY);
            CombatEngine.this.healPlayer(amount * mult, mult);
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
        publishSetChanges();
    }

    /** Ajoute un objet à l'inventaire. */
    public void give(Item item) {
        state.addItem(item);
    }
}

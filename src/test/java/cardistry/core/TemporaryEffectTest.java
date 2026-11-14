package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class TemporaryEffectTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy first;

    /** Adversaire feu de 100 PV / 10 d'attaque sur terrain Forêt (attaque ×1), puis un second. */
    private void setUp(List<Card> hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        first = new Enemy("Hydre", FIRE, 100, 10);
        state = new GameState(new Player(100), List.of(first, new Enemy("Sirène", WATER, 100, 10)),
                new TerrainCard("Forêt", NATURE), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private void addEffect(Supplier<TemporaryEffect> effect) {
        engine.give(new Item("x", "x", "x", "", Rarity.COMMON, Set.of(Tag.META), ctx -> ctx.addEffect(effect.get())));
        assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.UseItem(state.items().size() - 1)));
    }

    private long count(Class<? extends GameEvent> type) {
        return events.stream().filter(type::isInstance).count();
    }

    private void killFirstEnemy() {
        first.takeDamage(1000);
        engine.submit(new Action.DrawCard());
    }

    // --- Délai et durée ---

    @Test
    void delaiPuisDureeEnTours() {
        setUp(List.of());
        state.player().takeDamage(50);
        addEffect(() -> TemporaryEffect.builder("cake", "Cake").summary("+50 % dégâts").delay(3).lasts(2)
                .percent(StatType.FIRE_DAMAGE, 0.5).onActivate(ctx -> ctx.healPlayer(40)).build());
        TemporaryEffect cake = state.effects().get(0);
        assertFalse(cake.isActive());
        assertEquals("Cake : dans 3 tours", cake.describe());

        engine.startTurn(); // tour 1
        engine.startTurn(); // tour 2
        assertFalse(cake.isActive());
        assertEquals("Cake : dans 1 tour", cake.describe());
        assertEquals(1.0, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);

        engine.startTurn(); // tour 3 : déclenchement
        assertTrue(cake.isActive());
        assertTrue(events.contains(new GameEvent.EffectActivated(cake)));
        assertEquals(90, state.player().hp(), DELTA);
        assertEquals(1.5, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);
        assertEquals("Cake : +50 % dégâts, 2 tours", cake.describe());

        engine.startTurn(); // tour 4 : encore actif
        assertEquals("Cake : +50 % dégâts, 1 tour", cake.describe());
        assertEquals(1.5, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);

        engine.startTurn(); // tour 5 : terminé
        assertTrue(state.effects().isEmpty());
        assertTrue(events.contains(new GameEvent.EffectEnded(cake)));
        assertEquals(1.0, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);
    }

    // --- Prochain sort ---

    @Test
    void prochainSortMultiplieSeulementParUnSort() {
        setUp(List.of(new HealCard("Potion", NATURE, 10), new SpellCard("Sort", FIRE, 20)));
        addEffect(() -> TemporaryEffect.builder("shot", "Shot").summary("prochain sort ×2").charges(1)
                .multipliesNextSpell(2).build());
        assertEquals(2.0, state.spellMultiplier((SpellCard) state.hand().get(1)), DELTA); // aperçu

        engine.submit(new Action.PlayCard(0)); // un soin ne consomme pas l'effet
        assertEquals(1, state.effects().size());

        engine.submit(new Action.PlayCard(0)); // le sort
        assertEquals(60, first.hp(), DELTA);
        assertTrue(state.effects().isEmpty());
    }

    @Test
    void deuxBoostsUnParSort() {
        setUp(List.of(new SpellCard("Sort", FIRE, 10), new SpellCard("Sort", FIRE, 10), new SpellCard("Sort", FIRE, 10)));
        Supplier<TemporaryEffect> shot = () -> TemporaryEffect.builder("shot", "Shot").charges(1)
                .multipliesNextSpell(2).build();
        addEffect(shot);
        addEffect(shot);
        engine.submit(new Action.PlayCard(0));
        engine.submit(new Action.PlayCard(0));
        engine.submit(new Action.PlayCard(0));
        assertEquals(100 - 20 - 20 - 10, first.hp(), DELTA);
    }

    // --- Annuler une attaque ---

    @Test
    void annuleLaProchaineAttaque() {
        setUp(List.of());
        addEffect(() -> TemporaryEffect.builder("mule", "Mule").charges(1).cancelsNextEnemyAttack().build());

        engine.submit(new Action.DrawCard());
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.AttackCancelled(first, "Mule")));
        assertTrue(state.effects().isEmpty());

        engine.submit(new Action.DrawCard());
        assertEquals(90, state.player().hp(), DELTA);
    }

    // --- Tour sauté de l'adversaire ---

    @Test
    void lAdversaireSauteSonProchainTourAvantUneAnnulation() {
        setUp(List.of());
        addEffect(() -> TemporaryEffect.builder("mule", "Mule").charges(1).cancelsNextEnemyAttack().build());
        addEffect(() -> TemporaryEffect.builder("pause", "Pause").charges(1)
                .skipsNextEnemyTurn("{adversaire} passe son tour").build());

        engine.submit(new Action.DrawCard());
        assertEquals(new GameEvent.EnemyTurnSkipped(first, "Pause", "Hydre passe son tour"),
                events.stream().filter(GameEvent.EnemyTurnSkipped.class::isInstance).findFirst().orElseThrow());
        assertFalse(events.stream().anyMatch(GameEvent.EnemyAttacked.class::isInstance));
        assertEquals(List.of("mule"), state.effects().stream().map(TemporaryEffect::id).toList(), "le Mule reste");

        engine.submit(new Action.DrawCard());   // le Mule annule l'attaque suivante
        assertEquals(100, state.player().hp(), DELTA);
        engine.submit(new Action.DrawCard());
        assertEquals(90, state.player().hp(), DELTA);
    }

    // --- Sauter un tour ---

    @Test
    void sauterLeProchainTour() {
        setUp(List.of());
        addEffect(() -> TemporaryEffect.builder("raclette", "Raclette").charges(1).skipsNextTurn().build());

        assertFalse(engine.startTurn());
        assertEquals(new GameEvent.PlayerTurnSkipped(List.of("Raclette")),
                events.stream().filter(GameEvent.PlayerTurnSkipped.class::isInstance).findFirst().orElseThrow());
        assertEquals(90, state.player().hp(), DELTA); // l'adversaire attaque
        assertTrue(state.effects().isEmpty());

        assertTrue(engine.startTurn());
    }

    // --- Fin de combat ---

    @Test
    void perduALaFinDuCombatSaufSiGarde() {
        setUp(List.of());
        addEffect(() -> TemporaryEffect.builder("perdu", "Perdu").charges(1).multipliesNextSpell(2).build());
        addEffect(() -> TemporaryEffect.builder("garde", "Gardé").delay(5).keepAcrossCombats().build());

        killFirstEnemy();

        assertEquals(List.of("garde"), state.effects().stream().map(TemporaryEffect::id).toList());
        assertEquals(1, count(GameEvent.EffectEnded.class));
    }

    @Test
    void leCompteAReboursContinueAuCombatSuivant() {
        setUp(List.of());
        addEffect(() -> TemporaryEffect.builder("cake", "Cake").delay(3).lasts(2).keepAcrossCombats()
                .percent(StatType.WATER_DAMAGE, 0.5).build());
        engine.startTurn();
        engine.startTurn();          // il reste 1 tour
        killFirstEnemy();            // combat suivant
        TemporaryEffect cake = state.effects().get(0);
        assertEquals(1, cake.delayLeft());

        engine.startTurn();          // tour 1 du combat 2 : déclenchement
        assertTrue(cake.isActive());
        assertEquals(1.5, state.playerStats().get(StatType.WATER_DAMAGE), DELTA);
    }
}

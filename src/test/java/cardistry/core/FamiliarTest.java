package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.hero.HeroRegistry;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Familiar;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import cardistry.model.enemy.MoveEffect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** L'alien de Sassy, un familier : 20 PV, visé par 25 % des attaques, assommé à 0 PV. */
class FamiliarTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Familiar alien;

    /** Sassy contre deux adversaires neutres de 500 PV qui jouent {@code effects} à chaque tour. */
    private void setUp(long seed, MoveEffect... effects) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        List<Enemy> enemies = new ArrayList<>();
        for (String name : List.of("A", "B")) {
            enemies.add(Enemy.builder(name.toLowerCase(), name, Element.NEUTRAL, EnemyRank.COMMON).hp(500)
                    .behavior(EnemyBehavior.always(EnemyMove.of("coup", "Coup", "Coup", effects))).build());
        }
        state = new GameState(new Player(130), enemies, new TerrainCard("Neutre", Element.NEUTRAL, Rarity.COMMON),
                List.of(), new Stats())
                .withHero(HeroRegistry.SASSY);
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
        engine.start();
        alien = state.familiar().orElseThrow();
    }

    /** Fin du tour sans rien jouer. */
    private void endTurn() {
        engine.submit(new Action.RollCigarette());
    }

    @Test
    void sassyAUnAlienDeVingtPvLesAutresNon() {
        assertEquals(20, GameState.newGame(HeroRegistry.SASSY).familiar().orElseThrow().hp());
        assertFalse(GameState.newGame(HeroRegistry.LOUIS).familiar().isPresent());
        assertFalse(GameState.newGame().familiar().isPresent());
    }

    @Test
    void uneAttaqueSurQuatreViseLAlien() {
        int targeted = 0;
        int runs = 400;
        for (long seed = 0; seed < runs; seed++) {
            setUp(seed, MoveEffect.Hit.of(5));
            targeted += state.familiarTargeted() ? 1 : 0;
        }
        assertTrue(targeted > 0.18 * runs && targeted < 0.32 * runs, "visé : " + targeted + "/" + runs);
    }

    @Test
    void uneAttaqueSansCoupNeLeViseJamais() {
        for (long seed = 0; seed < 100; seed++) {
            setUp(seed, new MoveEffect.HealSelf(1));
            assertFalse(state.familiarTargeted(), "seed " + seed);
        }
    }

    @Test
    void leCoupVaSurLAlienSansTaDefEtSansStatut() {
        setUp(1, MoveEffect.Hit.of(6), new MoveEffect.Inflict("brulure", 1.0, 2, true));
        state.playerStats().add(StatModifier.flat(StatType.DEFENSE, 5, "test"));
        alien.setTargeted(true);
        endTurn();
        assertEquals(130, state.player().hp(), "Sassy n'est pas touchée");
        assertEquals(14, alien.hp(), "6 dégâts, sa DEF ne le protège pas");
        assertFalse(state.statuses(StatusTarget.PLAYER).has(StatusType.BURN), "statut « après un coup » perdu");
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.DamageDealt d && d.target() == alien));
    }

    @Test
    void aZeroPvIlEstAssommeJusquALaFinDuCombatPuisRevient() {
        setUp(1, MoveEffect.Hit.of(30));
        state.playerStats().add(StatModifier.flat(StatType.DEFENSE, 100, "test")); // Sassy tient le coup
        alien.setTargeted(true);
        endTurn();
        assertTrue(alien.isKnockedOut());
        assertTrue(events.contains(new GameEvent.FamiliarKnockedOut(alien)));
        assertFalse(state.familiarTargeted(), "assommé : plus visé");
        events.clear();
        for (int i = 0; i < 5; i++) {
            engine.startTurn();
            endTurn();
        }
        assertFalse(events.stream().anyMatch(e -> e instanceof GameEvent.FamiliarActed), "il n'agit plus");
        assertEquals(500, state.currentEnemy().hp(), "il n'attaque plus");
        state.currentEnemy().takeDamage(1000); // A meurt : combat suivant au prochain tour
        engine.startTurn();
        assertEquals("B", state.currentEnemy().name());
        assertEquals(20, alien.hp(), "tous ses PV au combat suivant");
        assertFalse(alien.isKnockedOut());
    }

    @Test
    void ilAttaqueOuFumeUnJointAChaqueTour() {
        boolean attacked = false;
        boolean smoked = false;
        for (long seed = 0; seed < 60; seed++) {
            setUp(seed, MoveEffect.Hit.of(0));
            events.clear();
            engine.startTurn();
            attacked |= events.contains(new GameEvent.FamiliarActed(alien, GameEvent.FamiliarActed.Action.ATTACK));
            smoked |= events.contains(new GameEvent.FamiliarActed(alien, GameEvent.FamiliarActed.Action.SMOKE));
        }
        assertTrue(attacked && smoked);
    }
}

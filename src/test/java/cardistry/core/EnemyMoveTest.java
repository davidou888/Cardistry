package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.NEUTRAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.items.ItemRegistry;
import cardistry.items.LootTable;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.DeadCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import cardistry.model.enemy.MoveEffect;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Attaques adverses génériques : intents, effets, annulation, esquive, renvoi. */
class EnemyMoveTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    private static final EnemyMove NORMAL = EnemyMove.of("normal", "Normal", "Normal", MoveEffect.Hit.of(5));
    private static final EnemyMove SPECIAL = EnemyMove.of("special", "Spécial", "Spécial", MoveEffect.Hit.of(10));

    /** Adversaire neutre (terrain sans effet) de 100 PV, main de {@code hand}. */
    private void setUp(EnemyBehavior behavior, List<Card> hand) {
        setUp(Enemy.builder("cible", "Cible", NEUTRAL, EnemyRank.COMMON).hp(100).behavior(behavior), hand);
    }

    private void setUp(Enemy.Builder builder, List<Card> hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = builder.build();
        state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(List.of(new TerrainCard("Pioche", FIRE, cardistry.items.Rarity.COMMON))), new Rng(3), bus,
                new LootTable(List::of));
    }

    private static EnemyBehavior always(MoveEffect... effects) {
        return EnemyBehavior.always(EnemyMove.of("coup", "Coup", "Coup", effects));
    }

    /** Fin de tour sans rien changer à la main : rouler une clope. */
    private void endTurn() {
        assertEquals(ActionResult.TURN_ENDED, engine.submit(new Action.RollCigarette()));
    }

    private void giveMoscowMule() {
        engine.give(ItemRegistry.item("moscow-mule"));
        engine.submit(new Action.UseItem(0));
    }

    // --- Intents ---

    @Test
    void intentAnnonceAuDebutPuisApresChaqueAttaque() {
        setUp(EnemyBehavior.every(3, SPECIAL, NORMAL), List.of());
        engine.start();
        assertSame(NORMAL, enemy.intent());
        assertTrue(events.contains(new GameEvent.IntentChosen(enemy, NORMAL)));

        endTurn();
        assertEquals(95, state.player().hp(), DELTA);
        assertSame(NORMAL, enemy.intent());
        endTurn();
        assertSame(SPECIAL, enemy.intent()); // 3e attaque
        endTurn();
        assertEquals(100 - 5 - 5 - 10, state.player().hp(), DELTA);
        assertSame(NORMAL, enemy.intent());
        assertEquals(3, enemy.actions());
        assertEquals(1, enemy.uses("special"));
    }

    @Test
    void attaqueUtiliseePublieeAvantSesEffets() {
        setUp(EnemyBehavior.always(SPECIAL), List.of());
        engine.start();
        events.clear();
        endTurn();
        int used = events.indexOf(new GameEvent.EnemyMoveUsed(enemy, SPECIAL));
        assertTrue(used >= 0);
        assertInstanceOf(GameEvent.EnemyAttacked.class, events.get(used + 1));
    }

    @Test
    void niveauEtTerrainMultiplientLesDegats() {
        setUp(Enemy.builder("feu", "Feu", FIRE, EnemyRank.COMMON).hp(50).scale(1.4, 1.5)
                .behavior(EnemyBehavior.always(SPECIAL)), List.of());
        assertEquals(70, enemy.maxHp(), DELTA);
        endTurn();
        // 10 × 1.5 (niveau) × 1 (Forêt : nature, ni pour ni contre le feu en attaque)
        assertEquals(85, state.player().hp(), DELTA);
    }

    @Test
    void neutreIgnoreLeTerrain() {
        setUp(EnemyBehavior.always(SPECIAL), List.of());
        assertEquals(1.0, state.enemyAttackMultiplier(), DELTA);
    }

    // --- Coups ---

    @Test
    void plusieursCoupsEtReductionParCoup() {
        setUp(always(MoveEffect.Hit.of(4, 2)), List.of());
        state.playerStats().add(cardistry.core.stats.StatModifier.flat(
                cardistry.core.stats.StatType.DEFENSE, 1, "test"));
        endTurn();
        assertEquals(100 - 3 - 3, state.player().hp(), DELTA);
        assertEquals(2, events.stream().filter(e -> e instanceof GameEvent.EnemyAttacked).count());
    }

    @Test
    void moscowMuleAnnuleTousLesCoups() {
        setUp(always(MoveEffect.Hit.of(4, 3)), List.of());
        giveMoscowMule();
        endTurn();
        assertEquals(100, state.player().hp(), DELTA);
        assertEquals(1, events.stream().filter(e -> e instanceof GameEvent.AttackCancelled).count());
        assertTrue(state.effects().isEmpty());
    }

    @Test
    void degatsSelonLesCartesQuiManquent() {
        // 6 + 3 par carte sous 5 : main de 2 cartes → 6 + 9
        setUp(always(new MoveEffect.Hit(6, 1, 3, 0, 0)),
                List.of(new TerrainCard("A", FIRE, cardistry.items.Rarity.COMMON), new TerrainCard("B", FIRE, cardistry.items.Rarity.COMMON)));
        assertEquals(15, state.enemyAttackDamage(), DELTA);
        endTurn();
        assertEquals(85, state.player().hp(), DELTA);
    }

    @Test
    void coupRateSoigneLAdversaire() {
        setUp(always(new MoveEffect.Hit(13, 1, 0, 1.0, 8)), List.of());
        enemy.takeDamage(20);
        endTurn();
        assertEquals(100, state.player().hp(), DELTA);
        assertEquals(88, enemy.hp(), DELTA);
        assertTrue(events.contains(new GameEvent.EnemyMissed(enemy, 8)));
    }

    // --- Vols ---

    @Test
    void volPrendDesChoomiesEnPriorite() {
        setUp(always(new MoveEffect.Pickpocket(5, 1, 4)), List.of());
        assertEquals(GameState.STARTING_CHOOMIES, state.choomies());
        endTurn();
        assertEquals(GameState.STARTING_CHOOMIES - 5, state.choomies());
        assertEquals(GameState.STARTING_CIGARETTES + 1, state.cigarettes()); // roulée, pas volée
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.ChoomiesStolen(enemy, 5)));
    }

    @Test
    void volSansChoomiesPrendUneClopePuisFrappe() {
        setUp(always(new MoveEffect.Pickpocket(50, 1, 4)), List.of());
        endTurn();
        assertEquals(GameState.STARTING_CIGARETTES, state.cigarettes()); // +1 roulée, −1 volée
        assertEquals(GameState.STARTING_CHOOMIES, state.choomies());

        engine.submit(new Action.DrawCard()); // pioche : ne change pas les clopes
        assertEquals(GameState.STARTING_CIGARETTES - 1, state.cigarettes());
        while (state.cigarettes() > 0) {
            engine.submit(new Action.Smoke());
        }
        double hp = state.player().hp();
        engine.submit(new Action.DrawCard());
        assertEquals(hp - 4, state.player().hp(), DELTA);
    }

    @Test
    void volPasAnnuleParMoscowMule() {
        setUp(always(new MoveEffect.Pickpocket(5, 1, 4)), List.of());
        giveMoscowMule();
        endTurn();
        assertEquals(GameState.STARTING_CHOOMIES - 5, state.choomies());
        assertFalse(state.effects().isEmpty()); // le Moscow Mule reste pour une vraie attaque
    }

    @Test
    void taxePrendUneClopeSinonDeuxCoups() {
        setUp(always(new MoveEffect.Tax(1, 5, 2)), List.of(new TerrainCard("A", FIRE, cardistry.items.Rarity.COMMON)));
        engine.submit(new Action.PlayCard(0));
        assertEquals(GameState.STARTING_CIGARETTES - 1, state.cigarettes());
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.CigarettesStolen(enemy, 1)));

        while (state.cigarettes() > 0) {
            engine.submit(new Action.Smoke());
        }
        double hp = state.player().hp();
        engine.submit(new Action.DrawCard());
        assertEquals(hp - 10, state.player().hp(), DELTA);
    }

    // --- Esquive, carte morte, soin ---

    @Test
    void esquiveLeProchainSort() {
        setUp(always(new MoveEffect.Gamble(1.0, new MoveEffect.Dodge(), MoveEffect.Hit.of(2, 2))),
                List.of(new SpellCard("Boule", FIRE, 10, cardistry.items.Rarity.COMMON), new SpellCard("Boule", FIRE, 10, cardistry.items.Rarity.COMMON)));
        endTurn();
        assertTrue(enemy.dodgeReady());
        engine.submit(new Action.PlayCard(0));
        assertEquals(100, enemy.hp(), DELTA);
        assertTrue(events.contains(new GameEvent.EnemyDodged(enemy)));
        assertTrue(enemy.dodgeReady()); // il s'est remis en garde à son tour
    }

    @Test
    void gambleRateDonneLAutreEffet() {
        setUp(always(new MoveEffect.Gamble(0.0, new MoveEffect.Dodge(), MoveEffect.Hit.of(2, 2))), List.of());
        endTurn();
        assertFalse(enemy.dodgeReady());
        assertEquals(96, state.player().hp(), DELTA);
    }

    @Test
    void carteMaudite() {
        setUp(always(new MoveEffect.CurseCard("Exercice 3b")),
                List.of(new TerrainCard("A", FIRE, cardistry.items.Rarity.COMMON), new TerrainCard("B", FIRE, cardistry.items.Rarity.COMMON)));
        endTurn();
        long dead = state.hand().stream().filter(c -> c instanceof DeadCard).count();
        assertEquals(1, dead);
        Card cursed = state.hand().stream().filter(c -> c instanceof DeadCard).findFirst().orElseThrow();
        assertEquals("Exercice 3b", cursed.name());
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.PlayCard(state.hand().indexOf(cursed))));
    }

    @Test
    void soinPlafonneAuxPvMax() {
        setUp(always(new MoveEffect.HealSelf(12)), List.of());
        enemy.takeDamage(5);
        endTurn();
        assertEquals(100, enemy.hp(), DELTA);
        assertTrue(events.contains(new GameEvent.EnemyHealed(enemy, 5)));
    }

    @Test
    void renvoiDeDegatsSansJamaisTuer() {
        setUp(Enemy.builder("peneu", "Peneu", NEUTRAL, EnemyRank.ELITE).hp(500).reflect(0.2)
                .behavior(EnemyBehavior.always(NORMAL)), List.of(new SpellCard("Gros", FIRE, 100, cardistry.items.Rarity.COMMON),
                new SpellCard("Gros", FIRE, 100, cardistry.items.Rarity.COMMON)));
        engine.submit(new Action.PlayCard(0));
        assertEquals(400, enemy.hp(), DELTA);
        assertEquals(100 - 20 - 5, state.player().hp(), DELTA);
        state.player().takeDamage(70); // 5 PV
        engine.submit(new Action.PlayCard(0));
        // le renvoi (20) est limité à 4 : il laisse 1 PV ; l'attaque normale (5) tue ensuite
        assertTrue(events.contains(new GameEvent.DamageDealt(state.player(), 4, 0.2,
                GameEvent.DamageDealt.Source.REFLECTED)));
        assertTrue(state.isLost());
    }

    @Test
    void tourSauteGardeLIntent() {
        setUp(EnemyBehavior.every(2, SPECIAL, NORMAL), List.of());
        engine.start();
        engine.give(new cardistry.items.Item("pause", "Pause", "x", "", cardistry.items.Rarity.COMMON,
                java.util.Set.of(), ctx -> ctx.addEffect(TemporaryEffect.builder("pause", "Pause")
                        .skipsNextEnemyTurn("Cible passe son tour").charges(1).build())));
        engine.submit(new Action.UseItem(0));
        endTurn();
        assertEquals(100, state.player().hp(), DELTA);
        assertEquals(0, enemy.actions());
        assertSame(NORMAL, enemy.intent());
    }
}

package cardistry.core.status;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Système de statuts générique, testé avec l'Intox. */
class StatusTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    /** Adversaire neutre de {@code hp} PV qui frappe de {@code attack} ; main : un sort de 1 dégât. */
    private void setUp(double hp, double attack, Stats stats) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", Element.NEUTRAL, hp, attack);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 50, 1)),
                new TerrainCard("Dojo", Element.NEUTRAL, Rarity.COMMON),
                List.of(new SpellCard("Pichenette", Element.NEUTRAL, 1, Rarity.COMMON)), stats);
        engine = new CombatEngine(state, new CardPool(), new Rng(3), bus, new LootTable(List::of));
    }

    private void setUp(double hp, double attack) {
        setUp(hp, attack, new Stats());
    }

    /** Utilise un objet qui inflige {@code type} à {@code target} avec la chance donnée. */
    private boolean inflict(StatusTarget target, StatusType type, double chance) {
        boolean[] landed = new boolean[1];
        engine.give(new Item("test", "Test", "", "", Rarity.COMMON, Set.of(),
                ctx -> landed[0] = ctx.applyStatus(target, type, chance)));
        engine.submit(new Action.UseItem(state.items().size() - 1));
        return landed[0];
    }

    private void drawPass() {
        engine.submit(new Action.DrawCard());
    }

    @Test
    void intoxSurLAdversaireMoinsCinqAuDebutDeSonTourPendantTroisTours() {
        setUp(100, 0);
        engine.startTurn();
        assertTrue(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        assertEquals(3, state.statuses(StatusTarget.ENEMY).get(StatusType.INTOX).orElseThrow().turnsLeft());
        assertEquals(100, enemy.hp(), "rien avant son tour");
        for (int turn = 1; turn <= 3; turn++) {
            drawPass();
            assertEquals(100 - 5 * turn, enemy.hp());
            engine.startTurn();
        }
        assertFalse(state.statuses(StatusTarget.ENEMY).has(StatusType.INTOX));
        drawPass();
        assertEquals(85, enemy.hp(), "trois tours seulement");
        assertTrue(events.contains(new GameEvent.StatusEnded(StatusTarget.ENEMY, StatusType.INTOX)));
        assertTrue(events.contains(new GameEvent.DamageDealt(enemy, 5, 1.0, GameEvent.DamageDealt.Source.STATUS)));
    }

    @Test
    void intoxNeSeReappliquePasTantQuIlEstActif() {
        setUp(100, 0);
        engine.startTurn();
        assertTrue(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        drawPass(); // 1er tour d'Intox : il en reste 2
        engine.startTurn();
        assertFalse(inflict(StatusTarget.ENEMY, StatusType.INTOX, 1));
        assertEquals(2, state.statuses(StatusTarget.ENEMY).get(StatusType.INTOX).orElseThrow().turnsLeft());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.StatusRefused r
                && r.type() == StatusType.INTOX));
    }

    @Test
    void intoxSurLeJoueurAuDebutDeSonTourEtDegatsBruts() {
        Stats stats = new Stats();
        stats.add(StatModifier.flat(StatType.DAMAGE_REDUCTION, 3, "test")); // ne réduit pas un statut
        setUp(100, 0, stats);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        assertEquals(100, state.player().hp());
        drawPass();
        engine.startTurn();
        assertEquals(95, state.player().hp());
    }

    @Test
    void lIntoxPeutTuerLAdversaireEtLeCombatSuivantCommence() {
        setUp(5, 0);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.INTOX, 1);
        drawPass();
        assertFalse(enemy.isAlive());
        assertTrue(events.contains(new GameEvent.EnemyKilled(enemy)));
        assertTrue(state.statuses(StatusTarget.ENEMY).isEmpty(), "statuts retirés en fin de combat");
    }

    @Test
    void lIntoxPeutTuerLeJoueur() {
        setUp(100, 0);
        state.player().takeDamage(97);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        drawPass();
        engine.startTurn();
        assertTrue(state.isLost());
        assertTrue(events.contains(new GameEvent.GameLost()));
    }

    @Test
    void lesStatutsDuJoueurSArretentALaFinDuCombat() {
        setUp(1, 0);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        engine.submit(new Action.PlayCard(0)); // tue l'adversaire (1 PV)
        assertTrue(state.statuses(StatusTarget.PLAYER).isEmpty());
        assertTrue(events.contains(new GameEvent.StatusEnded(StatusTarget.PLAYER, StatusType.INTOX)));
    }

    @Test
    void chanceDuJoueurDoubleeParClaudeMdMaisPasCelleSubie() {
        // Avec CLAUDE.md, un statut infligé à l'adversaire tente deux fois ; subi par le joueur, une fois.
        int landedOnEnemy = 0;
        int landedOnPlayer = 0;
        for (int seed = 0; seed < 400; seed++) {
            Stats stats = new Stats();
            stats.add(StatModifier.flat(StatType.LUCKY_ROLLS, 1, "claude"));
            EventBus bus = new EventBus();
            Enemy target = new Enemy("Cible", Element.NEUTRAL, 100, 0);
            state = new GameState(new Player(100), List.of(target), new TerrainCard("Dojo", Element.NEUTRAL,
                    Rarity.COMMON), List.of(), stats);
            engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
            engine.startTurn();
            landedOnEnemy += inflict(StatusTarget.ENEMY, StatusType.INTOX, 0.5) ? 1 : 0;
            landedOnPlayer += inflict(StatusTarget.PLAYER, StatusType.INTOX, 0.5) ? 1 : 0;
        }
        assertTrue(landedOnEnemy > 260 && landedOnEnemy < 340, "≈ 75 % : " + landedOnEnemy);
        assertTrue(landedOnPlayer > 160 && landedOnPlayer < 240, "≈ 50 % : " + landedOnPlayer);
    }

    // --- Fons, Hypnotisé, Bondage, Brûlure ---

    private void setTerrain(Element element) {
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 50, 1)),
                new TerrainCard("T", element, Rarity.COMMON), List.of(), new Stats());
    }

    /** Partie sur un terrain {@code element}, adversaire neutre {@code hp} PV qui frappe de {@code attack}. */
    private void setUpOn(Element element, double hp, double attack, List<cardistry.model.card.Card> hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", Element.NEUTRAL, hp, attack);
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 50, 1)),
                new TerrainCard("T", element, Rarity.COMMON), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(3), bus, new LootTable(List::of));
    }

    private boolean inflict(StatusTarget target, StatusType type, int turns, StatusExtras extras) {
        boolean[] landed = new boolean[1];
        engine.give(new Item("test", "Test", "", "", Rarity.COMMON, Set.of(),
                ctx -> landed[0] = ctx.applyStatus(target, type, 1, turns, extras)));
        engine.submit(new Action.UseItem(state.items().size() - 1));
        return landed[0];
    }

    @Test
    void fonsLAdversaireNAttaquePasPendantSaDureePuisSeReveille() {
        setUp(100, 10);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.FONS, 2, StatusExtras.NONE);
        drawPass();
        engine.startTurn();
        drawPass();
        assertEquals(100, state.player().hp(), "deux tours de sommeil");
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.EnemyTurnSkipped s
                && s.message().contains("dort")));
        engine.startTurn();
        drawPass();
        assertEquals(90, state.player().hp(), "réveillé");
    }

    @Test
    void fonsDureeTireeEntreUnEtTroisTours() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int seed = 0; seed < 60; seed++) {
            EventBus bus = new EventBus();
            enemy = new Enemy("Cible", Element.NEUTRAL, 100, 0);
            state = new GameState(new Player(100), List.of(enemy), new TerrainCard("Dojo", Element.NEUTRAL,
                    Rarity.COMMON), List.of(), new Stats());
            engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
            engine.startTurn();
            inflict(StatusTarget.ENEMY, StatusType.FONS, 1);
            seen.add(state.statuses(StatusTarget.ENEMY).get(StatusType.FONS).orElseThrow().turnsLeft());
        }
        assertEquals(java.util.Set.of(1, 2, 3), seen);
    }

    @Test
    void fonsReappliqueeDureeRemiseAuMax() {
        setUp(100, 0);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.FONS, 3, StatusExtras.NONE);
        drawPass();
        engine.startTurn(); // il reste 2 tours
        assertEquals(2, state.statuses(StatusTarget.ENEMY).get(StatusType.FONS).orElseThrow().turnsLeft());
        inflict(StatusTarget.ENEMY, StatusType.FONS, 3, StatusExtras.NONE);
        assertEquals(3, state.statuses(StatusTarget.ENEMY).get(StatusType.FONS).orElseThrow().turnsLeft());
    }

    @Test
    void fonsLeJoueurPasseSonTourEtLAdversaireAttaque() {
        setUp(100, 10);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.FONS, 1, StatusExtras.NONE); // posé pendant son propre tour
        drawPass();
        assertEquals(90, state.player().hp());
        assertFalse(engine.startTurn(), "tour sauté");
        assertEquals(80, state.player().hp(), "l'adversaire attaque quand même");
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.PlayerTurnSkipped s
                && s.causes().contains("Fons")));
        assertTrue(engine.startTurn(), "réveillé au tour suivant");
    }

    @Test
    void fonsAvecSoinParTourEtImmunite() {
        setUp(100, 10);
        state.player().takeDamage(50);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.FONS, 2, new StatusExtras(0.08, true));
        inflict(StatusTarget.PLAYER, StatusType.INTOX, 1);
        drawPass(); // immunisé : pas de coup
        assertEquals(50, state.player().hp());
        engine.startTurn(); // Intox −5, Fons +8 % de 100, tour sauté, coup annulé
        assertEquals(50 - 5 + 8, state.player().hp());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.AttackCancelled));
        engine.startTurn(); // dernier tour de Fons : décompté à sa fin, l'attaque suivante passe
        assertEquals(53 - 5 + 8 - 10, state.player().hp());
    }

    @Test
    void hypnotiseLAdversaireLesSortsFontX2JusquAuProchainTourDuJoueur() {
        setUpOn(Element.NEUTRAL, 100, 0, List.of(
                new SpellCard("A", Element.NEUTRAL, 10, Rarity.COMMON),
                new SpellCard("B", Element.NEUTRAL, 10, Rarity.COMMON),
                new SpellCard("C", Element.NEUTRAL, 10, Rarity.COMMON)));
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.HYPNOTIZED, 1, StatusExtras.NONE);
        assertEquals(2.0, state.spellMultiplier((SpellCard) state.hand().get(0)), 1e-9);
        engine.submit(new Action.PlayCard(0));
        assertEquals(80, enemy.hp());
        engine.startTurn(); // ne décompte pas à la fin du tour où il a été posé
        engine.submit(new Action.PlayCard(0));
        assertEquals(60, enemy.hp());
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(50, enemy.hp(), "terminé");
    }

    @Test
    void hypnotiseLeJoueurLesAttaquesAdversesFontX2() {
        setUp(100, 10);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.HYPNOTIZED, 1, StatusExtras.NONE);
        drawPass();
        assertEquals(80, state.player().hp());
    }

    @Test
    void bondageVoleCinqPvParTourAuProfitDeCeluiQuiLInflige() {
        setUpOn(Element.NEUTRAL, 100, 0, List.of());
        state.player().takeDamage(50);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.BONDAGE, 1);
        drawPass();
        assertEquals(95, enemy.hp());
        assertEquals(55, state.player().hp());
    }

    @Test
    void bondageHuitPvSurUnTerrainPlante() {
        setUpOn(Element.NATURE, 100, 0, List.of());
        state.player().takeDamage(50);
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.BONDAGE, 1);
        drawPass();
        assertEquals(92, enemy.hp());
        assertEquals(58, state.player().hp());
    }

    @Test
    void bondageSubiParLeJoueurSoigneLAdversaire() {
        setUpOn(Element.NEUTRAL, 100, 0, List.of());
        enemy.takeDamage(20);
        engine.startTurn();
        inflict(StatusTarget.PLAYER, StatusType.BONDAGE, 3, StatusExtras.NONE);
        drawPass();
        engine.startTurn();
        assertEquals(95, state.player().hp());
        assertEquals(85, enemy.hp());
    }

    @Test
    void brulureHuitSurTerrainFeuQuatreAilleurs() {
        setUpOn(Element.FIRE, 100, 0, List.of());
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.BURN, 1);
        drawPass();
        assertEquals(92, enemy.hp());
        setUpOn(Element.NATURE, 100, 0, List.of());
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.BURN, 1);
        drawPass();
        assertEquals(96, enemy.hp());
    }

    @Test
    void brulureImpossibleSurUnTerrainEau() {
        setUpOn(Element.WATER, 100, 0, List.of());
        engine.startTurn();
        assertFalse(inflict(StatusTarget.ENEMY, StatusType.BURN, 1));
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.StatusRefused r
                && r.type() == StatusType.BURN));
    }

    @Test
    void unTerrainEauEteintLesBrulures() {
        setUpOn(Element.FIRE, 100, 0, List.of(new TerrainCard("Marécage", Element.WATER, Rarity.COMMON)));
        engine.startTurn();
        inflict(StatusTarget.ENEMY, StatusType.BURN, 1);
        inflict(StatusTarget.PLAYER, StatusType.BURN, 1);
        engine.submit(new Action.PlayCard(0));
        assertTrue(state.statuses(StatusTarget.ENEMY).isEmpty());
        assertTrue(state.statuses(StatusTarget.PLAYER).isEmpty());
        assertEquals(100, enemy.hp());
    }
}

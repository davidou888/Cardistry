package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.TemporaryEffect;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Objets et trinkets à effets temporaires (étape 4), et leurs combinaisons. */
class EffectContentTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy first;

    /** Adversaire feu 100 PV / 10 d'attaque, terrain Forêt (attaque ×1), puis un second adversaire. */
    private void setUp(List<Card> hand) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        first = new Enemy("Mannequin", FIRE, 100, 10);
        state = new GameState(new Player(100), List.of(first, new Enemy("Bouée", WATER, 100, 10)),
                new TerrainCard("Forêt", NATURE), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private ActionResult use(String id) {
        engine.give(ItemRegistry.item(id));
        return engine.submit(new Action.UseItem(state.items().size() - 1));
    }

    private List<String> effectIds() {
        return state.effects().stream().map(TemporaryEffect::id).toList();
    }

    // --- Shot de Jäger ---

    @Test
    void shotDeJagerProchainSortX2EtUnPointDIvresse() {
        setUp(List.of(new SpellCard("Sort", FIRE, 20)));
        assertEquals(ActionResult.FREE_ACTION, use("shot-de-jager"));
        assertEquals(1, state.drunkenness());
        assertEquals("Shot de Jäger : prochain sort ×2", state.effects().get(0).describe());

        engine.submit(new Action.PlayCard(0));
        assertEquals(60, first.hp(), DELTA);
        assertTrue(state.effects().isEmpty());
    }

    @Test
    void shotDeJagerPerduALaFinDuCombat() {
        setUp(List.of(new SpellCard("Coup fatal", WATER, 1000)));
        use("shot-de-jager");
        first.takeDamage(99);
        engine.give(ItemRegistry.item("gaz-du-bat-inf")); // tue l'adversaire sans jouer de sort
        engine.submit(new Action.UseItem(0));
        assertEquals(2, state.combatNumber());
        assertTrue(state.effects().isEmpty());
    }

    @Test
    void shotDeJagerEtPtitSat() {
        setUp(List.of(new SpellCard("Sort", FIRE, 20)));
        engine.equip(ItemRegistry.trinket("ptit-sat"));
        use("shot-de-jager"); // Ivresse 1 → +10 %
        engine.submit(new Action.PlayCard(0));
        // 20 × 1.1 (Ptit Sat ?) × 2 (Shot de Jäger)
        assertEquals(100 - 44, first.hp(), DELTA);
    }

    // --- Moscow Mule ---

    @Test
    void moscowMuleAnnuleLaProchaineAttaque() {
        setUp(List.of());
        use("moscow-mule");
        assertEquals(1, state.drunkenness());

        engine.submit(new Action.DrawCard());
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.AttackCancelled(first, "Moscow Mule")));

        engine.submit(new Action.DrawCard());
        assertEquals(90, state.player().hp(), DELTA);
    }

    @Test
    void moscowMulePerduALaFinDuCombatSIlNAPasServi() {
        setUp(List.of(new SpellCard("Coup fatal", WATER, 1000)));
        use("moscow-mule");
        engine.submit(new Action.PlayCard(0)); // tue le Mannequin : pas d'attaque, combat suivant
        assertTrue(state.effects().isEmpty());
        engine.submit(new Action.DrawCard());
        // La Bouée (eau) attaque bien : sur Forêt (nature bat eau), 10 × 0.75.
        assertEquals(100 - 7.5, state.player().hp(), DELTA);
    }

    // --- Raclette ---

    @Test
    void racletteCinquantePvPuisTourSaute() {
        setUp(List.of());
        state.player().takeDamage(60);
        engine.startTurn();
        use("raclette");
        assertEquals(90, state.player().hp(), DELTA);
        engine.submit(new Action.DrawCard());           // fin du tour 1 : attaque −10
        assertEquals(80, state.player().hp(), DELTA);

        assertFalse(engine.startTurn());                // tour 2 sauté, l'adversaire attaque
        assertEquals(70, state.player().hp(), DELTA);
        assertTrue(engine.startTurn());                 // tour 3 joué
    }

    // --- Space cake ---

    @Test
    void spaceCakeAuBoutDeTroisToursPendantDeuxTours() {
        setUp(List.of(new SpellCard("Sort", FIRE, 20), new SpellCard("Sort", FIRE, 20), new SpellCard("Sort", FIRE, 20)));
        state.player().takeDamage(60);
        engine.startTurn();                         // tour 1 (T)
        use("space-cake");
        assertEquals("Space cake : dans 3 tours", state.effects().get(0).describe());

        engine.startTurn();                         // T+1
        engine.startTurn();                         // T+2
        assertEquals(40, state.player().hp(), DELTA);
        engine.startTurn();                         // T+3 : +40 PV, +50 %
        assertEquals(80, state.player().hp(), DELTA);
        assertEquals("Space cake : +50 % dégâts, 2 tours", state.effects().get(0).describe());
        engine.submit(new Action.PlayCard(0));
        assertEquals(70, first.hp(), DELTA);        // 20 × 1.5

        engine.startTurn();                         // T+4 : toujours actif
        engine.submit(new Action.PlayCard(0));
        assertEquals(40, first.hp(), DELTA);

        engine.startTurn();                         // T+5 : terminé
        engine.submit(new Action.PlayCard(0));
        assertEquals(20, first.hp(), DELTA);
        assertTrue(state.effects().isEmpty());
    }

    @Test
    void spaceCakeReporteAuCombatSuivant() {
        setUp(List.of());
        state.player().takeDamage(60);
        engine.startTurn();                         // combat 1, tour 1
        use("space-cake");
        engine.startTurn();                         // combat 1, tour 2 : dans 2 tours
        first.takeDamage(1000);
        engine.submit(new Action.DrawCard());       // fin du combat 1
        assertEquals(2, state.combatNumber());
        assertEquals("Space cake : dans 2 tours", state.effects().get(0).describe());

        engine.startTurn();                         // combat 2, tour 1 : dans 1 tour
        assertFalse(state.effects().get(0).isActive());
        engine.startTurn();                         // combat 2, tour 2 : déclenchement
        assertTrue(state.effects().get(0).isActive());
        assertEquals(1.5, state.playerStats().get(StatType.WATER_DAMAGE), DELTA);
    }

    // --- Pétou ancestral ---

    /** Équipe Pétou ancestral puis passe au combat suivant (son effet se pose en début de combat). */
    private void equipPetouThenNextCombat() {
        engine.equip(ItemRegistry.trinket("petou-ancestral"));
        first.takeDamage(1000);
        engine.submit(new Action.DrawCard());
        assertEquals(2, state.combatNumber());
    }

    @Test
    void petouAncestralHuitPvAChaqueCombatGagne() {
        setUp(List.of());
        state.player().takeDamage(50);
        engine.equip(ItemRegistry.trinket("petou-ancestral"));
        first.takeDamage(1000);
        engine.submit(new Action.DrawCard());
        // Soin feu sur Forêt (le feu bat la nature : ×1)
        assertEquals(58, state.player().hp(), DELTA);
        assertTrue(events.contains(new GameEvent.Healed(state.player(), 8, 1.0)));
    }

    @Test
    void petouAncestralPasDeSoinEnCasDeDefaite() {
        setUp(List.of());
        engine.equip(ItemRegistry.trinket("petou-ancestral"));
        state.player().takeDamage(95);
        engine.submit(new Action.DrawCard()); // le Mannequin attaque : défaite
        assertTrue(state.isLost());
        assertFalse(events.stream().anyMatch(GameEvent.Healed.class::isInstance));
    }

    @Test
    void petouAncestralSoinFeuBoosteParGrinder() {
        setUp(List.of());
        state.player().takeDamage(50);
        engine.equip(ItemRegistry.trinket("petou-ancestral"));
        engine.equip(ItemRegistry.trinket("grinder"));
        first.takeDamage(1000);
        engine.submit(new Action.DrawCard());
        assertEquals(50 + 12, state.player().hp(), DELTA); // 8 × 1.5
    }

    @Test
    void petouAncestralLAdversaireSauteSonPremierTour() {
        setUp(List.of());
        equipPetouThenNextCombat();
        assertEquals("Pétou ancestral : l'adversaire saute son premier tour", state.effects().get(0).describe());
        events.clear();

        engine.submit(new Action.DrawCard());
        assertEquals(new GameEvent.EnemyTurnSkipped(state.currentEnemy(), "Pétou ancestral",
                "Bouée passe son tour : tu as fait tourner le j."),
                events.stream().filter(GameEvent.EnemyTurnSkipped.class::isInstance).findFirst().orElseThrow());
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(state.effects().isEmpty());

        engine.submit(new Action.DrawCard()); // la Bouée attaque ensuite normalement (Forêt : ×0.75)
        assertEquals(100 - 7.5, state.player().hp(), DELTA);
    }

    @Test
    void petouAncestralEtMoscowMuleLeMuleNEstPasGaspille() {
        setUp(List.of());
        equipPetouThenNextCombat();
        use("moscow-mule");

        // Rouler une clope termine le tour sans remplir la main.
        engine.submit(new Action.RollCigarette());     // tour sauté par Pétou ancestral
        assertEquals(List.of("moscow-mule"), effectIds());
        engine.submit(new Action.RollCigarette());     // le Mule annule cette attaque-ci
        assertEquals(100, state.player().hp(), DELTA);
        assertTrue(state.effects().isEmpty());
        engine.submit(new Action.RollCigarette());
        assertEquals(100 - 7.5, state.player().hp(), DELTA);
    }
}

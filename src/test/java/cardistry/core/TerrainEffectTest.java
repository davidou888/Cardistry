package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyBehavior;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.EnemyRank;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Effets des terrains du joueur : bonus tant qu'ils sont en jeu, Migros, Gare. */
class TerrainEffectTest {

    private GameState state;
    private CombatEngine engine;
    private Enemy enemy;

    private static TerrainCard terrain(String name) {
        return (TerrainCard) new CardPool().cards().stream().filter(c -> c.name().equals(name)).findFirst()
                .orElseThrow().copy();
    }

    private void setUp(TerrainCard start, double enemyAttack, Card... hand) {
        enemy = Enemy.builder("cible", "Cible", Element.NEUTRAL, EnemyRank.COMMON).hp(1000).choomies(40)
                .behavior(EnemyBehavior.always(EnemyMove.basicAttack(enemyAttack))).build();
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", Element.FIRE, 50, 0)), start,
                List.of(hand), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(5), new EventBus(), new LootTable(List::of));
        engine.startTurn();
    }

    private static SpellCard spell(Element element, double damage) {
        return new SpellCard("Sort", element, damage, Rarity.COMMON);
    }

    @Test
    void dojoDixPourcentDeDegatsTousElements() {
        setUp(CardPool.startingTerrain(), 0, spell(Element.NEUTRAL, 10), spell(Element.ALGEBRAIC, 20));
        assertEquals("Dojo", state.terrain().name());
        engine.submit(new Action.PlayCard(0));
        assertEquals(1000 - 11, enemy.hp());
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(1000 - 11 - 22, enemy.hp());
    }

    @Test
    void amazonieEnferAtlantisVingtPourcentASonElement() {
        assertEquals(1.2, bonusOn("Amazonie", StatType.NATURE_DAMAGE), 1e-9);
        assertEquals(1.0, bonusOn("Amazonie", StatType.FIRE_DAMAGE), 1e-9);
        assertEquals(1.2, bonusOn("Enfer", StatType.FIRE_DAMAGE), 1e-9);
        assertEquals(1.2, bonusOn("Atlantis", StatType.WATER_DAMAGE), 1e-9);
        assertEquals(1.0, bonusOn("Atlantis", StatType.NEUTRAL_DAMAGE), 1e-9);
    }

    private double bonusOn(String terrain, StatType stat) {
        setUp(terrain(terrain), 0);
        return state.playerStats().get(stat);
    }

    @Test
    void bonusRetiresQuandLeTerrainChangePuisEnFinDeCombat() {
        setUp(CardPool.startingTerrain(), 0, terrain("Amazonie"), spell(Element.NEUTRAL, 1000));
        assertEquals(1.1, state.playerStats().get(StatType.NATURE_DAMAGE), 1e-9);
        engine.submit(new Action.PlayCard(0)); // Dojo → Amazonie
        assertEquals(1.2, state.playerStats().get(StatType.NATURE_DAMAGE), 1e-9, "le Dojo est retiré");
        assertEquals(1.0, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9);
        engine.startTurn();
        engine.submit(new Action.PlayCard(0)); // tue l'adversaire : fin du combat
        assertEquals("Suivant", state.currentEnemy().name());
        assertEquals(1.2, state.playerStats().get(StatType.NATURE_DAMAGE), 1e-9,
                "nouveau combat sur Amazonie : ses bonus reviennent");
    }

    @Test
    void migrosVingtCinqPourcentDeChoomiesSiLeCombatSyTermine() {
        setUp(terrain("Migros"), 0, spell(Element.NEUTRAL, 1000));
        int before = state.choomies();
        engine.submit(new Action.PlayCard(0));
        assertEquals(50, state.choomies() - before, "40 × 1.25");
    }

    @Test
    void migrosQuitteAvantLaFinPasDeBonus() {
        setUp(terrain("Migros"), 0, terrain("Forêt"), spell(Element.NEUTRAL, 1000));
        engine.submit(new Action.PlayCard(0));
        engine.startTurn();
        int before = state.choomies();
        engine.submit(new Action.PlayCard(0));
        assertEquals(40, state.choomies() - before);
    }

    @Test
    void gareChangerDeTerrainNeCoutePasLeTourEtDonneUneClope() {
        setUp(terrain("Gare"), 10, terrain("Forêt"));
        int clopes = state.cigarettes();
        assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.PlayCard(0)));
        assertEquals("Forêt", state.terrain().name());
        assertEquals(clopes + 1, state.cigarettes());
        assertEquals(100, state.player().hp(), "l'adversaire n'a pas joué");
    }

    @Test
    void gareUnSortCouteLeTourNormalement() {
        setUp(terrain("Gare"), 10, spell(Element.NEUTRAL, 5));
        assertEquals(ActionResult.TURN_ENDED, engine.submit(new Action.PlayCard(0)));
        assertEquals(90, state.player().hp());
    }

    @Test
    void gareAvecPaquetPleinResteGratuite() {
        setUp(terrain("Gare"), 10, terrain("Forêt"));
        state.setCigarettes(state.cigaretteMax());
        assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.PlayCard(0)));
        assertEquals(state.cigaretteMax(), state.cigarettes());
    }

    @Test
    void descriptionDuTerrain() {
        assertTrue(terrain("Gare").effectText().orElseThrow().contains("ne coûte pas le tour"));
        assertTrue(CardPool.startingTerrain().effectText().orElseThrow().contains("+10 %"));
    }

    // --- Désert kaléidoscopique, Fractale ---

    private void setUpAgainst(TerrainCard start, Element enemyElement, double enemyAttack, Card... hand) {
        enemy = Enemy.builder("cible", "Cible", enemyElement, EnemyRank.COMMON).hp(1000)
                .behavior(EnemyBehavior.always(EnemyMove.basicAttack(enemyAttack))).build();
        state = new GameState(new Player(100), List.of(enemy), start, List.of(hand), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(5), new EventBus(), new LootTable(List::of));
        engine.startTurn();
    }

    @Test
    void desertLHypnoseFaitX4() {
        setUpAgainst(terrain("Désert kaléidoscopique"), Element.NEUTRAL, 0, spell(Element.NEUTRAL, 10));
        state.statuses(cardistry.core.status.StatusTarget.ENEMY).apply(cardistry.core.status.StatusType.HYPNOTIZED, 1,
                cardistry.core.status.StatusTarget.PLAYER);
        engine.submit(new Action.PlayCard(0));
        assertEquals(1000 - 40, enemy.hp());
    }

    @Test
    void desertLeJoueurHypnotiseSubitX4() {
        setUpAgainst(terrain("Désert kaléidoscopique"), Element.NEUTRAL, 5);
        state.statuses(cardistry.core.status.StatusTarget.PLAYER).apply(cardistry.core.status.StatusType.HYPNOTIZED, 1,
                cardistry.core.status.StatusTarget.ENEMY);
        engine.submit(new Action.DrawCard());
        assertEquals(80, state.player().hp());
    }

    @Test
    void fractaleArrondiALEntierSuperieurPourLeJoueur() {
        setUpAgainst(terrain("Fractale"), Element.FIRE, 0, spell(Element.WATER, 10), spell(Element.NATURE, 10));
        assertEquals(2.0, state.spellMultiplier((SpellCard) state.hand().get(0)), 1e-9, "×1.5 → ×2");
        assertEquals(1.0, state.spellMultiplier((SpellCard) state.hand().get(1)), 1e-9, "×0.5 → ×1");
        engine.submit(new Action.PlayCard(0));
        assertEquals(1000 - 20, enemy.hp());
    }

    @Test
    void fractaleArrondiAussiLAttaqueAdverseEtLesSoins() {
        setUpAgainst(terrain("Fractale"), Element.FIRE, 10);
        state.playerStats().add(cardistry.core.stats.StatModifier.percent(StatType.ENEMY_DAMAGE, 0.25, "test"));
        state.playerStats().add(cardistry.core.stats.StatModifier.percent(StatType.HEAL_EFFICIENCY, 0.1, "test"));
        assertEquals(2.0, state.enemyAttackMultiplier(), 1e-9, "×1.25 → ×2");
        assertEquals(2.0, state.healMultiplier(Element.NATURE), 1e-9, "×1.1 → ×2");
        engine.submit(new Action.DrawCard());
        assertEquals(80, state.player().hp());
    }

    @Test
    void horsDeLaFractaleLesMultiplicateursRestentDecimaux() {
        setUpAgainst(terrain("Forêt"), Element.FIRE, 0, spell(Element.WATER, 10));
        assertEquals(1.5, state.spellMultiplier((SpellCard) state.hand().get(0)), 1e-9);
    }
}

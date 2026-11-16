package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyRegistry;
import cardistry.model.enemy.EnemyTerrains;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Chaque combat commence chez l'adversaire, avec les malus de son terrain. */
class EnemyTerrainTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    /** Nouvelle partie (deck de départ), combat contre {@code id} au niveau 1. */
    private Enemy fight(String id) {
        state = GameState.newGame();
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        engine = new CombatEngine(state, new CardPool(), new Rng(3), bus, new LootTable(List::of));
        engine.start();
        Enemy enemy = EnemyRegistry.create(id, 1);
        engine.startCombat(enemy, false);
        return enemy;
    }

    @Test
    void chaqueAdversaireASonTerrainEtLesBossLeDojo() {
        assertEquals("Parking du Migros", EnemyTerrains.of(EnemyRegistry.CADDIE).name());
        assertEquals("Le canapé", EnemyTerrains.of(EnemyRegistry.CLARENCE).name());
        assertEquals("Bord d'autoroute", EnemyTerrains.of(EnemyRegistry.PENEU).name());
        for (String choomah : EnemyRegistry.CHOOMAH_VARIANTS) {
            assertEquals("Buisson louche", EnemyTerrains.of(choomah).name());
        }
        assertEquals("Canopée", EnemyTerrains.of(EnemyRegistry.PARESSEUX).name());
        assertEquals("Jurassic Ecublens", EnemyTerrains.of(EnemyRegistry.TREX).name());
        assertEquals("Passage sous-gare", EnemyTerrains.of(EnemyRegistry.CAME).name());
        assertEquals("Amphi d'examen", EnemyTerrains.of(EnemyRegistry.VAFI).name());
        assertEquals("Toit qui fuit", EnemyTerrains.of(EnemyRegistry.SEAU).name());
        assertEquals("Biblio à 3h du mat", EnemyTerrains.of(EnemyRegistry.ETUDIANT).name());
        for (int level = 1; level <= 3; level++) {
            assertEquals("Dojo", EnemyRegistry.create(EnemyRegistry.bossId(level), level).terrain().orElseThrow().name());
        }
        for (String id : EnemyRegistry.ids()) {
            assertTrue(EnemyRegistry.create(id, 1).terrain().isPresent(), id);
        }
    }

    @Test
    void lesTerrainsEnnemisNeSontJamaisDansLePool() {
        List<String> pool = new CardPool().cards().stream().map(Card::name).toList();
        for (String id : EnemyTerrains.enemiesWithTerrain()) {
            assertFalse(pool.contains(EnemyTerrains.of(id).name()), id);
        }
    }

    @Test
    void leCombatCommenceChezLAdversaire() {
        fight(EnemyRegistry.CADDIE);
        assertEquals("Parking du Migros", state.terrain().name());
        assertEquals(6, state.handSize(), "−1 taille de main");
    }

    @Test
    void sortirDeChezLEnnemiRetireSesMalus() {
        fight(EnemyRegistry.CADDIE);
        while (!state.hand().isEmpty()) {
            state.removeFromHand(0);
        }
        state.addToHand(new TerrainCard("Forêt", Element.NATURE, Rarity.COMMON));
        engine.startTurn();
        engine.submit(new Action.PlayCard(0));
        assertEquals(7, state.handSize());
    }

    @Test
    void bordDAutorouteMoinsDeuxDef() {
        Enemy peneu = fight(EnemyRegistry.PENEU);
        assertEquals(-2, state.playerStats().get(StatType.DEFENSE), 1e-9);
        assertEquals(6 + 1 + 2, state.hitDamage(6), 1e-9); // Crevaison 6 + FOR 1, neutre : ×1, DEF −2
        assertEquals(peneu.name(), state.currentEnemy().name());
    }

    @Test
    void buissonLoucheEtToitQuiFuitMoinsVingtPourcentDeSoins() {
        fight(EnemyRegistry.CHOOMAH);
        assertEquals(0.8, state.playerStats().get(StatType.HEAL_EFFICIENCY), 1e-9);
        fight(EnemyRegistry.SEAU);
        assertEquals(0.8, state.playerStats().get(StatType.HEAL_EFFICIENCY), 1e-9);
    }

    @Test
    void canopeeEtAmphiMoinsDeDegatsDesSorts() {
        fight(EnemyRegistry.PARESSEUX);
        assertEquals(0.85, state.playerStats().get(StatType.FIRE_DAMAGE), 1e-9);
        assertEquals(0.85, state.playerStats().get(StatType.NEUTRAL_DAMAGE), 1e-9);
        fight(EnemyRegistry.VAFI);
        assertEquals(0.8, state.playerStats().get(StatType.WATER_DAMAGE), 1e-9);
    }

    @Test
    void jurassicEtPassageAdversairePlusQuinzePourcentEtBonusDeSonElement() {
        fight(EnemyRegistry.TREX);
        assertEquals(1.25 * 1.15, state.enemyAttackMultiplier(), 1e-9, "plante sur terrain plante, +15 %");
        fight(EnemyRegistry.CAME);
        assertEquals(1.25 * 1.15, state.enemyAttackMultiplier(), 1e-9);
    }

    @Test
    void biblioMoinsUneTailleDeMainEtCanapeSansMalus() {
        fight(EnemyRegistry.ETUDIANT);
        assertEquals(6, state.handSize());
        fight(EnemyRegistry.CLARENCE);
        assertEquals(7, state.handSize());
        assertEquals(1.0, state.playerStats().get(StatType.HEAL_EFFICIENCY), 1e-9);
    }

    @Test
    void mainPleineQuandLaTailleBaisseOnGardeSesCartesSansPiocher() {
        fight(EnemyRegistry.CLARENCE);
        engine.startTurn();
        assertEquals(5, state.hand().size());
        state.playerStats().add(StatModifier.flat(StatType.HAND_SIZE, -3, "test")); // taille 4
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.DrawCard()));
        assertEquals(5, state.hand().size(), "aucune carte perdue");
        Card last = state.hand().get(4);
        assertTrue(engine.submit(new Action.PlayCard(4)) != ActionResult.REJECTED, "la 5e carte reste jouable");
        assertFalse(state.hand().contains(last));
    }

    @Test
    void leTerrainEnnemiEstAnnonceAuDebutDuCombat() {
        fight(EnemyRegistry.SEAU);
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.CombatStarted c
                && c.enemy().terrain().orElseThrow().name().equals("Toit qui fuit")));
        assertTrue(EnemyTerrains.of(EnemyRegistry.SEAU).effectText().orElseThrow().contains("−20 %"));
        assertTrue(new SpellCard("x", Element.FIRE, 1, Rarity.COMMON).power().isEmpty());
    }
}

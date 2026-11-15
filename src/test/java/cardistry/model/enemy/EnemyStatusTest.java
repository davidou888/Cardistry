package cardistry.model.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.stats.Stats;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Statuts infligés par les adversaires (Camé, Choomah, Vafi, Paresseux). */
class EnemyStatusTest {

    private GameState state;
    private CombatEngine engine;

    private void setUp(String enemyId, Element terrain, int seed) {
        Enemy enemy = EnemyRegistry.create(enemyId, 1);
        state = new GameState(new Player(500), List.of(enemy), new TerrainCard("T", terrain, Rarity.COMMON),
                List.of(), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), new EventBus(), new LootTable(List::of));
    }

    /** Joue {@code n} tours (le joueur pioche, l'adversaire agit). */
    private void turns(int n) {
        for (int i = 0; i < n; i++) {
            if (engine.startTurn()) {
                engine.submit(new Action.DrawCard());
            }
        }
    }

    private boolean player(StatusType type) {
        return state.statuses(StatusTarget.PLAYER).has(type);
    }

    @Test
    void identifiantsDeStatutsValides() {
        for (String id : List.of(EnemyRegistry.INTOX, EnemyRegistry.FONS, EnemyRegistry.BURN)) {
            assertEquals(id, StatusType.byId(id).id());
        }
    }

    @Test
    void cameTrentePourcentDeBruler() {
        int burned = 0;
        for (int seed = 0; seed < 400; seed++) {
            setUp(EnemyRegistry.CAME, Element.NEUTRAL, seed);
            turns(1); // Coup de coude
            burned += player(StatusType.BURN) ? 1 : 0;
        }
        assertTrue(burned > 90 && burned < 150, "≈ 30 % : " + burned);
    }

    @Test
    void cameNeBrulePasSurUnTerrainEauNiQuandLaTaxePrendUneClope() {
        for (int seed = 0; seed < 50; seed++) {
            setUp(EnemyRegistry.CAME, Element.WATER, seed);
            turns(1);
            assertFalse(player(StatusType.BURN), "eau");
            setUp(EnemyRegistry.CAME, Element.NEUTRAL, seed);
            turns(1);
            state.statuses(StatusTarget.PLAYER).remove(StatusType.BURN);
            turns(1); // Taxe : prend une clope, aucun coup
            assertFalse(player(StatusType.BURN), "taxe sans coup");
        }
    }

    @Test
    void volALaTireDuChoomahDonneIntox() {
        setUp(EnemyRegistry.CHOOMAH, Element.NEUTRAL, 1);
        turns(2);
        assertFalse(player(StatusType.INTOX));
        turns(1); // 3e action : vol à la tire
        assertTrue(player(StatusType.INTOX));
    }

    @Test
    void serieDAlgebreDeVafiEndortUnTour() {
        setUp(EnemyRegistry.VAFI, Element.NEUTRAL, 1);
        turns(3); // 3e action : série d'algèbre
        assertEquals(1, state.statuses(StatusTarget.PLAYER).get(StatusType.FONS).orElseThrow().turnsLeft());
        assertFalse(engine.startTurn(), "tu t'endors : tour sauté");
        assertTrue(engine.startTurn(), "réveillé");
    }

    @Test
    void badoingDuParesseuxVingtCinqPourcentDeFons() {
        int asleep = 0;
        for (int seed = 0; seed < 400; seed++) {
            setUp(EnemyRegistry.PARESSEUX, Element.NEUTRAL, seed);
            turns(2); // 2e action : Badoing, badoing !
            asleep += player(StatusType.FONS) ? 1 : 0;
        }
        assertTrue(asleep > 70 && asleep < 130, "≈ 25 % : " + asleep);
    }
}

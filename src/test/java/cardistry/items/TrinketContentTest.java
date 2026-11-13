package cardistry.items;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.core.Action;
import cardistry.core.CombatEngine;
import cardistry.core.GameState;
import cardistry.core.Rng;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.Stats;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Un test par effet chiffré des trinkets du registre. */
class TrinketContentTest {

    private static final double DELTA = 1e-9;

    final List<GameEvent> events = new ArrayList<>();
    GameState state;
    CombatEngine engine;
    Enemy enemy;

    /**
     * Combat contre un adversaire de 100 PV / 10 d'attaque, sur un terrain neutre pour lui
     * (attaque ×1), avec la main donnée.
     */
    void setUp(cardistry.model.Element enemyElement, List<Card> hand, CardPool pool) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        enemy = new Enemy("Cible", enemyElement, 100, 10);
        TerrainCard neutral = switch (enemyElement) {
            case FIRE -> new TerrainCard("Forêt", NATURE);
            case WATER -> new TerrainCard("EPFL", FIRE);
            case NATURE -> new TerrainCard("Marécage", WATER);
        };
        state = new GameState(new Player(100), List.of(enemy, new Enemy("Suivant", FIRE, 100, 10)), neutral, hand,
                new Stats());
        engine = new CombatEngine(state, pool, new Rng(2), bus, new LootTable(List::of));
    }

    void setUp(cardistry.model.Element enemyElement, List<Card> hand) {
        setUp(enemyElement, hand, new CardPool());
    }

    void equip(String id) {
        engine.equip(ItemRegistry.trinket(id));
    }

    double lastDamageToEnemy() {
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i) instanceof GameEvent.DamageDealt d && d.target() == enemy) {
                return d.amount();
            }
        }
        throw new AssertionError("Aucun dégât à l'adversaire");
    }

    private void spellBoost(String id, cardistry.model.Element element, double expectedDamage) {
        setUp(element, List.of(new SpellCard("Sort", element, 20)));
        equip(id);
        engine.submit(new Action.PlayCard(0));
        assertEquals(expectedDamage, lastDamageToEnemy(), DELTA);
    }

    @Test
    void redistributionDesBriquets() {
        spellBoost("redistribution-des-briquets", FIRE, 25);
    }

    @Test
    void pichetDeSat() {
        spellBoost("pichet-de-sat", WATER, 25);
    }

    @Test
    void planteDuRolex() {
        spellBoost("plante-du-rolex", NATURE, 25);
    }

    @Test
    void lesBonusDElementNeSAppliquentQuASonElement() {
        setUp(WATER, List.of(new SpellCard("Sort", WATER, 20)));
        equip("redistribution-des-briquets");
        engine.submit(new Action.PlayCard(0));
        assertEquals(20, lastDamageToEnemy(), DELTA);
    }

    @Test
    void polycopieDAnalyseI() {
        setUp(FIRE, List.of());
        equip("polycopie-d-analyse-i");
        engine.submit(new Action.DrawCard());
        assertEquals(100 - (10 - 2), state.player().hp(), DELTA);
    }

    @Test
    void pax() {
        setUp(FIRE, List.of());
        equip("pax");
        assertEquals(8, state.handSize());
    }

    @Test
    void grinderSoinsFeu() {
        setUp(FIRE, List.of(new HealCard("Petit pétou", FIRE, 10)));
        state.player().takeDamage(50);
        equip("grinder");
        engine.submit(new Action.PlayCard(0));
        // Terrain Forêt : le feu bat la nature → soin feu ×1 ; Grinder +50 %.
        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
    }

    @Test
    void grinderPetitPetouDeuxFoisPlusFrequent() {
        setUp(FIRE, List.of());
        Card pétou = new HealCard("Petit pétou", FIRE, 15);
        Card other = new SpellCard("Boule de feu", FIRE, 10);
        equip("grinder");
        assertEquals(2.0, state.cardWeight(pétou), DELTA);
        assertEquals(1.0, state.cardWeight(other), DELTA);
    }

    @Test
    void photocopieuse() {
        setUp(FIRE, List.of());
        equip("photocopieuse");
        engine.submit(new Action.DrawCard());
        assertEquals(2, state.hand().size());
    }

    @Test
    void cms() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20)));
        equip("cms");
        engine.submit(new Action.PlayCard(0));
        assertEquals(30, lastDamageToEnemy(), DELTA);          // +50 %
        assertEquals(100 - 12.5, state.player().hp(), DELTA);  // adversaire +25 %
    }

    @Test
    void cmsEtBriquetsSAdditionnent() {
        setUp(FIRE, List.of(new SpellCard("Sort", FIRE, 20)));
        equip("cms");
        equip("redistribution-des-briquets");
        engine.submit(new Action.PlayCard(0));
        assertEquals(20 * 1.75, lastDamageToEnemy(), DELTA);
    }
}

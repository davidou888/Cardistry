package cardistry.items;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Action;
import cardistry.core.ActionResult;
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
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Un test par effet chiffré des objets du registre. */
class ItemContentTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;
    private Enemy hydra;

    private void setUp(List<Card> hand, long seed) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        hydra = new Enemy("Mannequin", FIRE, 100, 10);
        state = new GameState(new Player(100), List.of(hydra), new TerrainCard("EPFL", FIRE, cardistry.items.Rarity.COMMON), hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(seed), bus, new LootTable(List::of));
    }

    private ActionResult use(String id) {
        engine.give(ItemRegistry.item(id));
        return engine.submit(new Action.UseItem(state.items().size() - 1));
    }

    @Test
    void mate() {
        setUp(List.of(), 1);
        state.player().takeDamage(30);
        assertEquals(ActionResult.FREE_ACTION, use("mate"));
        assertEquals(80, state.player().hp(), DELTA);
    }

    @Test
    void spritz() {
        setUp(List.of(), 1);
        state.player().takeDamage(50);
        use("spritz");
        assertEquals(80, state.player().hp(), DELTA);
        assertEquals(1, state.drunkenness());
    }

    @Test
    void pichetVide() {
        setUp(List.of(), 1);
        assertEquals(ActionResult.FREE_ACTION, use("pichet-vide"));
        assertEquals(85, hydra.hp(), DELTA);
        assertEquals(95, state.player().hp(), DELTA);
        assertEquals(1, state.drunkenness());
    }

    @Test
    void pichetVideIgnoreLaReductionDeDegats() {
        setUp(List.of(), 1);
        engine.equip(ItemRegistry.trinket("polycopie-d-analyse-i"));
        use("pichet-vide");
        assertEquals(95, state.player().hp(), DELTA);
    }

    @Test
    void pichetVidePeutTuerLeJoueur() {
        setUp(List.of(), 1);
        state.player().takeDamage(97);
        assertEquals(ActionResult.TURN_ENDED, use("pichet-vide"));
        assertTrue(state.isLost());
        assertTrue(events.contains(new GameEvent.GameLost()));
        assertEquals(0, events.stream().filter(GameEvent.EnemyAttacked.class::isInstance).count());
    }

    @Test
    void pichetVideMortSimultaneeCEstUneDefaite() {
        setUp(List.of(), 1);
        hydra.takeDamage(90);           // l'adversaire meurt aussi
        state.player().takeDamage(97);
        use("pichet-vide");
        assertTrue(state.isLost());
        assertEquals(false, state.isWon());
    }

    @Test
    void caipirinha() {
        setUp(List.of(), 1);
        state.player().takeDamage(50);
        use("caipirinha");
        assertEquals(70, state.player().hp(), DELTA);
        assertEquals(1, state.drunkenness());
    }

    @Test
    void petitPetouObjetSoinFeuEtMoinsUnIvresse() {
        setUp(List.of(), 1);
        state.player().takeDamage(80); // 20 + 20 (Caïpirinha) puis il manque 40 PV
        use("caipirinha");
        use("caipirinha");
        use("petit-petou");
        // Terrain EPFL (feu) : soin feu ×1.5 → 15 × 1.5 = 22.5
        assertEquals(new GameEvent.Healed(state.player(), 22.5, 1.5), events.stream()
                .filter(GameEvent.Healed.class::isInstance).reduce((a, b) -> b).orElseThrow());
        assertEquals(1, state.drunkenness());
    }

    @Test
    void petitPetouNeFaitPasDescendreLIvresseSousZero() {
        setUp(List.of(), 1);
        use("petit-petou");
        assertEquals(0, state.drunkenness());
        assertEquals(0, events.stream().filter(GameEvent.DrunkennessChanged.class::isInstance).count());
    }

    @Test
    void petitPetouEtGrinder() {
        setUp(List.of(), 1);
        state.player().takeDamage(60);
        engine.equip(ItemRegistry.trinket("grinder"));
        use("petit-petou");
        // 15 × 1.5 (terrain feu) × 1.5 (Grinder) = 33.75
        assertEquals(100 - 60 + 33.75, state.player().hp(), DELTA);
    }

    @Test
    void cafffeeeeinnnn() {
        setUp(List.of(), 1);
        assertEquals(ActionResult.FREE_ACTION, use("cafffeeeeinnnn"));
        assertEquals(2, state.hand().size());
    }

    @Test
    void cafffeeeeinnnnLimiteParLaMain() {
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            hand.add(new TerrainCard("T" + i, FIRE, cardistry.items.Rarity.COMMON));
        }
        setUp(hand, 1);
        use("cafffeeeeinnnn");
        assertEquals(7, state.hand().size());
    }

    @Test
    void gazDuBatInfVingtDegatsDUnElementAleatoire() {
        Set<Double> seen = new HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            events.clear();
            setUp(List.of(), seed);
            use("gaz-du-bat-inf");
            GameEvent.DamageDealt damage = (GameEvent.DamageDealt) events.get(1);
            // Contre le Mannequin (feu) : eau ×1.5 = 30, feu ×1 = 20, nature ×0.5 = 10.
            assertTrue(Set.of(10.0, 20.0, 30.0).contains(damage.amount()), "dégâts : " + damage.amount());
            assertEquals(GameEvent.DamageDealt.Source.EFFECT, damage.source());
            seen.add(damage.amount());
        }
        assertEquals(3, seen.size(), "les trois éléments sortent");
    }

    @Test
    void gazDuBatInfDeterministe() {
        setUp(List.of(), 7);
        use("gaz-du-bat-inf");
        double first = hydra.hp();
        events.clear();
        setUp(List.of(), 7);
        use("gaz-du-bat-inf");
        assertEquals(first, hydra.hp(), DELTA);
    }
}

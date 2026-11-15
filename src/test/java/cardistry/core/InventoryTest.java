package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.event.GameEvent.ActionRejected.Reason;
import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.items.Trinket;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private static final double DELTA = 1e-9;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    private void setUp(List<Card> hand, Enemy... enemies) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(enemies), new TerrainCard("Forêt", cardistry.model.Element.NATURE, cardistry.items.Rarity.COMMON),
                hand, new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private static Enemy hydra() {
        return new Enemy("Mannequin", FIRE, 20, 5);
    }

    private static Item item(String id, java.util.function.Consumer<EffectContext> effect) {
        return new Item(id, id, "", "", Rarity.COMMON, Set.of(), effect);
    }

    // --- Trinkets ---

    @Test
    void equiperUnTrinketAppliqueSesModificateursEtLeRetirerLesEnleve() {
        setUp(List.of(), hydra());
        Trinket trinket = Trinket.builder("t", "T", Rarity.COMMON).percent(StatType.FIRE_DAMAGE, 0.25).build();

        engine.equip(trinket);
        assertEquals(1.25, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);
        assertTrue(state.hasTrinket("t"));

        state.unequipTrinket(0);
        assertEquals(1.0, state.playerStats().get(StatType.FIRE_DAMAGE), DELTA);
        assertFalse(state.hasTrinket("t"));
    }

    @Test
    void emplacementsLimites() {
        setUp(List.of(), hydra());
        assertEquals(5, state.trinketSlots());
        assertEquals(3, state.itemSlots());
        for (int i = 0; i < 5; i++) {
            engine.equip(Trinket.builder("t" + i, "T", Rarity.COMMON).build());
        }
        for (int i = 0; i < 3; i++) {
            engine.give(item("o" + i, ctx -> { }));
        }
        assertThrows(IllegalStateException.class, () -> engine.equip(Trinket.builder("x", "X", Rarity.COMMON).build()));
        assertThrows(IllegalStateException.class, () -> engine.give(item("x", ctx -> { })));
    }

    @Test
    void lesHooksDUnTrinketEquipeRecoiventLesEvenementsApresLEvenementEnCours() {
        SpellCard spell = new SpellCard("Jet d'eau", WATER, 10, cardistry.items.Rarity.COMMON);
        setUp(List.of(spell), hydra());
        state.player().takeDamage(50);
        engine.equip(Trinket.builder("sangsue", "Sangsue", Rarity.RARE)
                .hooks(new GameHooks() {
                    @Override
                    public void onCardPlayed(GameEvent.CardPlayed event, EffectContext ctx) {
                        ctx.healPlayer(3);
                    }
                })
                .build());

        engine.submit(new Action.PlayCard(0));

        // Le soin du trinket est livré après l'événement CardPlayed qui l'a déclenché.
        assertEquals(new GameEvent.CardPlayed(spell), events.get(0));
        assertTrue(events.indexOf(new GameEvent.Healed(state.player(), 3, 1.0)) > 0);
        assertEquals(50 + 3 - 5, state.player().hp(), DELTA); // Forêt (nature) : attaque de le Mannequin ×1
    }

    // --- Objets ---

    @Test
    void utiliserUnObjetEstGratuit() {
        setUp(List.of(), hydra());
        state.player().takeDamage(30);
        engine.give(item("mate", ctx -> ctx.healPlayer(10)));

        assertEquals(ActionResult.FREE_ACTION, engine.submit(new Action.UseItem(0)));

        assertEquals(80, state.player().hp(), DELTA); // pas d'attaque adverse
        assertTrue(state.items().isEmpty());
        assertEquals(GameEvent.ItemUsed.class, events.get(0).getClass());
        assertEquals(new GameEvent.Healed(state.player(), 10, 1.0), events.get(1));
        assertFalse(events.stream().anyMatch(GameEvent.EnemyAttacked.class::isInstance));
    }

    @Test
    void lEfficaciteDesSoinsSAppliqueAuxObjets() {
        setUp(List.of(), hydra());
        state.player().takeDamage(50);
        state.playerStats().add(cardistry.core.stats.StatModifier.percent(StatType.HEAL_EFFICIENCY, 0.5, "test"));
        engine.give(item("mate", ctx -> ctx.healPlayer(10)));

        engine.submit(new Action.UseItem(0));

        assertEquals(new GameEvent.Healed(state.player(), 15, 1.5), events.get(1));
        assertEquals(65, state.player().hp(), DELTA);
    }

    @Test
    void objetAbsentRefuse() {
        setUp(List.of(), hydra());
        assertEquals(ActionResult.REJECTED, engine.submit(new Action.UseItem(0)));
        assertEquals(List.of(new GameEvent.ActionRejected(Reason.NO_ITEM)), events);
    }

    @Test
    void unObjetQuiTueLAdversaireTermineLeTour() {
        Enemy hydra = hydra();
        setUp(List.of(), hydra, new Enemy("Bouée", WATER, 30, 10));
        engine.give(item("bombe", ctx -> ctx.damageEnemy(100)));

        assertEquals(ActionResult.TURN_ENDED, engine.submit(new Action.UseItem(0)));

        assertTrue(events.contains(new GameEvent.EnemyKilled(hydra)));
        assertEquals(2, state.combatNumber());
        assertEquals(100, state.player().hp(), DELTA);
    }

    @Test
    void piocherParUnEffetRespecteLaTailleDeMain() {
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            hand.add(new TerrainCard("T" + i, FIRE, cardistry.items.Rarity.COMMON));
        }
        setUp(hand, hydra());
        engine.give(item("cafe", ctx -> ctx.drawCards(2)));

        engine.submit(new Action.UseItem(0));

        assertEquals(7, state.hand().size());
        assertEquals(1, events.stream().filter(GameEvent.CardDrawn.class::isInstance).count());
    }
}

package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatType;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.items.Trinket;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus de set, avec des trinkets « vides » qui ne portent que des tags. */
class SetBonusTest {

    private static final double DELTA = 1e-9;
    private static int serial = 0;

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    private void setUp() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 100, 10), new Enemy("B", FIRE, 100, 10),
                new Enemy("C", FIRE, 100, 10)), new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), List.of(), new Stats());
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(List::of));
    }

    private static Trinket tagged(Tag... tags) {
        return Trinket.builder("t" + (++serial), "T" + serial, Rarity.COMMON).tags(tags).build();
    }

    private void equip(int count, Tag... tags) {
        for (int i = 0; i < count; i++) {
            engine.equip(tagged(tags));
        }
    }

    private double stat(StatType stat) {
        return state.playerStats().get(stat);
    }

    private void killCurrentEnemy() {
        state.currentEnemy().takeDamage(1000);
        engine.submit(new Action.DrawCard());
    }

    // --- EPFL « Cursus » ---

    @Test
    void epflUnTrinketRien() {
        setUp();
        equip(1, Tag.EPFL);
        assertEquals(1.0, stat(StatType.FIRE_DAMAGE), DELTA);
        assertTrue(state.activeSetTiers().isEmpty());
    }

    @Test
    void epflDeux() {
        setUp();
        equip(2, Tag.EPFL);
        assertEquals(1.10, stat(StatType.WATER_DAMAGE), DELTA);
        assertEquals(7, state.handSize());
        assertTrue(events.contains(new GameEvent.SetTierActivated("EPFL « Cursus »", Tag.EPFL, 2,
                "+10 % dégâts élémentaires")));
    }

    @Test
    void epflTroisCumuleLeDeux() {
        setUp();
        equip(3, Tag.EPFL);
        assertEquals(1.10, stat(StatType.FIRE_DAMAGE), DELTA);
        assertEquals(8, state.handSize());
    }

    @Test
    void epflQuatreCumuleTout() {
        setUp();
        equip(4, Tag.EPFL);
        assertEquals(1.25, stat(StatType.NATURE_DAMAGE), DELTA); // 10 % + 15 %
        assertEquals(8, state.handSize());
        assertEquals(125, state.maxHp(), DELTA);
        assertEquals(3, events.stream().filter(GameEvent.SetTierActivated.class::isInstance).count());
    }

    // --- Soirée « Before → Afterparty » ---

    @Test
    void soireeDeuxUneClopeAuDebutDeChaqueCombat() {
        setUp();
        equip(2, Tag.PARTY);
        killCurrentEnemy();
        assertEquals(6, state.cigarettes());
        killCurrentEnemy();
        assertEquals(7, state.cigarettes());
    }

    @Test
    void soireeDeuxPaquetPlein() {
        setUp();
        equip(2, Tag.PARTY);
        state.setCigarettes(20);
        killCurrentEnemy();
        assertEquals(20, state.cigarettes());
    }

    @Test
    void soireeTroisSeuilDeGueuleDeBois() {
        setUp();
        equip(3, Tag.PARTY);
        assertEquals(4, state.hangoverThreshold());
    }

    @Test
    void soireeQuatreChaqueBoissonSoigneCinq() {
        setUp();
        equip(4, Tag.PARTY);
        state.player().takeDamage(50);
        engine.give(new Item("biere", "Bière", "x", "", Rarity.COMMON, Set.of(Tag.DRINK), ctx -> { }));
        engine.submit(new Action.UseItem(0));
        assertEquals(55, state.player().hp(), DELTA);

        engine.give(new Item("eau", "Eau", "x", "", Rarity.COMMON, Set.of(Tag.HEAL), ctx -> { }));
        engine.submit(new Action.UseItem(0));
        assertEquals(55, state.player().hp(), DELTA); // pas une boisson
    }

    // --- Soin, Pioche ---

    @Test
    void soinDeux() {
        setUp();
        equip(2, Tag.HEAL);
        assertEquals(1.15, stat(StatType.HEAL_EFFICIENCY), DELTA);
    }

    @Test
    void piocheDeuxUneCarteDePlusAuDepart() {
        setUp();
        equip(2, Tag.DRAW);
        killCurrentEnemy();
        assertEquals(6, state.hand().size());
    }

    // --- Règles générales ---

    @Test
    void unTrinketComptePourChacunDeSesTags() {
        setUp();
        equip(2, Tag.EPFL, Tag.DRAW);
        assertEquals(2, state.tagCount(Tag.EPFL));
        assertEquals(2, state.tagCount(Tag.DRAW));
        assertEquals(1.10, stat(StatType.FIRE_DAMAGE), DELTA);
        assertEquals(6, state.playerStats().getInt(StatType.STARTING_HAND));
    }

    @Test
    void lesObjetsNeComptentPas() {
        setUp();
        equip(1, Tag.EPFL);
        engine.give(new Item("x", "x", "x", "", Rarity.COMMON, Set.of(Tag.EPFL), ctx -> { }));
        assertTrue(state.activeSetTiers().isEmpty());
    }

    @Test
    void perdreUnTrinketFaitRedescendreLePalier() {
        setUp();
        equip(3, Tag.EPFL);
        events.clear();

        state.unequipTrinket(0);
        engine.equip(tagged(Tag.PARTY)); // un changement d'inventaire publie les notifications

        assertEquals(7, state.handSize());
        assertEquals(1.10, stat(StatType.FIRE_DAMAGE), DELTA);
        assertTrue(events.contains(new GameEvent.SetTierDeactivated("EPFL « Cursus »", Tag.EPFL, 3, "+1 taille de main")));
        assertFalse(events.stream().anyMatch(e -> e instanceof GameEvent.SetTierDeactivated d && d.count() == 2));
    }

    @Test
    void remplacerUnTrinketParUneRecompenseRecalculeLesSets() {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 20, 10), new Enemy("B", FIRE, 100, 10)),
                new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), List.of(new cardistry.model.card.SpellCard("x", cardistry.model.Element.WATER, 100, cardistry.items.Rarity.COMMON)),
                new Stats());
        Trinket incoming = tagged(Tag.PARTY);
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus, new LootTable(() -> List.of(incoming)));
        for (int i = 0; i < 5; i++) {
            engine.equip(i < 2 ? tagged(Tag.EPFL) : tagged(Tag.META));
        }
        assertEquals(1.10, stat(StatType.FIRE_DAMAGE), DELTA);

        engine.submit(new Action.PlayCard(0));
        engine.resolveReward(new RewardChoice.Take(0, 0)); // remplace un trinket EPFL

        assertEquals(1.0, stat(StatType.FIRE_DAMAGE), DELTA);
        assertTrue(events.contains(new GameEvent.SetTierDeactivated("EPFL « Cursus »", Tag.EPFL, 2,
                "+10 % dégâts élémentaires")));
    }
}

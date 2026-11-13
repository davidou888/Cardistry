package cardistry.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.GameState;
import cardistry.core.Rng;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LootTableTest {

    private static Trinket trinket(String id, Rarity rarity) {
        return Trinket.builder(id, id, rarity).description("x").tags(Tag.META).build();
    }

    private static Item item(String id, Rarity rarity) {
        return new Item(id, id, "x", "", rarity, Set.of(Tag.META), ctx -> { });
    }

    @Test
    void propositionsDistinctesEtJamaisSecret() {
        List<Collectible> candidates = List.of(trinket("a", Rarity.COMMON), item("b", Rarity.RARE),
                trinket("secret", Rarity.SECRET), item("c", Rarity.LEGENDARY));
        LootTable table = new LootTable(() -> candidates);
        for (long seed = 0; seed < 50; seed++) {
            List<String> ids = table.offer(GameState.newGame(), new Rng(seed), 3).stream().map(Collectible::id).toList();
            assertEquals(3, ids.size());
            assertEquals(3, ids.stream().distinct().count());
            assertFalse(ids.contains("secret"));
        }
    }

    @Test
    void moinsDeCandidatsQueDemande() {
        LootTable table = new LootTable(() -> List.of(item("a", Rarity.COMMON)));
        assertEquals(1, table.offer(GameState.newGame(), new Rng(1), 3).size());
    }

    @Test
    void memeSeedMemesPropositions() {
        LootTable table = new LootTable();
        List<String> a = table.offer(GameState.newGame(), new Rng(9), 3).stream().map(Collectible::id).toList();
        List<String> b = table.offer(GameState.newGame(), new Rng(9), 3).stream().map(Collectible::id).toList();
        assertEquals(a, b);
    }

    @Test
    void respecteLesPoidsDeRarete() {
        List<Collectible> candidates = List.of(
                item("c", Rarity.COMMON), item("u", Rarity.UNCOMMON), item("r", Rarity.RARE), item("l", Rarity.LEGENDARY));
        LootTable table = new LootTable(() -> candidates);
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        Rng rng = new Rng(123);
        int draws = 20_000;
        for (int i = 0; i < draws; i++) {
            Collectible first = table.offer(GameState.newGame(), rng, 1).get(0);
            counts.merge(first.rarity(), 1, Integer::sum);
        }
        // Poids 60/25/12/3 sur 100 : on tolère ±2 points.
        assertEquals(0.60, counts.get(Rarity.COMMON) / (double) draws, 0.02);
        assertEquals(0.25, counts.get(Rarity.UNCOMMON) / (double) draws, 0.02);
        assertEquals(0.12, counts.get(Rarity.RARE) / (double) draws, 0.02);
        assertEquals(0.03, counts.get(Rarity.LEGENDARY) / (double) draws, 0.02);
    }

    @Test
    void trinketDejaEquipeExclu() {
        // On passe par un état où le trinket est équipé (via le moteur, package core) :
        GameState state = GameState.newGame();
        new cardistry.core.CombatEngine(state, new cardistry.model.card.CardPool(), new Rng(1),
                new cardistry.core.event.EventBus()).equip(trinket("a", Rarity.COMMON));
        LootTable table = new LootTable(() -> List.of(trinket("a", Rarity.COMMON), item("b", Rarity.COMMON)));

        List<Collectible> offer = table.offer(state, new Rng(1), 3);

        assertEquals(1, offer.size());
        assertTrue(offer.get(0) instanceof Item);
    }
}

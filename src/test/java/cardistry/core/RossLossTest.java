package cardistry.core;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cardistry.core.event.EventBus;
import cardistry.core.stats.Stats;
import cardistry.items.Item;
import cardistry.items.ItemRegistry;
import cardistry.items.LootTable;
import cardistry.items.Rarity;
import cardistry.items.Tag;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.TerrainCard;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RossLossTest {

    @Test
    void perdreRossGardeLesPvEnTropMaisReplafonne() {
        GameState state = new GameState(new Player(100), List.of(new Enemy("A", FIRE, 100, 10)),
                new TerrainCard("Forêt", NATURE, cardistry.items.Rarity.COMMON), List.of(), new Stats());
        CombatEngine engine = new CombatEngine(state, new CardPool(), new Rng(1), new EventBus(), new LootTable(List::of));
        engine.equip(ItemRegistry.trinket("ross"));
        engine.give(ItemRegistry.item("spritz"));
        engine.submit(new Action.UseItem(0));
        assertEquals(160, state.player().hp(), 1e-9);   // 30 × 2

        state.unequipTrinket(0);                        // Ross remplacé
        assertEquals(160, state.player().hp(), 1e-9);   // PV en trop conservés
        assertEquals(100, state.maxHp(), 1e-9);

        engine.give(new Item("soin", "soin", "x", "", Rarity.COMMON, Set.of(Tag.HEAL), ctx -> ctx.healPlayer(20)));
        engine.submit(new Action.UseItem(0));
        assertEquals(160, state.player().hp(), 1e-9);   // de nouveau plafonné : rien de plus
    }
}

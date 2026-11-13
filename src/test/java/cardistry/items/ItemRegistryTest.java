package cardistry.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemRegistryTest {

    @Test
    void identifiantsUniques() {
        List<String> ids = ItemRegistry.all().stream().map(Collectible::id).toList();
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    void chaqueEntreeEstComplete() {
        for (Collectible c : ItemRegistry.all()) {
            assertFalse(c.id().isBlank(), c.toString());
            assertFalse(c.name().isBlank(), c.toString());
            assertFalse(c.description().isBlank(), c.toString());
            assertFalse(c.tags().isEmpty(), c.toString());
        }
    }

    @Test
    void chaqueAppelCreeDeNouvellesInstances() {
        assertNotSame(ItemRegistry.trinkets().get(0), ItemRegistry.trinkets().get(0));
    }
}

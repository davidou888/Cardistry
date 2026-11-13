package cardistry.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void nomsExactsDuGameDesign() {
        List<String> names = ItemRegistry.all().stream().map(Collectible::name).toList();
        for (String expected : List.of("Redistribution des briquets", "Pichet de Sat", "Plante du Rolex",
                "Polycopié d'Analyse I", "Pax", "Grinder", "Photocopieuse", "CMS", "Maté",
                "Latte macchiato vanille", "M1", "Monde Snack", "Plan du campus", "Compréhension algébrique", "George")) {
            assertTrue(names.contains(expected), expected);
        }
    }

    @Test
    void chaqueAppelCreeDeNouvellesInstances() {
        assertNotSame(ItemRegistry.trinkets().get(0), ItemRegistry.trinkets().get(0));
    }
}

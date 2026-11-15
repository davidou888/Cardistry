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
                "Latte macchiato vanille", "M1", "Monde Snack", "Plan du campus", "Compréhension algébrique", "George",
                "Cafffeeeeinnnn", "Gaz du bat Inf", "Spritz", "Pichet vide", "Foie d'ingénieur", "Ptit Sat ?",
                "Caïpirinha", "Petit pétou", "Shot de Jäger", "Moscow Mule", "Raclette", "Space cake",
                "Pétou ancestral", "Googz", "Honoré", "Ross", "Diplôme EPFL", "Paquet de cartes")) {
            assertTrue(names.contains(expected), expected);
        }
    }

    @Test
    void identifiantsQualifiesUniquesEntreCartesObjetsEtTrinkets() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        new cardistry.model.card.CardPool().cards().stream()
                .map(cardistry.model.card.Card::qualifiedId).distinct().forEach(ids::add);
        for (Collectible c : ItemRegistry.all()) {
            assertTrue(ids.add(c.qualifiedId()), "identifiant en double : " + c.qualifiedId());
        }
        // Les homonymes carte / objet ne se confondent pas.
        assertTrue(ids.contains("carte:petit-petou"));
        assertTrue(ids.contains("objet:petit-petou"));
        assertTrue(ids.contains("objet:caipirinha"));
    }

    /** Deux cartes du pool avec le même identifiant sont la même carte (doublon = poids), jamais deux cartes différentes. */
    @Test
    void memeIdentifiantMemeCarte() {
        java.util.Map<String, cardistry.model.card.Card> byId = new java.util.HashMap<>();
        for (cardistry.model.card.Card card : new cardistry.model.card.CardPool().cards()) {
            cardistry.model.card.Card first = byId.putIfAbsent(card.qualifiedId(), card);
            if (first != null) {
                assertTrue(first.getClass() == card.getClass() && first.element() == card.element()
                        && first.rarity() == card.rarity(), "deux cartes différentes : " + card.qualifiedId());
            }
        }
    }

    @Test
    void grinderNeCibleQueLaCartePetitPetou() {
        Trinket grinder = ItemRegistry.trinket("grinder");
        cardistry.model.card.Card card = new cardistry.model.card.CardPool().cards().stream()
                .filter(c -> c.name().equals("Petit pétou")).findFirst().orElseThrow();
        assertEquals(2.0, grinder.cardWeight(card), 1e-9);
    }

    @Test
    void chaqueAppelCreeDeNouvellesInstances() {
        assertNotSame(ItemRegistry.trinkets().get(0), ItemRegistry.trinkets().get(0));
    }
}

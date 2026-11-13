package cardistry.model.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import cardistry.core.Rng;
import cardistry.model.Element;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardPoolTest {

    private final CardPool pool = new CardPool();

    @Test
    void chaquePiocheRenvoieUneNouvelleInstance() {
        Rng rng = new Rng(1);
        Set<Card> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < 500; i++) {
            seen.add(pool.draw(rng));
        }
        assertEquals(500, seen.size());
    }

    @Test
    void memeSeedMemeSuiteDeCartes() {
        assertEquals(drawNames(42, 50), drawNames(42, 50));
    }

    private List<String> drawNames(long seed, int count) {
        Rng rng = new Rng(seed);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add(pool.draw(rng).name());
        }
        return names;
    }

    @Test
    void copyConserveLesValeurs() {
        SpellCard spell = new SpellCard("Boule de feu", Element.FIRE, 10);
        SpellCard spellCopy = spell.copy();
        assertNotSame(spell, spellCopy);
        assertEquals("Boule de feu", spellCopy.name());
        assertEquals(Element.FIRE, spellCopy.element());
        assertEquals(10, spellCopy.damage());

        HealCard heal = new HealCard("Potion", Element.NATURE, 20);
        HealCard healCopy = heal.copy();
        assertNotSame(heal, healCopy);
        assertEquals(20, healCopy.amount());
        assertEquals(Element.NATURE, healCopy.element());

        TerrainCard terrain = new TerrainCard("Forêt", Element.NATURE);
        TerrainCard terrainCopy = terrain.copy();
        assertNotSame(terrain, terrainCopy);
        assertEquals("Forêt", terrainCopy.name());
    }
}

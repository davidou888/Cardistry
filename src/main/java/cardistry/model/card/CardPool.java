package cardistry.model.card;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;

import cardistry.core.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Registre des cartes piochables. La pioche est infinie et avec remise ;
 * une carte présente plusieurs fois a plus de chances d'être tirée.
 */
public class CardPool {

    private final List<Card> cards = new ArrayList<>();

    /** Pool de cartes personnalisé (tests, variantes). */
    public CardPool(List<Card> cards) {
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("Le pool est vide");
        }
        this.cards.addAll(cards);
    }

    /** Pool standard du jeu. */
    public CardPool() {
        // --- Sortilèges (nom, élément, dégâts) ---
        cards.add(new SpellCard("Boule de feu", FIRE, 10.0));
        cards.add(new SpellCard("Pluie de flammes", FIRE, 15.0));
        cards.add(new SpellCard("Jet d'eau", WATER, 10.0));
        cards.add(new SpellCard("Tsunami", WATER, 15.0));
        cards.add(new SpellCard("Fouet de lianes", NATURE, 10.0));
        cards.add(new SpellCard("Tempête d'épines", NATURE, 15.0));
        cards.add(new SpellCard("Pluie de flammes", FIRE, 15.0));
        cards.add(new SpellCard("Tsunami", WATER, 15.0));
        cards.add(new SpellCard("Fouet de lianes", NATURE, 10.0));

        // --- Terrains (nom, élément) ---
        cards.add(new TerrainCard("EPFL", FIRE));
        cards.add(new TerrainCard("Marécage", WATER));
        cards.add(new TerrainCard("Forêt", NATURE));
        cards.add(new TerrainCard("Enfer", FIRE));
        cards.add(new TerrainCard("Atlantis", WATER));
        cards.add(new TerrainCard("Amazonie", NATURE));

        // --- Soins (nom, élément, PV rendus) ---
        cards.add(new HealCard("Potion", NATURE, 10.0));
        cards.add(new HealCard("Caïpirinha", WATER, 10.0));
        cards.add(new HealCard("Petit pétou", FIRE, 15.0));
        cards.add(new HealCard("Grande potion", NATURE, 20.0));
    }

    /** Terrain sur lequel commence la partie. */
    public static TerrainCard startingTerrain() {
        return new TerrainCard("EPFL", FIRE);
    }

    /** Tire une carte au hasard et en renvoie une copie. */
    public Card draw(Rng rng) {
        return rng.pick(cards).copy();
    }
}

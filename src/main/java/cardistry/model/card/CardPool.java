package cardistry.model.card;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;

import cardistry.core.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Registre des cartes : tirées pour les récompenses de cartes et les effets qui créent une carte
 * (George), et pour la pioche des parties sans deck (tests). Une carte présente plusieurs fois a
 * plus de chances d'être tirée.
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

    /** Cartes du registre (avec leurs doublons), en lecture seule. */
    public List<Card> cards() {
        return java.util.Collections.unmodifiableList(cards);
    }

    /**
     * Deck de départ (12 cartes) : 6 sorts (un de 10 et un de 15 par élément), 3 soins, 3 terrains.
     * Le terrain de départ (EPFL) est en jeu, pas dans le deck.
     */
    public static List<Card> startingDeck() {
        return List.of(
                new SpellCard("Boule de feu", FIRE, 10.0),
                new SpellCard("Pluie de flammes", FIRE, 15.0),
                new SpellCard("Jet d'eau", WATER, 10.0),
                new SpellCard("Tsunami", WATER, 15.0),
                new SpellCard("Fouet de lianes", NATURE, 10.0),
                new SpellCard("Tempête d'épines", NATURE, 15.0),
                new HealCard("Potion", NATURE, 10.0),
                new HealCard("Caïpirinha", WATER, 10.0),
                new HealCard("Petit pétou", FIRE, 15.0),
                new TerrainCard("Enfer", FIRE),
                new TerrainCard("Atlantis", WATER),
                new TerrainCard("Amazonie", NATURE));
    }

    /** Terrain sur lequel commence la partie. */
    public static TerrainCard startingTerrain() {
        return new TerrainCard("EPFL", FIRE);
    }

    /** Tire une carte au hasard et en renvoie une copie. */
    /**
     * Tire une carte en pondérant chaque entrée du registre par {@code weight} (1 = normal) ;
     * renvoie une copie. Si tous les poids valent 1, équivaut à {@link #draw(Rng)}.
     */
    public Card draw(Rng rng, java.util.function.ToDoubleFunction<Card> weight) {
        double[] weights = new double[cards.size()];
        double total = 0;
        boolean uniform = true;
        for (int i = 0; i < cards.size(); i++) {
            weights[i] = Math.max(0, weight.applyAsDouble(cards.get(i)));
            total += weights[i];
            uniform &= weights[i] == 1.0;
        }
        if (uniform || total <= 0) {
            return draw(rng);
        }
        double roll = rng.nextDouble() * total;
        for (int i = 0; i < cards.size(); i++) {
            roll -= weights[i];
            if (roll < 0) {
                return cards.get(i).copy();
            }
        }
        return cards.get(cards.size() - 1).copy();
    }

    public Card draw(Rng rng) {
        return rng.pick(cards).copy();
    }
}

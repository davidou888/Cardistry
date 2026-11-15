package cardistry.model.card;

import java.util.ArrayList;
import java.util.List;

import cardistry.core.Rng;
import cardistry.core.stats.StatType;
import static cardistry.core.stats.StatModifier.percent;
import static cardistry.items.Rarity.COMMON;
import static cardistry.items.Rarity.LEGENDARY;
import static cardistry.items.Rarity.RARE;
import static cardistry.items.Rarity.UNCOMMON;
import static cardistry.model.Element.ALGEBRAIC;
import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.GEOMETRIC;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.NEUTRAL;
import static cardistry.model.Element.SPIRIT;
import static cardistry.model.Element.WATER;

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
        //COMMON
        cards.add(new SpellCard("Boule de feu", FIRE, 10.0, COMMON));
        cards.add(new SpellCard("Jet d'eau", WATER, 10.0, COMMON));
        cards.add(new SpellCard("Fouet de lianes", NATURE, 10.0, COMMON));
        cards.add(new SpellCard("Pet de Mamouz", NEUTRAL, 10.0, COMMON));
        cards.add(new SpellCard("Carte basique pas ouf", NEUTRAL, 10.0, COMMON));
        cards.add(new SpellCard("Lancer de manette", NEUTRAL, 12.0, COMMON));
        //UNCOMMON
        cards.add(new SpellCard("Pluie de flammes", FIRE, 15.0, UNCOMMON));
        cards.add(new SpellCard("Tsunami", WATER, 15.0, UNCOMMON));
        cards.add(new SpellCard("Tempête d'épines", NATURE, 15.0, UNCOMMON));

        //RARE
        //LEGENDARY
        cards.add(new SpellCard("Big Lez Shotgun", NEUTRAL, 80, LEGENDARY));


        
        // --- Terrains (nom, élément) ---
        // COMMON
        cards.add(new TerrainCard("Marécage", WATER, COMMON));
        cards.add(new TerrainCard("Forêt", NATURE, COMMON));
        cards.add(new TerrainCard("Volcan", FIRE, COMMON));
        cards.add(dojo());
        cards.add(new TerrainCard("Migros", NEUTRAL, COMMON).withBonuses(
                "si le combat se termine ici : +25 % de Choomies", percent(StatType.CHOOMIES_GAIN, 0.25, "")));
        cards.add(new TerrainCard("Gare", NEUTRAL, COMMON).withEffect(TerrainCard.Effect.FREE_DEPARTURE,
                "jouer un terrain depuis la Gare ne coûte pas le tour et donne +1 clope"));

        //UNCOMMON
        cards.add(new TerrainCard("Amazonie", NATURE, UNCOMMON).withBonuses("attaques plante +20 %",
                percent(StatType.NATURE_DAMAGE, ELEMENT_TERRAIN_BONUS, "")));
        cards.add(new TerrainCard("Désert kaléidoscopique", SPIRIT, UNCOMMON));
        cards.add(new TerrainCard("Espace de dimension n", ALGEBRAIC, UNCOMMON));
        cards.add(new TerrainCard("Fractale", GEOMETRIC, UNCOMMON));

        //RARE
        cards.add(new TerrainCard("Enfer", FIRE, RARE).withBonuses("attaques feu +20 %",
                percent(StatType.FIRE_DAMAGE, ELEMENT_TERRAIN_BONUS, "")));
        cards.add(new TerrainCard("Atlantis", WATER, RARE).withBonuses("attaques eau +20 %",
                percent(StatType.WATER_DAMAGE, ELEMENT_TERRAIN_BONUS, "")));
        //LEGENDARY
        cards.add(new TerrainCard("EPFL", FIRE, LEGENDARY));
        
        
        
        
        

        // --- Soins (nom, élément, PV rendus) ---
        //COMMON
        cards.add(new HealCard("Potion", NATURE, 10.0, COMMON));
        cards.add(new HealCard("Petit pétou", FIRE, 15.0, COMMON));
        //UNCOMMON
        cards.add(new HealCard("Grande potion", NATURE, 20.0, UNCOMMON));
        //RARE
        //LEGENDARY
        
        
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
            new SpellCard("Boule de feu", FIRE, 10.0, COMMON),
            new SpellCard("Jet d'eau", WATER, 10.0, COMMON),
            new SpellCard("Fouet de lianes", NATURE, 10.0, COMMON),
            new SpellCard("Pet de Mamouz", NEUTRAL, 10.0, COMMON),
            new SpellCard("Carte basique pas ouf", NEUTRAL, 10.0, COMMON),
            new SpellCard("Lancer de manette", NEUTRAL, 12.0, COMMON),
            new HealCard("Potion", NATURE, 10.0, COMMON),
            new HealCard("Potion", NATURE, 10.0, COMMON),
            new HealCard("Potion", NATURE, 10.0, COMMON),
            new TerrainCard("Marécage", WATER, COMMON),
            new TerrainCard("Forêt", NATURE, COMMON),
            new TerrainCard("Volcan", FIRE, COMMON)

        );
    }

    /** Terrain sur lequel commence la partie. */
    public static TerrainCard startingTerrain() {
        return dojo();
    }

    /** Bonus de dégâts du Dojo (tous éléments). */
    public static final double DOJO_DAMAGE_BONUS = 0.10;
    /** Bonus d'Amazonie, Enfer, Atlantis à leur élément. */
    public static final double ELEMENT_TERRAIN_BONUS = 0.20;

    /** Dojo : terrain de départ, +10 % de dégâts. */
    public static TerrainCard dojo() {
        return new TerrainCard("Dojo", NEUTRAL, COMMON).withBonuses("+10 % de dégâts",
                StatType.allDamageMultipliers().stream().map(stat -> percent(stat, DOJO_DAMAGE_BONUS, "")).toList());
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

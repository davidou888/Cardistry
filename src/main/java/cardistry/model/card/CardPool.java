package cardistry.model.card;

import java.util.ArrayList;
import java.util.List;

import cardistry.core.Rng;
import cardistry.core.stats.StatType;
import cardistry.core.status.StatusExtras;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
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
        cards.add(bouleDeFeu());
        cards.add(new SpellCard("Jet d'eau", WATER, 10.0, COMMON));
        cards.add(fouetDeLianes());
        cards.add(petDeMamouz());
        cards.add(new SpellCard("Carte basique pas ouf", NEUTRAL, 10.0, COMMON));
        cards.add(new SpellCard("Lancer de manette", NEUTRAL, 12.0, COMMON));
        cards.add(new SpellCard("CAYOUUU", NEUTRAL, 12, COMMON));
        cards.add(new SpellCard("Pain pita", GEOMETRIC, 10, COMMON));
        cards.add(matrix());
        //UNCOMMON
        cards.add(new SpellCard("Pluie de flammes", FIRE, 15.0, UNCOMMON));
        cards.add(new SpellCard("Tsunami", WATER, 15.0, UNCOMMON));
        cards.add(new SpellCard("Tempête d'épines", NATURE, 15.0, UNCOMMON));
        cards.add(moindreCarre());
        cards.add(distPlayer());

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
        cards.add(new TerrainCard("Enfer", FIRE, UNCOMMON).withBonuses("attaques feu +20 %",
                percent(StatType.FIRE_DAMAGE, ELEMENT_TERRAIN_BONUS, "")));
        cards.add(new TerrainCard("Atlantis", WATER, UNCOMMON).withBonuses("attaques eau +20 %",
                percent(StatType.WATER_DAMAGE, ELEMENT_TERRAIN_BONUS, "")));

        //RARE
        cards.add(new TerrainCard("Désert kaléidoscopique", SPIRIT, RARE).withEffect(
                TerrainCard.Effect.DEEP_HYPNOSIS, "l'hypnose fait ×4 au lieu de ×2"));
        cards.add(new TerrainCard("Espace de dimension n", ALGEBRAIC, RARE).withEffect(TerrainCard.Effect.REPLAY,
                "chaque carte jouée ici est rejouée une fois à 50 % au tour suivant, si tu y es encore"));
        cards.add(new TerrainCard("Fractale", GEOMETRIC, RARE).withEffect(TerrainCard.Effect.CEILING,
                "tous les multiplicateurs (toi et l'adversaire) arrondis à l'entier supérieur"));

        //LEGENDARY
        cards.add(epfl());
        
        
        
        
        

        // --- Soins (nom, élément, PV rendus) ---
        //COMMON
        cards.add(new HealCard("Potion", NATURE, 10.0, COMMON));
        cards.add(petiteTaffe());
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
            bouleDeFeu(),
            new SpellCard("Jet d'eau", WATER, 10.0, COMMON),
            fouetDeLianes(),
            petDeMamouz(),
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

    // --- Cartes à pouvoir partagées (pool, deck de départ, événements, trinkets) ---

    public static final double BOULE_DE_FEU_BURN = 0.10;
    public static final double FOUET_BONDAGE = 0.10;
    public static final double PETITE_TAFFE_FONS = 0.15;
    /** Soin par tour (en part des PV max) pendant la Fons de la Petite taffe. */
    public static final double PETITE_TAFFE_HEAL_PER_TURN = 0.05;

    /** Boule de feu : 10 dégâts feu, 10 % d'infliger Brûlure. */
    public static SpellCard bouleDeFeu() {
        return new SpellCard("Boule de feu", FIRE, 10, COMMON).withPower("10 % d'infliger Brûlure",
                ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.BURN, BOULE_DE_FEU_BURN));
    }

    /** Fouet de lianes : 10 dégâts plante, 10 % d'infliger Bondage. */
    public static SpellCard fouetDeLianes() {
        return new SpellCard("Fouet de lianes", NATURE, 10, COMMON).withPower("10 % d'infliger Bondage",
                ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.BONDAGE, FOUET_BONDAGE));
    }

    /** Pet de Mamouz : aucun dégât, Intox à coup sûr. */
    public static SpellCard petDeMamouz() {
        return new SpellCard("Pet de Mamouz", NEUTRAL, 0, COMMON).withPower("inflige Intox",
                ctx -> ctx.applyStatus(StatusTarget.ENEMY, StatusType.INTOX, 1));
    }

    /**
     * Petite taffe (ex-Petit pétou) : soin feu de 15 ; 15 % de tomber en Fons, pendant laquelle tu
     * récupères 5 % de tes PV max par tour.
     */
    public static HealCard petiteTaffe() {
        return new HealCard("Petite taffe", FIRE, 15, COMMON).withPower(
                "15 % de tomber en Fons (+5 % de tes PV max par tour pendant la Fons)",
                ctx -> ctx.applyStatus(StatusTarget.PLAYER, StatusType.FONS, PETITE_TAFFE_FONS, 0,
                        new StatusExtras(PETITE_TAFFE_HEAL_PER_TURN, false)));
    }

    /** Matrix : dégâts de base, doublés si les PV de l'adversaire finissent par 0 ou 1 (avant le coup). */
    public static final double MATRIX_DAMAGE = 10;

    /** Matrix : 10 dégâts, ×2 si les PV de l'adversaire finissent par 0 ou 1 (vérifié avant le coup). */
    public static SpellCard matrix() {
        return new SpellCard("Matrix", ALGEBRAIC, MATRIX_DAMAGE, COMMON).withDamageRule("10 / 20",
                ctx -> {
                    int lastDigit = Math.floorMod(ctx.enemyHp(), 10);
                    return lastDigit == 0 || lastDigit == 1 ? 2 * MATRIX_DAMAGE : MATRIX_DAMAGE;
                }).withPower("×2 si les PV de l'adversaire finissent par 0 ou 1", ctx -> { });
    }

    public static final int MOINDRE_CARRE_MIN = 1;
    public static final int MOINDRE_CARRE_MAX = 40;

    /** Moindre carré : dégâts tirés entre 1 et 40 (tirage en ta faveur : deux avec CLAUDE.md, le meilleur). */
    public static SpellCard moindreCarre() {
        return new SpellCard("Moindre carré", ALGEBRAIC, 0, UNCOMMON).withDamageRule(
                MOINDRE_CARRE_MIN + "-" + MOINDRE_CARRE_MAX,
                ctx -> ctx.favorable(() -> MOINDRE_CARRE_MIN + ctx.rng().nextInt(MOINDRE_CARRE_MAX - MOINDRE_CARRE_MIN + 1),
                        Integer::compare));
    }

    public static final double DIST_PLAYER_DAMAGE = 10;

    /** dist(PLAYER) : 10 dégâts + le nombre d'étages parcourus. */
    public static SpellCard distPlayer() {
        return new SpellCard("dist(PLAYER)", GEOMETRIC, DIST_PLAYER_DAMAGE, UNCOMMON).withDamageRule(
                "10 + étages", ctx -> DIST_PLAYER_DAMAGE + ctx.floorsTraveled());
    }

    /** Tours passés à l'EPFL avant le retour automatique au Dojo. */
    public static final int EPFL_TURNS = 3;

    /**
     * EPFL : pendant {@link #EPFL_TURNS} tours, toutes les valeurs du joueur sont doublées (dégâts,
     * soins, réduction de dégâts, PV max ; PV actuels ×2 à l'arrivée, ÷2 au départ). Pas la taille de
     * main, les Choomies ni les clopes.
     */
    public static TerrainCard epfl() {
        java.util.List<cardistry.core.stats.StatModifier> doubled = new java.util.ArrayList<>();
        for (StatType stat : StatType.allDamageMultipliers()) {
            doubled.add(cardistry.core.stats.StatModifier.multiply(stat, 2, ""));
        }
        for (StatType stat : java.util.List.of(StatType.HEAL_EFFICIENCY, StatType.DAMAGE_REDUCTION, StatType.MAX_HP)) {
            doubled.add(cardistry.core.stats.StatModifier.multiply(stat, 2, ""));
        }
        return new TerrainCard("EPFL", FIRE, LEGENDARY)
                .withBonuses("", doubled)
                .withEffect(TerrainCard.Effect.EPFL, "pendant " + EPFL_TURNS + " tours, tout est doublé : dégâts,"
                        + " soins, réduction, PV (÷2 en partant) ; puis retour au Dojo");
    }

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

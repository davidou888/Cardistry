package cardistry.core;

import cardistry.model.Element;

/**
 * Ce que les effets d'objets et de trinkets peuvent faire. Chaque action modifie l'état et publie
 * l'événement correspondant, comme le ferait une carte.
 */
public interface EffectContext {

    GameState state();

    Rng rng();

    /** Inflige {@code amount} dégâts bruts à l'adversaire actuel (aucun multiplicateur). */
    void damageEnemy(double amount);

    /**
     * Inflige {@code amount} dégâts de l'élément {@code element}, comme un sortilège : avec le
     * multiplicateur élémentaire et la stat de dégâts de cet élément.
     */
    void elementalDamage(double amount, Element element);

    /** Remplace la carte à l'index {@code slot} de la main par une carte tirée du pool. */
    void replaceCardInHand(int slot);

    /** Soigne le joueur de {@code amount} PV × efficacité des soins, plafonné aux PV max. */
    void healPlayer(double amount);

    /** Inflige {@code amount} dégâts bruts au joueur (ni réduction ni multiplicateur) ; peut le tuer. */
    void damagePlayer(double amount);

    /** Ajoute {@code amount} points d'Ivresse (peut déclencher une Gueule de bois). */
    void addDrunkenness(int amount);

    /** Pioche jusqu'à {@code count} cartes, sans dépasser la taille de main. */
    void drawCards(int count);
}

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

    /**
     * Soin de l'élément {@code element}, comme une carte de soin : terrain, efficacité des soins et
     * soin de l'élément ; plafonné aux PV max.
     */
    void elementalHeal(double amount, Element element);

    /** Soigne le joueur de {@code amount} PV × efficacité des soins, plafonné aux PV max. */
    void healPlayer(double amount);

    /** Inflige {@code amount} dégâts bruts au joueur (ni réduction ni multiplicateur) ; peut le tuer. */
    void damagePlayer(double amount);

    /** Ajoute {@code amount} points d'Ivresse (négatif pour en retirer, jamais sous 0) ; peut déclencher une Gueule de bois. */
    void addDrunkenness(int amount);

    /** Pose un effet temporaire (voir {@link TemporaryEffect}). */
    void addEffect(TemporaryEffect effect);

    /** Pioche jusqu'à {@code count} cartes, sans dépasser la taille de main. */
    void drawCards(int count);
}

package cardistry.core;

/**
 * Ce que les effets d'objets et de trinkets peuvent faire. Chaque action modifie l'état et publie
 * l'événement correspondant, comme le ferait une carte.
 */
public interface EffectContext {

    GameState state();

    Rng rng();

    /** Inflige {@code amount} dégâts bruts à l'adversaire actuel (aucun multiplicateur). */
    void damageEnemy(double amount);

    /** Soigne le joueur de {@code amount} PV × efficacité des soins, plafonné aux PV max. */
    void healPlayer(double amount);

    /** Pioche jusqu'à {@code count} cartes, sans dépasser la taille de main. */
    void drawCards(int count);
}

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

    /**
     * Remplace la carte à l'index {@code slot} de la main par une carte tirée du pool. Sans effet
     * sur une carte morte (NO CARDS).
     */
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

    /** Puissance du trinket équipé {@code trinketId} (×2 avec Diplôme EPFL pour un trinket EPFL), 1 sinon. */
    double potency(String trinketId);

    /** Ajoute jusqu'à {@code count} clopes sans dépasser le paquet ; renvoie le nombre ajouté. */
    int addCigarettes(int count);

    /** Pose un effet temporaire (voir {@link TemporaryEffect}). */
    void addEffect(TemporaryEffect effect);

    /** Pioche jusqu'à {@code count} cartes, sans dépasser la taille de main. */
    void drawCards(int count);

    /**
     * L'adversaire actuel devient null : mort sur le coup (sa mort est résolue par le moteur au
     * prochain point de contrôle, comme une mort par dégâts).
     */
    void nullifyEnemy();

    /** La carte à l'index {@code slot} de la main devient « NO CARDS » (injouable jusqu'à la fin du combat). */
    void voidCardInHand(int slot);

    /** Tirage de chance en faveur du joueur (deux chances avec CLAUDE.md, voir {@link Luck}). */
    default boolean chance(double probability) {
        return Luck.chance(state(), rng(), probability);
    }

    /** Tirage en faveur du joueur : le plus favorable de deux avec CLAUDE.md (voir {@link Luck}). */
    default <T> T favorable(java.util.function.Supplier<T> roll, java.util.Comparator<? super T> better) {
        return Luck.favorable(state(), roll, better);
    }

    /** Affiche une ligne dans le journal (capacité d'un personnage qui se déclenche). */
    default void announce(String message) {
    }

    /** Ajoute une carte au deck de la run (sans deck : rien). */
    default void addCardToDeck(cardistry.model.card.Card card) {
    }

    /**
     * Tente d'infliger {@code type} à {@code target} pour sa durée par défaut, avec une chance de
     * {@code probability} (1 = sûr). Infligé à l'adversaire, c'est un tirage en faveur du joueur
     * (doublé par CLAUDE.md) ; infligé au joueur, non.
     *
     * @return {@code true} si le statut a été posé (ou sa durée remise au max)
     */
    default boolean applyStatus(cardistry.core.status.StatusTarget target, cardistry.core.status.StatusType type,
            double probability) {
        return false;
    }

    /** Comme {@link #applyStatus(cardistry.core.status.StatusTarget, cardistry.core.status.StatusType, double)}, pour {@code turns} tours. */
    default boolean applyStatus(cardistry.core.status.StatusTarget target, cardistry.core.status.StatusType type,
            double probability, int turns) {
        return false;
    }
}

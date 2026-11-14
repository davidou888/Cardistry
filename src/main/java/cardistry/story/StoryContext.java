package cardistry.story;

import cardistry.core.GameState;
import cardistry.model.card.Card;

/**
 * Ce que la conséquence d'un choix d'événement peut faire (fourni par le moteur ; chaque action
 * publie l'événement de jeu correspondant). Hors combat : les dégâts ne tuent jamais.
 */
public interface StoryContext {

    GameState state();

    /** Ligne de résultat affichée au joueur après son choix. */
    void say(String line);

    /** Tirage de chance en faveur du joueur (doublé par CLAUDE.md). */
    boolean chance(double probability);

    /** Choomies gagnés (positif) ou payés (négatif), jamais sous 0. */
    void choomies(int amount);

    /** Soin (efficacité des soins comprise). */
    void heal(double amount);

    /** Dégâts bruts ; laissent au moins 1 PV. */
    void hurt(double amount);

    /** Clopes en plus (sans dépasser le paquet) ; renvoie le nombre ajouté. */
    int cigarettes(int count);

    /** Gueule de bois au prochain combat (intensité 1, cumulable). */
    void hangover();

    /** Carte ajoutée au deck. */
    void addCard(Card card);
}

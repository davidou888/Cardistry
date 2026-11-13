package cardistry.ui;

import cardistry.core.Action;
import cardistry.core.GameState;

/** Saisie du joueur. */
public interface Input {

    /** {@code true} si le joueur veut voir l'intro. */
    boolean wantsIntro();

    /** Attend que le joueur veuille continuer. */
    void waitForContinue();

    /** Action choisie par le joueur (pas forcément valide : le moteur valide). */
    Action nextAction(GameState state);
}

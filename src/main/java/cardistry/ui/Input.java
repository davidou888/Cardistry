package cardistry.ui;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.items.Collectible;
import java.util.List;

/** Saisie du joueur. */
public interface Input {

    /** {@code true} si le joueur veut voir l'intro. */
    boolean wantsIntro();

    /** Attend que le joueur veuille continuer. */
    void waitForContinue();

    /**
     * Action choisie par le joueur (pas forcément valide : le moteur valide).
     *
     * @throws cardistry.ui.QuitGameException si le joueur quitte
     */
    Action nextAction(GameState state);

    /**
     * Choix d'une récompense parmi {@code options}. Peut être invalide (le moteur valide et
     * redemande). Par défaut : refuse.
     */
    default RewardChoice chooseReward(GameState state, List<Collectible> options) {
        return new RewardChoice.Skip();
    }

    /** Attend avant de fermer le jeu, une fois l'écran de fin affiché. */
    default void waitForExit() {
    }
}

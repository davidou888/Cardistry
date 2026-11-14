package cardistry.ui;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.core.PlayerDecisions;
import cardistry.core.RewardChoice;
import cardistry.items.Collectible;
import java.util.List;

/** Saisie du joueur, y compris les décisions demandées par le moteur en cours d'action. */
public interface Input extends PlayerDecisions {

    /** Pioche au choix : par défaut la première carte (les interfaces la remplacent). */
    @Override
    default int chooseDrawnCard(GameState state, List<cardistry.model.card.Card> options) {
        return 0;
    }

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

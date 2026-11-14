package cardistry.ui;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;

/** Affichage du jeu. Implémenté en texte simple aujourd'hui, en Lanterna plus tard. */
public interface Renderer {

    /** Appelé pour chaque événement publié par le moteur. */
    void onEvent(GameEvent event);

    void showIntro(GameState state);

    /** PV, adversaire, terrain. */
    void showStatus(GameState state);

    /** Main du joueur et choix possibles. */
    void showHand(GameState state);

    /** Carte du niveau, avant le choix du prochain nœud. */
    default void showMap(cardistry.run.map.MapView view) {
    }

    /**
     * Écran de fin (victoire ou défaite), une fois la partie terminée.
     *
     * @param unlockedSecrets noms des secrets débloqués pendant cette partie (souvent aucun)
     */
    default void showGameOver(GameState state, java.util.List<String> unlockedSecrets) {
    }
}

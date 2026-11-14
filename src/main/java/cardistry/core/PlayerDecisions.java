package cardistry.core;

import cardistry.model.card.Card;
import java.util.List;

/**
 * Décisions que le moteur demande au joueur <i>au milieu</i> d'une action (une carte piochée par un
 * hook, un trinket à jeter après une récompense…). L'interface ({@code ui.Input}) les implémente ;
 * les tests fournissent les leurs. Une réponse hors limites est redemandée.
 */
public interface PlayerDecisions {

    /** Toujours la première option (valeur par défaut du moteur). */
    PlayerDecisions FIRST = new PlayerDecisions() {
        @Override
        public int chooseDrawnCard(GameState state, List<Card> options) {
            return 0;
        }

        @Override
        public int chooseTrinketToDiscard(GameState state) {
            return 0;
        }
    };

    /** Index de la carte gardée parmi {@code options} (pioche au choix : Paquet de cartes). */
    int chooseDrawnCard(GameState state, List<Card> options);

    /** Index du trinket à jeter quand il y en a plus que d'emplacements. */
    int chooseTrinketToDiscard(GameState state);
}

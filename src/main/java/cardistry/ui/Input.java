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

    /**
     * Écran titre, avant le début de la partie : jouer, voir les règles, ou entrer un code secret.
     *
     * @throws cardistry.ui.QuitGameException si le joueur quitte
     */
    TitleChoice title(long seed);

    /**
     * Choix du personnage, après l'écran titre. Par défaut le premier.
     *
     * @return le personnage choisi, ou vide pour revenir à l'écran titre
     * @throws cardistry.ui.QuitGameException si le joueur quitte
     */
    default java.util.Optional<cardistry.hero.Hero> chooseHero(List<cardistry.hero.Hero> heroes) {
        return java.util.Optional.of(heroes.get(0));
    }

    /** Attend que le joueur veuille continuer. */
    void waitForContinue();

    /**
     * Action choisie par le joueur (pas forcément valide : le moteur valide). {@link Action.Undo}
     * demande d'annuler le dernier tour (trinket Ctrl+Z).
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

    /**
     * Choix du prochain nœud de la carte : index dans {@code view.choices()}. Par défaut le premier.
     *
     * @throws cardistry.ui.QuitGameException si le joueur quitte
     */
    default int chooseNode(cardistry.run.map.MapView view) {
        return 0;
    }

    /** Récompense de carte : index dans {@code options}, ou −1 pour passer. Par défaut : passe. */
    default int chooseCardReward(GameState state, List<cardistry.model.card.Card> options) {
        return -1;
    }

    /** Sieste « Faire le tri » : index de la carte du deck à retirer. Par défaut la première. */
    default int chooseCardToRemove(GameState state) {
        return 0;
    }

    /**
     * Shop : la prochaine action. Un achat d'objet ou de trinket avec l'inventaire plein doit dire
     * quoi remplacer ({@code Buy.replaceSlot}). Par défaut : partir.
     *
     * @param removalPrice prix du prochain retrait de carte
     */
    default cardistry.core.shop.ShopAction chooseShopAction(GameState state, cardistry.core.Shop shop,
            int removalPrice) {
        return new cardistry.core.shop.ShopAction.Leave();
    }

    /**
     * Événement : index du choix (le moteur refuse un choix indisponible et redemande). Par défaut :
     * le premier choix disponible.
     */
    default int chooseStory(GameState state, cardistry.story.Story story) {
        for (int i = 0; i < story.choices().size(); i++) {
            if (story.choices().get(i).isAvailable(state)) {
                return i;
            }
        }
        return 0;
    }

    /** Sieste : un choix parmi {@code options}. Par défaut le premier. */
    default cardistry.run.RestOption chooseRest(GameState state, List<cardistry.run.RestOption> options) {
        return options.get(0);
    }

    /** Attend avant de fermer le jeu, une fois l'écran de fin affiché. */
    default void waitForExit() {
    }
}

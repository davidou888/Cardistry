package cardistry.core.shop;

/** Ce que le joueur fait au shop. */
public sealed interface ShopAction {

    /**
     * Acheter l'article {@code offer}.
     *
     * @param replaceSlot emplacement (objet ou trinket) à libérer si l'inventaire est plein ; −1 sinon
     */
    record Buy(int offer, int replaceSlot) implements ShopAction {

        public static Buy of(int offer) {
            return new Buy(offer, -1);
        }
    }

    /** Payer pour retirer la carte {@code deckIndex} du deck. */
    record RemoveCard(int deckIndex) implements ShopAction {
    }

    /** Payer pour renouveler les articles. */
    record Reroll() implements ShopAction {
    }

    /** Quitter le shop (retour à la carte). */
    record Leave() implements ShopAction {
    }
}

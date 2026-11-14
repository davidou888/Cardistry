package cardistry.core.shop;

import cardistry.items.Collectible;
import cardistry.model.card.Card;

/** Un article du shop et son prix. */
public sealed interface ShopOffer {

    int price();

    String name();

    /** Une carte, ajoutée au deck à l'achat. */
    record CardOffer(Card card, int price) implements ShopOffer {

        @Override
        public String name() {
            return card.name();
        }
    }

    /** Un objet ou un trinket, mis dans l'inventaire à l'achat. */
    record CollectibleOffer(Collectible collectible, int price) implements ShopOffer {

        @Override
        public String name() {
            return collectible.name();
        }
    }
}

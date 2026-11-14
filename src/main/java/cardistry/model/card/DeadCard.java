package cardistry.model.card;

/**
 * Carte morte « NO CARDS » (NullPointerException) : injouable, sans élément, elle occupe un
 * emplacement de la main jusqu'à la fin du combat. Les effets qui remplacent des cartes l'ignorent.
 */
public final class DeadCard extends Card {

    public static final String NAME = "NO CARDS";

    public DeadCard() {
        super(NAME, null);
    }

    @Override
    public DeadCard copy() {
        return new DeadCard();
    }
}

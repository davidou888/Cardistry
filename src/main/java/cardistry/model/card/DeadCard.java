package cardistry.model.card;

/**
 * Carte morte : injouable, sans élément, elle occupe un emplacement de la main jusqu'à la fin du
 * combat. « NO CARDS » (NullPointerException) ou « Exercice 3b » (Vafi). Les effets qui remplacent
 * des cartes l'ignorent.
 */
public final class DeadCard extends Card {

    public static final String NAME = "NO CARDS";

    public DeadCard() {
        this(NAME);
    }

    public DeadCard(String name) {
        super(name, null);
    }

    @Override
    public DeadCard copy() {
        return new DeadCard(name());
    }
}

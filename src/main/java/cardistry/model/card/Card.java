package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Une carte. Les sous-types sont fermés : sortilège, soin, terrain, et la carte morte (injouable). */
public abstract sealed class Card permits SpellCard, HealCard, TerrainCard, DeadCard {

    private final String name;
    private final Element element;
    private final Rarity rarity;

    protected Card(String name, Element element, Rarity rarity) {
        this.name = name;
        this.element = element;
        this.rarity = rarity;
    }

    public String name() {
        
        return name;
    }

    /** Élément de la carte ; {@code null} pour la carte morte ({@link DeadCard}). */
    public Element element() {
        return element;
    }

    /** Identifiant qualifié, distinct de celui d'un objet ou trinket du même nom : « carte:petit-petou ». */
    public String qualifiedId() {
        return "carte:" + cardistry.model.Ids.slug(name);
    }

    /**Rareté de la carte */
    public Rarity rarity() {
        return rarity;
    }
    /**
     * Nouvelle instance indépendante. Les cartes du registre sont des modèles : on ne met en main
     * que des copies, pour qu'une carte améliorée ou modifiée n'affecte pas les autres.
     */
    public abstract Card copy();
}

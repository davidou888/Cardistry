package cardistry.model.card;

import cardistry.model.Element;

/** Une carte jouable. Les sous-types sont fermés : sortilège, soin, terrain. */
public abstract sealed class Card permits SpellCard, HealCard, TerrainCard {

    private final String name;
    private final Element element;

    protected Card(String name, Element element) {
        this.name = name;
        this.element = element;
    }

    public String name() {
        return name;
    }

    public Element element() {
        return element;
    }

    /** Identifiant qualifié, distinct de celui d'un objet ou trinket du même nom : « carte:petit-petou ». */
    public String qualifiedId() {
        return "carte:" + cardistry.model.Ids.slug(name);
    }

    /**
     * Nouvelle instance indépendante. Les cartes du registre sont des modèles : on ne met en main
     * que des copies, pour qu'une carte améliorée ou modifiée n'affecte pas les autres.
     */
    public abstract Card copy();
}

package cardistry.model.card;

import cardistry.model.Element;

/** Une carte. Les sous-types sont fermés : sortilège, soin, terrain, et la carte morte (injouable). */
public abstract sealed class Card permits SpellCard, HealCard, TerrainCard, DeadCard {

    private final String name;
    private final Element element;

    protected Card(String name, Element element) {
        this.name = name;
        this.element = element;
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

    /**
     * Nouvelle instance indépendante. Les cartes du registre sont des modèles : on ne met en main
     * que des copies, pour qu'une carte améliorée ou modifiée n'affecte pas les autres.
     */
    public abstract Card copy();
}

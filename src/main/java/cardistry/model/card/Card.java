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
}

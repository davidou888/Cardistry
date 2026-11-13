package cardistry.model.card;

import cardistry.model.Element;

/** Terrain : remplace le terrain actuel et modifie l'attaque adverse. */
public final class TerrainCard extends Card {

    public TerrainCard(String name, Element element) {
        super(name, element);
    }

    @Override
    public TerrainCard copy() {
        return new TerrainCard(name(), element());
    }
}

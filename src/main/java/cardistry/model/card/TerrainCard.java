package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Terrain : remplace le terrain actuel et modifie l'attaque adverse. */
public final class TerrainCard extends Card {

    public TerrainCard(String name, Element element, Rarity rarity) {
        super(name, element, rarity);
    }

    @Override
    public TerrainCard copy() {
        return new TerrainCard(name(), element(), rarity());
    }
}

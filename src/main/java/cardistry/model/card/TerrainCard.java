package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Terrain : remplace le terrain actuel et modifie l'attaque adverse. */
public final class TerrainCard extends Card {

    public TerrainCard(String name, Element element, Rarity rarity) {
        super(name, element, rarity);
    }

    /** Copie avec un pouvoir (texte + effet après le changement de terrain). */
    public TerrainCard withPower(String text, java.util.function.Consumer<cardistry.core.EffectContext> effect) {
        return withPower(copy(), new CardPower(text, effect));
    }

    @Override
    public TerrainCard copy() {
        return samePower(this, new TerrainCard(name(), element(), rarity()));
    }
}

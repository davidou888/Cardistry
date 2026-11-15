package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Sortilège : inflige des dégâts à l'adversaire. */
public final class SpellCard extends Card {

    private final double damage;

    public SpellCard(String name, Element element, double damage, Rarity rarity) {
        super(name, element, rarity);
        this.damage = damage;
    }

    /** Dégâts de base (positifs), avant multiplicateurs. */
    public double damage() {
        return damage;
    }

    @Override
    public SpellCard copy() {
        return new SpellCard(name(), element(), damage, rarity());
    }
}

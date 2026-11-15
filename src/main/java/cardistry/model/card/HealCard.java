package cardistry.model.card;

import cardistry.items.Rarity;
import cardistry.model.Element;

/** Soin : rend des PV au joueur, plus efficace dans un terrain du même élément. */
public final class HealCard extends Card {

    private final double amount;

    public HealCard(String name, Element element, double amount, Rarity rarity) {
        super(name, element, rarity);
        this.amount = amount;
    }

    /** PV rendus de base, avant multiplicateurs. */
    public double amount() {
        return amount;
    }

    @Override
    public HealCard copy() {
        return new HealCard(name(), element(), amount, rarity());
    }
}

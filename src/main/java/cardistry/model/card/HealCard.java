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

    /** Copie avec un pouvoir (texte + effet après le soin). */
    public HealCard withPower(String text, java.util.function.Consumer<cardistry.core.EffectContext> effect) {
        return withPower(copy(), new CardPower(text, effect));
    }

    @Override
    public HealCard copy() {
        return samePower(this, new HealCard(name(), element(), amount, rarity()));
    }
}

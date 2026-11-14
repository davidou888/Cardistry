package cardistry.core.shop;

import cardistry.items.Collectible;
import cardistry.items.Rarity;
import cardistry.model.card.Card;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;

/**
 * Prix du shop, en Choomies. Objets et trinkets : prix de leur rareté ({@link Rarity#price()} :
 * 50 / 80 / 130 / 200). Cartes : une rareté déduite de leur puissance, au prix
 * {@link #CARD_PRICE_RATIO} de celui d'un objet de même rareté, arrondi à 5.
 */
public final class ShopPrices {

    /** Une carte coûte 60 % d'un objet de même rareté : 30 / 50 / 80 / 120. */
    public static final double CARD_PRICE_RATIO = 0.6;
    /** Retrait d'une carte : {@link #REMOVAL_BASE}, +{@link #REMOVAL_STEP} à chaque retrait de la partie. */
    public static final int REMOVAL_BASE = 75;
    public static final int REMOVAL_STEP = 25;
    /** Reroll : {@link #REROLL_BASE}, +{@link #REROLL_STEP} à chaque reroll dans ce shop. */
    public static final int REROLL_BASE = 20;
    public static final int REROLL_STEP = 10;

    private ShopPrices() {
    }

    public static int price(Collectible collectible) {
        return collectible.rarity().price();
    }

    /**
     * Rareté d'une carte : sort de 15+ ou soin de 15 = Peu commun ; soin de 20+ = Rare ; sorts et
     * soins de base, terrains = Commun.
     */
    public static Rarity cardRarity(Card card) {
        return switch (card) {
            case SpellCard spell -> spell.damage() >= 15 ? Rarity.UNCOMMON : Rarity.COMMON;
            case HealCard heal -> heal.amount() >= 20 ? Rarity.RARE : heal.amount() >= 15 ? Rarity.UNCOMMON : Rarity.COMMON;
            case TerrainCard terrain -> Rarity.COMMON;
            case DeadCard dead -> Rarity.COMMON;
        };
    }

    public static int price(Card card) {
        return (int) (Math.round(cardRarity(card).price() * CARD_PRICE_RATIO / 5.0) * 5);
    }

    /** Prix du prochain retrait, après {@code removals} retraits dans la partie. */
    public static int removal(int removals) {
        return REMOVAL_BASE + REMOVAL_STEP * removals;
    }

    /** Prix du prochain reroll, après {@code rerolls} rerolls dans ce shop. */
    public static int reroll(int rerolls) {
        return REROLL_BASE + REROLL_STEP * rerolls;
    }
}

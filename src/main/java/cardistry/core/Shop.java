package cardistry.core;

import cardistry.core.shop.ShopOffer;
import cardistry.core.shop.ShopPrices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le shop ouvert : {@link #CARDS} cartes, {@link #ITEMS} objets, {@link #TRINKETS} trinkets (dans
 * cet ordre), et le nombre de rerolls faits ici. Modifié par le moteur seulement.
 */
public final class Shop {

    public static final int CARDS = 5;
    public static final int ITEMS = 2;
    public static final int TRINKETS = 2;

    private final List<ShopOffer> offers = new ArrayList<>();
    private final List<Boolean> sold = new ArrayList<>();
    private int rerolls = 0;

    /** Articles, cartes puis objets puis trinkets (vendus compris). */
    public List<ShopOffer> offers() {
        return Collections.unmodifiableList(offers);
    }

    public boolean isSold(int index) {
        return sold.get(index);
    }

    public int rerolls() {
        return rerolls;
    }

    /** Prix du prochain reroll. */
    public int rerollPrice() {
        return ShopPrices.reroll(rerolls);
    }

    void restock(List<ShopOffer> stock) {
        offers.clear();
        offers.addAll(stock);
        sold.clear();
        stock.forEach(o -> sold.add(false));
    }

    void markSold(int index) {
        sold.set(index, true);
    }

    void countReroll() {
        rerolls++;
    }
}

package cardistry.items;

/** Rareté d'un objet ou trinket : poids de tirage et prix indicatif (voir GAME_DESIGN.md). */
public enum Rarity {
    COMMON("Commun", 60, 50),
    UNCOMMON("Peu commun", 25, 80),
    RARE("Rare", 12, 130),
    LEGENDARY("Légendaire", 1, 200),
    /** N'apparaît jamais dans les tirages normaux. */
    SECRET("Secret", 0, 0);

    private final String displayName;
    private final int dropWeight;
    private final int price;

    Rarity(String displayName, int dropWeight, int price) {
        this.displayName = displayName;
        this.dropWeight = dropWeight;
        this.price = price;
    }

    public String displayName() {
        return displayName;
    }

    /** Poids relatif dans les tirages de récompense (0 = jamais tiré). */
    public int dropWeight() {
        return dropWeight;
    }

    public int price() {
        return price;
    }
}

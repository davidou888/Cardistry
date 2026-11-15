package cardistry.ui;

import static cardistry.ui.Format.num;

import cardistry.model.card.Card;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;

/**
 * Textes d'une carte <b>hors combat</b> (valeurs de base, sans adversaire ni terrain du moment) :
 * récompenses, shop, deck. L'effet réel en combat est dans {@code CombatScreen.describe}.
 */
public final class CardText {

    private CardText() {
    }

    /** « sort feu », « soin plante », « terrain eau », « injouable ». */
    public static String kind(Card card) {
        return switch (card) {
            case SpellCard s -> "sort " + s.element().displayName();
            case HealCard h -> "soin " + h.element().displayName();
            case TerrainCard t -> "terrain " + t.element().displayName();
            case DeadCard d -> "injouable";
        };
    }

    /** Valeur de base : « 10 dégâts », « 1-40 dégâts », « +15 PV » ; vide pour un terrain. */
    public static String baseValue(Card card) {
        return switch (card) {
            case SpellCard s when s.damage() <= 0 && !s.hasDamageRule() -> "aucun dégât";
            case SpellCard s -> s.damageLabel().orElse(num(s.damage())) + " dégâts"
                    + (s.isUnmultiplied() ? " (non multipliés)" : "");
            case HealCard h -> "+" + num(h.amount()) + " PV";
            case TerrainCard t -> "";
            case DeadCard d -> "jusqu'à la fin du combat";
        };
    }

    /** Pouvoir (« ★ … ») ou effet de terrain ; vide s'il n'y en a pas. */
    public static String extra(Card card) {
        String power = card.power().map(p -> "★ " + p.text()).orElse("");
        String effect = card instanceof TerrainCard t ? t.effectText().filter(e -> !e.isEmpty()).orElse("") : "";
        return power.isEmpty() ? effect : effect.isEmpty() ? power : effect + " · " + power;
    }

    /** « Boule de feu · sort feu · Commun : 10 dégâts · ★ 10 % d'infliger Brûlure ». */
    public static String summary(Card card) {
        String rarity = card.rarity() == null ? "" : " · " + card.rarity().displayName();
        String value = baseValue(card);
        String extra = extra(card);
        return card.name() + " · " + kind(card) + rarity + (value.isEmpty() ? "" : " : " + value)
                + (extra.isEmpty() ? "" : " · " + extra);
    }

    /** Une carte du deck et son nombre d'exemplaires. */
    public record DeckEntry(Card card, int count) {
    }

    /** Un groupe du deck : « Sorts (6) » et ses cartes. */
    public record DeckGroup(String title, java.util.List<DeckEntry> entries) {
    }

    /**
     * Le deck regroupé par type (sorts, soins, terrains, cartes mortes), puis par rareté et par nom ;
     * les exemplaires d'une même carte sont réunis (×N).
     */
    public static java.util.List<DeckGroup> deckGroups(java.util.List<Card> deck) {
        java.util.List<DeckGroup> groups = new java.util.ArrayList<>();
        String[] titles = {"Sorts", "Soins", "Terrains", "Cartes mortes"};
        for (int type = 0; type < titles.length; type++) {
            java.util.Map<String, DeckEntry> byName = new java.util.LinkedHashMap<>();
            int total = 0;
            for (Card card : deck) {
                if (typeOrder(card) == type) {
                    byName.merge(card.name(), new DeckEntry(card, 1), (a, b) -> new DeckEntry(a.card(), a.count() + 1));
                    total++;
                }
            }
            if (byName.isEmpty()) {
                continue;
            }
            java.util.List<DeckEntry> entries = new java.util.ArrayList<>(byName.values());
            entries.sort(java.util.Comparator.comparing((DeckEntry e) -> e.card().rarity() == null ? 0
                    : e.card().rarity().ordinal()).thenComparing(e -> e.card().name()));
            groups.add(new DeckGroup(titles[type] + " (" + total + ")", entries));
        }
        return groups;
    }

    private static int typeOrder(Card card) {
        return switch (card) {
            case SpellCard s -> 0;
            case HealCard h -> 1;
            case TerrainCard t -> 2;
            case DeadCard d -> 3;
        };
    }

    /** « ×2 Boule de feu · sort feu · Commun : 10 dégâts · ★ … » (sans « ×1 »). */
    public static String deckLine(DeckEntry entry) {
        return (entry.count() > 1 ? "×" + entry.count() + " " : "") + summary(entry.card());
    }

    /** Le deck en lignes de texte (mode texte). */
    public static java.util.List<String> deckLines(java.util.List<Card> deck) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Ton deck : " + deck.size() + " cartes");
        for (DeckGroup group : deckGroups(deck)) {
            lines.add(group.title() + " :");
            for (DeckEntry entry : group.entries()) {
                lines.add("  " + deckLine(entry));
            }
        }
        return lines;
    }
}

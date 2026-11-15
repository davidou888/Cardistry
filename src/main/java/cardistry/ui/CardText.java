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
}

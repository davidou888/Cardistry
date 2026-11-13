package cardistry.items;

import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import java.util.List;

/**
 * Bonus de set : paliers débloqués selon le nombre de trinkets équipés portant {@link #tag()}.
 * Les paliers se cumulent.
 */
public final class SetBonus {

    /**
     * Un palier : {@code count} trinkets du tag. Les modificateurs sont déclarés sans source (elle
     * est fixée à l'application : « set EPFL 3 ») ; les hooks réagissent aux événements tant que le
     * palier est actif.
     */
    public record Tier(int count, String description, List<StatModifier> modifiers, GameHooks hooks) {

        public Tier {
            modifiers = List.copyOf(modifiers);
        }

        public Tier(int count, String description, List<StatModifier> modifiers) {
            this(count, description, modifiers, new GameHooks() {
            });
        }
    }

    private final Tag tag;
    private final String name;
    private final List<Tier> tiers;

    public SetBonus(Tag tag, String name, List<Tier> tiers) {
        this.tag = tag;
        this.name = name;
        this.tiers = List.copyOf(tiers);
    }

    public Tag tag() {
        return tag;
    }

    /** Nom du set (provisoire), vide s'il n'en a pas. */
    public String name() {
        return name;
    }

    /** « EPFL « Cursus » », ou juste « Soin ». */
    public String displayName() {
        return name.isEmpty() ? tag.displayName() : tag.displayName() + " « " + name + " »";
    }

    public List<Tier> tiers() {
        return tiers;
    }

    /** Source des modificateurs d'un palier dans les stats : « set EPFL 3 ». */
    public String source(Tier tier) {
        return "set " + tag.displayName() + " " + tier.count();
    }

    /** Paliers atteints avec {@code count} trinkets du tag. */
    public List<Tier> reached(int count) {
        return tiers.stream().filter(t -> count >= t.count()).toList();
    }

    /** Prochain palier non atteint, s'il y en a un. */
    public java.util.Optional<Tier> next(int count) {
        return tiers.stream().filter(t -> count < t.count()).findFirst();
    }
}

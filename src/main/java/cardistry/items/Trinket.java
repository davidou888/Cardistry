package cardistry.items;

import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Trinket : passif, actif tant qu'il est équipé. Deux leviers, combinables :
 * <ul>
 *   <li>des modificateurs de stats, ajoutés à l'équipement et retirés au déséquipement ;</li>
 *   <li>des hooks, appelés par le moteur à chaque événement.</li>
 * </ul>
 * Chaque trinket est une instance à part : un hook peut garder un état (compteur, « une fois par combat »).
 */
public final class Trinket implements Collectible {

    private final String id;
    private final String name;
    private final String description;
    private final String flavor;
    private final Rarity rarity;
    private final Set<Tag> tags;
    private final List<StatModifier> modifiers;
    private final GameHooks hooks;

    private Trinket(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.flavor = builder.flavor;
        this.rarity = builder.rarity;
        this.tags = Set.copyOf(builder.tags);
        this.modifiers = List.copyOf(builder.modifiers);
        this.hooks = builder.hooks;
    }

    public static Builder builder(String id, String name, Rarity rarity) {
        return new Builder(id, name, rarity);
    }

    /** Source des modificateurs de ce trinket dans {@link cardistry.core.stats.Stats}. */
    public String modifierSource() {
        return sourceOf(id);
    }

    static String sourceOf(String id) {
        return "trinket:" + id;
    }

    public List<StatModifier> modifiers() {
        return modifiers;
    }

    public GameHooks hooks() {
        return hooks;
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String flavor() { return flavor; }
    @Override public Rarity rarity() { return rarity; }
    @Override public Set<Tag> tags() { return tags; }
    @Override public String kindName() { return "Trinket"; }

    @Override
    public String toString() {
        return "Trinket[" + id + "]";
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final Rarity rarity;
        private String description = "";
        private String flavor = "";
        private final List<Tag> tags = new ArrayList<>();
        private final List<StatModifier> modifiers = new ArrayList<>();
        private GameHooks hooks = new GameHooks() {
        };

        private Builder(String id, String name, Rarity rarity) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder flavor(String flavor) {
            this.flavor = flavor;
            return this;
        }

        public Builder tags(Tag... tags) {
            this.tags.addAll(List.of(tags));
            return this;
        }

        /** Bonus plat : {@code flat(HAND_SIZE, 1)} = +1 taille de main. */
        public Builder flat(StatType stat, double value) {
            modifiers.add(StatModifier.flat(stat, value, sourceOf(id)));
            return this;
        }

        /** Bonus en pourcentage : {@code percent(FIRE_DAMAGE, 0.25)} = +25 % dégâts feu. */
        public Builder percent(StatType stat, double percent) {
            modifiers.add(StatModifier.percent(stat, percent, sourceOf(id)));
            return this;
        }

        /** Réactions aux événements. */
        public Builder hooks(GameHooks hooks) {
            this.hooks = hooks;
            return this;
        }

        public Trinket build() {
            return new Trinket(this);
        }
    }
}

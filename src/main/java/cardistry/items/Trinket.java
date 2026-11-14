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
    private final java.util.Map<String, Double> cardWeights;
    private final List<java.util.function.Supplier<cardistry.core.TemporaryEffect>> effects;
    private final Gauge gauge;
    private final java.util.Map<Tag, Double> tagBoosts;

    private Trinket(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.flavor = builder.flavor;
        this.rarity = builder.rarity;
        this.tags = Set.copyOf(builder.tags);
        this.modifiers = List.copyOf(builder.modifiers);
        this.hooks = builder.hooks;
        this.cardWeights = java.util.Map.copyOf(builder.cardWeights);
        this.effects = List.copyOf(builder.effects);
        this.gauge = builder.gauge;
        this.tagBoosts = java.util.Map.copyOf(builder.tagBoosts);
    }

    /** Jauge affichée par l'interface, s'il y en a une. */
    public java.util.Optional<Gauge> gauge() {
        return java.util.Optional.ofNullable(gauge);
    }

    /**
     * Nouveaux effets permanents posés quand on équipe ce trinket (retirés au déséquipement).
     * Ils sont actifs tout de suite ; {@code onActivate} n'est pas appelé.
     */
    public List<cardistry.core.TemporaryEffect> createEffects() {
        return effects.stream().map(java.util.function.Supplier::get).toList();
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

    /** Source des modificateurs posés dynamiquement par les hooks (gardés lors des recalculs). */
    public String dynamicSource() {
        return dynamicSourceOf(id);
    }

    static String dynamicSourceOf(String id) {
        return sourceOf(id) + ":dynamique";
    }

    /** Facteur appliqué aux trinkets portant {@code tag} (1 si ce trinket n'en amplifie pas). */
    public double boostFor(Tag tag) {
        return tagBoosts.getOrDefault(tag, 1.0);
    }

    public List<StatModifier> modifiers() {
        return modifiers;
    }

    public GameHooks hooks() {
        return hooks;
    }

    /** Facteur de fréquence de cette carte dans la pioche (1 si le trinket ne la change pas). */
    public double cardWeight(cardistry.model.card.Card card) {
        return cardWeights.getOrDefault(card.name(), 1.0);
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
        private final java.util.Map<String, Double> cardWeights = new java.util.HashMap<>();
        private final List<java.util.function.Supplier<cardistry.core.TemporaryEffect>> effects = new ArrayList<>();
        private Gauge gauge;
        private final java.util.Map<Tag, Double> tagBoosts = new java.util.EnumMap<>(Tag.class);

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

        /** Multiplicateur final : {@code multiply(HEAL_EFFICIENCY, 2)} = ×2 après tous les autres bonus. */
        public Builder multiply(StatType stat, double factor) {
            modifiers.add(StatModifier.multiply(stat, factor, sourceOf(id)));
            return this;
        }

        /**
         * Amplifie les autres trinkets portant {@code tag} (×{@code factor} sur leurs effets) et le
         * bonus de set de ce tag. Ne s'applique pas à ce trinket lui-même.
         */
        public Builder boostsTag(Tag tag, double factor) {
            tagBoosts.put(tag, factor);
            return this;
        }

        /** Jauge affichée par l'interface (les hooks la font avancer). */
        public Builder gauge(Gauge gauge) {
            this.gauge = gauge;
            return this;
        }

        /** La carte nommée {@code cardName} est {@code factor} fois plus fréquente dans la pioche. */
        public Builder cardWeight(String cardName, double factor) {
            cardWeights.put(cardName, factor);
            return this;
        }

        /** Effet permanent tant que le trinket est équipé (voir {@link cardistry.core.TemporaryEffect}). */
        public Builder effect(java.util.function.Supplier<cardistry.core.TemporaryEffect> effect) {
            effects.add(effect);
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

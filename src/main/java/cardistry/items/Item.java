package cardistry.items;

import cardistry.core.EffectContext;
import java.util.Set;
import java.util.function.Consumer;

/** Objet consommable : usage unique, action gratuite (ne termine pas le tour). */
public final class Item implements Collectible {

    private final String id;
    private final String name;
    private final String description;
    private final String flavor;
    private final Rarity rarity;
    private final Set<Tag> tags;
    private final Consumer<EffectContext> effect;

    public Item(String id, String name, String description, String flavor, Rarity rarity, Set<Tag> tags,
            Consumer<EffectContext> effect) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.flavor = flavor;
        this.rarity = rarity;
        this.tags = Set.copyOf(tags);
        this.effect = effect;
    }

    /** Applique l'effet de l'objet. */
    public void use(EffectContext context) {
        effect.accept(context);
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String flavor() { return flavor; }
    @Override public Rarity rarity() { return rarity; }
    @Override public Set<Tag> tags() { return tags; }
    @Override public String kindName() { return "Objet"; }

    @Override
    public String toString() {
        return "Item[" + id + "]";
    }
}

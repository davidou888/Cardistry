package cardistry.core;

import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import java.util.List;

/**
 * Malédiction : effet négatif qui dure un combat entier. Obtenue pendant un combat, elle s'active
 * au combat suivant et disparaît à la fin de celui-ci.
 */
public enum Curse {
    /** Obtenue en atteignant le seuil d'Ivresse. */
    HANGOVER("Gueule de bois", "−1 taille de main", List.of(new Mod(StatType.HAND_SIZE, -1)));

    private record Mod(StatType stat, double value) {
    }

    private final String displayName;
    private final String description;
    private final List<Mod> mods;

    Curse(String displayName, String description, List<Mod> mods) {
        this.displayName = displayName;
        this.description = description;
        this.mods = mods;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /** Source de ses modificateurs dans les stats. */
    public String modifierSource() {
        return "curse:" + name();
    }

    public List<StatModifier> modifiers() {
        return mods.stream().map(m -> StatModifier.flat(m.stat(), m.value(), modifierSource())).toList();
    }
}

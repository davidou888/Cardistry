package cardistry.core.stats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Stats calculées, jamais écrasées.
 *
 * <p>Ordre de calcul : {@code (base + somme des FLAT) × (1 + somme des PERCENT)}.
 * Les pourcentages s'additionnent : deux +25 % donnent +50 % (×1.5), pas ×1.5625.
 * Le multiplicateur total ne descend jamais sous 0.
 */
public final class Stats {

    private final Map<StatType, Double> base = new EnumMap<>(StatType.class);
    private final List<StatModifier> modifiers = new ArrayList<>();

    /** Stats aux valeurs de base par défaut, sans modificateur. */
    public Stats() {
        for (StatType stat : StatType.values()) {
            base.put(stat, stat.defaultBase());
        }
    }

    public double get(StatType stat) {
        double flat = 0;
        double percent = 0;
        for (StatModifier modifier : modifiers) {
            if (modifier.stat() != stat) {
                continue;
            }
            switch (modifier.kind()) {
                case FLAT -> flat += modifier.value();
                case PERCENT -> percent += modifier.value();
            }
        }
        return (base.get(stat) + flat) * Math.max(0, 1 + percent);
    }

    /** Valeur arrondie à l'entier inférieur, pour les stats entières (taille de main…). */
    public int getInt(StatType stat) {
        return (int) Math.floor(get(stat));
    }

    public double base(StatType stat) {
        return base.get(stat);
    }

    public void setBase(StatType stat, double value) {
        base.put(stat, value);
    }

    public void add(StatModifier modifier) {
        modifiers.add(modifier);
    }

    /** Retire tous les modificateurs apportés par {@code source}. */
    public void removeBySource(String source) {
        modifiers.removeIf(modifier -> modifier.source().equals(source));
    }

    public List<StatModifier> modifiers() {
        return List.copyOf(modifiers);
    }
}

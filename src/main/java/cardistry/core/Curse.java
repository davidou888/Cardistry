package cardistry.core;

import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import java.util.List;

/**
 * Malédiction : effet négatif qui dure un combat entier. Obtenue pendant un combat, elle s'active
 * au combat suivant et disparaît à la fin de celui-ci. Elle a une <b>intensité</b> (1 par défaut)
 * qui multiplie son effet ; à 0 elle disparaît (les clopes réduisent la Gueule de bois).
 */
public enum Curse {
    /** Obtenue en atteignant le seuil d'Ivresse : −1 taille de main par point d'intensité. */
    HANGOVER("Gueule de bois", StatType.HAND_SIZE, -1, "taille de main");

    private final String displayName;
    private final StatType stat;
    private final double perIntensity;
    private final String statLabel;

    Curse(String displayName, StatType stat, double perIntensity, String statLabel) {
        this.displayName = displayName;
        this.stat = stat;
        this.perIntensity = perIntensity;
        this.statLabel = statLabel;
    }

    public String displayName() {
        return displayName;
    }

    /** Effet à cette intensité : « −2 taille de main ». */
    public String description(int intensity) {
        double value = perIntensity * intensity;
        String sign = value < 0 ? "−" : "+";
        double abs = Math.abs(value);
        String number = abs == Math.rint(abs) ? Long.toString((long) abs) : Double.toString(abs);
        return sign + number + " " + statLabel;
    }

    /** Source de ses modificateurs dans les stats. */
    public String modifierSource() {
        return "curse:" + name();
    }

    public List<StatModifier> modifiers(int intensity) {
        return List.of(StatModifier.flat(stat, perIntensity * intensity, modifierSource()));
    }
}

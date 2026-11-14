package cardistry.ui;

import java.util.ArrayList;
import java.util.List;

/** Mise en forme de texte commune aux rendus. */
public final class Format {

    private Format() {
    }

    /** 15.0 → "15", 6.25 → "6.25". */
    public static String num(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /** " (×1.5)", ou "" si le multiplicateur vaut 1. */
    public static String mult(double multiplier) {
        return multiplier == 1.0 ? "" : " (×" + num(multiplier) + ")";
    }

    /** Étiquette du joueur en combat : le nom de son personnage en capitales, « TOI » sans personnage. */
    public static String playerLabel(cardistry.core.GameState state) {
        return state.hero().map(h -> h.name().toUpperCase(java.util.Locale.ROOT)).orElse("TOI");
    }

    public static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Découpe {@code text} en lignes d'au plus {@code width} caractères, par mots ;
     * un mot trop long est coupé avec un tiret.
     */
    public static List<String> wrap(String text, int width) {
        if (width < 2) {
            throw new IllegalArgumentException("Largeur trop petite : " + width);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            while (word.length() > width) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                lines.add(word.substring(0, width - 1) + "-");
                word = word.substring(width - 1);
            }
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Où en est la partie : « Niveau 2 · Étage 5/8 · Élite » sur la carte, « Combat 2/3 » avec des
     * adversaires fixés.
     */
    public static String location(cardistry.core.GameState state) {
        String rank = !state.hasEnemy() ? "" : switch (state.currentEnemy().rank()) {
            case COMMON -> "";
            case ELITE -> " · Élite";
            case BOSS -> " · Boss";
        };
        if (state.isOpenEnded()) {
            return "Niveau " + state.level() + " · Étage " + state.floor() + "/" + cardistry.run.map.RunMap.FLOORS + rank;
        }
        return "Combat " + state.combatNumber() + "/" + state.enemies().size() + rank;
    }

    /** « sort 10 », « soin 15 », « terrain », « injouable ». */
    public static String cardKind(cardistry.model.card.Card card) {
        return switch (card) {
            case cardistry.model.card.SpellCard spell -> spell.element().displayName() + ", " + num(spell.damage());
            case cardistry.model.card.HealCard heal -> "soin " + heal.element().displayName() + ", " + num(heal.amount());
            case cardistry.model.card.TerrainCard terrain -> "terrain " + terrain.element().displayName();
            case cardistry.model.card.DeadCard dead -> "injouable";
        };
    }
}

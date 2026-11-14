package cardistry.ui.sprite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sprite ASCII d'un adversaire : des animations ({@link #IDLE}, {@link #ATTACK}, {@link #HURT},
 * {@link #DEATH}), chacune faite de frames. Format des fichiers : voir {@link #parse} et le README.
 */
public final class Sprite {

    public static final String IDLE = "idle";
    public static final String ATTACK = "attack";
    public static final String HURT = "hurt";
    public static final String DEATH = "death";
    /** Animations connues, dans l'ordre de la visionneuse. */
    public static final List<String> ANIMATIONS = List.of(IDLE, ATTACK, HURT, DEATH);

    /** Taille maximale d'une frame (zone du sprite à l'écran de combat). */
    public static final int MAX_WIDTH = 30;
    public static final int MAX_HEIGHT = 10;

    /**
     * Une image : ses lignes, et sa carte de couleurs (même forme, une lettre par caractère ;
     * {@code null} = couleurs de la première frame du sprite).
     */
    public record Frame(List<String> lines, List<String> colors) {

        public Frame {
            lines = List.copyOf(lines);
            colors = colors == null ? null : List.copyOf(colors);
        }

        public int width() {
            return lines.stream().mapToInt(String::length).max().orElse(0);
        }

        /** Lettre de couleur au caractère ({@code col}, {@code row}), ou '.' (couleur par défaut). */
        public char colorAt(int col, int row) {
            if (colors == null || row >= colors.size()) {
                return '.';
            }
            String line = colors.get(row);
            return col < line.length() ? line.charAt(col) : '.';
        }
    }

    private final String name;
    private final Map<String, List<Frame>> animations;

    private Sprite(String name, Map<String, List<Frame>> animations) {
        this.name = name;
        this.animations = animations;
    }

    /** Nom affiché par la visionneuse ({@code @name}), ou {@code fallbackName}. */
    public String name() {
        return name;
    }

    /** Frames d'une animation (vide si le sprite n'en a pas). */
    public List<Frame> frames(String animation) {
        return animations.getOrDefault(animation, List.of());
    }

    /** Animations présentes dans le fichier. */
    public List<String> animations() {
        return List.copyOf(animations.keySet());
    }

    /** Frames idle (au moins une). */
    public List<Frame> idle() {
        return frames(IDLE);
    }

    /** Hauteur et largeur maximales sur toutes les frames. */
    public int height() {
        return animations.values().stream().flatMap(List::stream).mapToInt(f -> f.lines().size()).max().orElse(0);
    }

    public int width() {
        return animations.values().stream().flatMap(List::stream).mapToInt(Frame::width).max().orElse(0);
    }

    /**
     * Lit un sprite.
     * <pre>
     * &#64;name Caddie énervé       nom (facultatif)
     * &#64;# commentaire
     * &#64;frame idle               une frame de l'animation idle (2-3 frames idle : 2-3 blocs)
     * ...lignes du dessin...
     * &#64;colors                   facultatif : couleurs de la frame qui précède
     * ...une lettre par caractère : r g y b m c w k (majuscule = vif), . ou espace = défaut
     * &#64;frame attack / hurt / death
     * </pre>
     * Un fichier sans aucune directive est une seule frame idle. Les lignes vides en fin de frame
     * sont retirées (pas celles du début : elles décalent le dessin). Une frame sans {@code @colors} prend les couleurs de la première frame.
     */
    public static Sprite parse(String text, String fallbackName) {
        String name = fallbackName;
        Map<String, List<Frame>> animations = new LinkedHashMap<>();
        List<String> lines = text.lines().map(String::stripTrailing).toList();
        boolean directives = lines.stream().anyMatch(l -> l.startsWith("@frame"));
        if (!directives) {
            animations.put(IDLE, List.of(new Frame(trim(lines), null)));
            return new Sprite(name, animations);
        }
        String animation = null;
        List<String> art = new ArrayList<>();
        List<String> colors = null;
        List<String> firstColors = null;
        for (String line : lines) {
            if (line.startsWith("@name ")) {
                name = line.substring(6).strip();
            } else if (line.startsWith("@#")) {
                continue;
            } else if (line.startsWith("@frame")) {
                firstColors = add(animations, animation, art, colors, firstColors);
                animation = line.substring(6).strip();
                if (animation.isEmpty()) {
                    animation = IDLE;
                }
                art = new ArrayList<>();
                colors = null;
            } else if (line.startsWith("@colors")) {
                colors = new ArrayList<>();
            } else if (animation != null) {
                (colors != null ? colors : art).add(line);
            }
        }
        add(animations, animation, art, colors, firstColors);
        if (!animations.containsKey(IDLE)) {
            List<Frame> first = animations.values().iterator().next();
            animations.put(IDLE, List.of(first.get(0)));
        }
        return new Sprite(name, animations);
    }

    private static List<String> add(Map<String, List<Frame>> animations, String animation, List<String> art,
            List<String> colors, List<String> firstColors) {
        if (animation == null) {
            return firstColors;
        }
        List<String> own = colors != null ? colors : firstColors;
        animations.computeIfAbsent(animation, a -> new ArrayList<>()).add(new Frame(trim(art), own));
        return firstColors != null ? firstColors : colors;
    }

    private static List<String> trim(List<String> lines) {
        List<String> out = new ArrayList<>(lines);
        while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
            out.remove(out.size() - 1);
        }
        return Collections.unmodifiableList(out);
    }

    /** Sprite de secours : un cadre avec le nom. */
    public static Sprite placeholder(String name) {
        String label = " " + name + " ";
        int width = Math.min(MAX_WIDTH, Math.max(12, label.length() + 2));
        String border = "+" + "-".repeat(width - 2) + "+";
        String inner = label.length() > width - 2 ? label.substring(0, width - 2) : label;
        int left = (width - 2 - inner.length()) / 2;
        String middle = "|" + " ".repeat(left) + inner + " ".repeat(width - 2 - left - inner.length()) + "|";
        String blank = "|" + " ".repeat(width - 2) + "|";
        Map<String, List<Frame>> animations = new LinkedHashMap<>();
        animations.put(IDLE, List.of(new Frame(List.of(border, blank, middle, blank, border), null)));
        return new Sprite(name, animations);
    }
}

package cardistry.tools;

import cardistry.items.Collectible;
import cardistry.items.ItemRegistry;
import cardistry.items.Rarity;
import cardistry.model.Element;
import cardistry.model.Ids;
import cardistry.model.card.Card;
import cardistry.model.card.CardPool;
import cardistry.model.card.DeadCard;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.run.map.NodeType;
import cardistry.ui.window.CellFonts;
import cardistry.ui.window.CellPalette;
import cardistry.ui.window.Images;
import com.googlecode.lanterna.TextColor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Planche récapitulative des icônes ({@code ./gradlew planche} → {@code art/planche.png}) : chaque
 * icône agrandie avec son nom et son id, par catégorie et par rareté ; une icône absente est marquée
 * « à faire ». Lit le contenu dans les registres et les images dans {@code images/}.
 */
public final class ArtSheet {

    private static final int COLUMNS = 8;
    private static final int CELL_W = 196;
    private static final int ICON = 96;
    private static final int CELL_H = ICON + 52;
    private static final int MARGIN = 16;
    private static final int TITLE_H = 34;

    private record Entry(String name, String id, String path, Color frame) {
    }

    private ArtSheet() {
    }

    public static void main(String[] args) throws IOException {
        Map<String, List<Entry>> sections = sections();
        int rows = 0;
        for (List<Entry> entries : sections.values()) {
            rows += (entries.size() + COLUMNS - 1) / COLUMNS;
        }
        int width = MARGIN * 2 + COLUMNS * CELL_W;
        int height = MARGIN * 2 + sections.size() * TITLE_H + rows * CELL_H;
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(CellPalette.BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        CellFonts small = new CellFonts(1);
        CellFonts big = new CellFonts(2);
        int y = MARGIN;
        int missing = 0;
        for (Map.Entry<String, List<Entry>> section : sections.entrySet()) {
            g.setFont(big.main());
            g.setColor(color(TextColor.ANSI.YELLOW_BRIGHT));
            g.drawString(section.getKey(), MARGIN, y + 22);
            y += TITLE_H;
            List<Entry> entries = section.getValue();
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                int x = MARGIN + (i % COLUMNS) * CELL_W;
                int top = y + (i / COLUMNS) * CELL_H;
                int iconX = x + (CELL_W - ICON) / 2;
                g.setColor(color(TextColor.ANSI.BLACK));
                g.fillRect(iconX - 4, top, ICON + 8, ICON + 8);
                Optional<BufferedImage> image = Images.get(entry.path());
                if (image.isPresent()) {
                    int k = Math.max(1, ICON / Math.max(image.get().getWidth(), image.get().getHeight()));
                    int w = image.get().getWidth() * k;
                    int h = image.get().getHeight() * k;
                    g.drawImage(image.get(), iconX + (ICON - w) / 2, top + 4 + (ICON - h) / 2, w, h, null);
                } else {
                    missing++;
                    g.setFont(small.main());
                    g.setColor(color(TextColor.ANSI.BLACK_BRIGHT));
                    g.drawString("à faire", iconX + 24, top + 56);
                }
                g.setColor(entry.frame());
                g.drawRect(iconX - 4, top, ICON + 7, ICON + 7);
                g.drawRect(iconX - 3, top + 1, ICON + 5, ICON + 5);
                g.setFont(small.main());
                g.setColor(CellPalette.FOREGROUND);
                centered(g, fit(entry.name(), CELL_W / 7 - 1), x, top + ICON + 24);
                g.setColor(color(TextColor.ANSI.BLACK_BRIGHT));
                centered(g, fit(entry.id(), CELL_W / 7 - 1), x, top + ICON + 38);
            }
            y += ((entries.size() + COLUMNS - 1) / COLUMNS) * CELL_H;
        }
        g.dispose();
        File out = new File(args.length > 0 ? args[0] : "art/planche.png");
        ImageIO.write(sheet, "png", out);
        System.out.println(out + " : " + sections.values().stream().mapToInt(List::size).sum() + " icônes, dont "
                + missing + " à faire");
        enemies(new File(args.length > 1 ? args[1] : "art/planche-ennemis.png"));
    }

    private static final List<String> ANIMATIONS = List.of("idle", "attack", "hurt", "death");
    private static final int ENEMY_SCALE = 3;
    private static final int ENEMY_CELL = 64 * ENEMY_SCALE + 16;

    /** Planche des adversaires : une ligne par adversaire qui a des images, une case par frame d'animation. */
    private static void enemies(File out) throws IOException {
        record Frame(String label, BufferedImage image) {
        }
        Map<String, List<Frame>> rows = new LinkedHashMap<>();
        java.util.Set<String> sprites = new java.util.HashSet<>();
        for (String id : cardistry.model.enemy.EnemyRegistry.ids()) {
            cardistry.model.Enemy enemy = cardistry.model.enemy.EnemyRegistry.create(id, 1);
            List<Frame> frames = new ArrayList<>();
            for (String animation : ANIMATIONS) {
                for (int n = 1; ; n++) {
                    Optional<BufferedImage> image = Images.get("enemies/" + enemy.spriteId() + "." + animation + "." + n
                            + ".png");
                    if (image.isEmpty()) {
                        break;
                    }
                    frames.add(new Frame(animation + " " + n, image.get()));
                }
            }
            // Un sprite partagé (les Choomah, les trois boss) n'apparaît qu'une fois.
            if (!frames.isEmpty() && sprites.add(enemy.spriteId())) {
                rows.put(enemy.name() + " (" + enemy.spriteId() + ")", frames);
            }
        }
        int columns = rows.values().stream().mapToInt(List::size).max().orElse(1);
        int rowHeight = TITLE_H + ENEMY_CELL + 24;
        BufferedImage sheet = new BufferedImage(MARGIN * 2 + columns * ENEMY_CELL,
                MARGIN * 2 + Math.max(1, rows.size()) * rowHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(CellPalette.BACKGROUND);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        CellFonts small = new CellFonts(1);
        CellFonts big = new CellFonts(2);
        int y = MARGIN;
        for (Map.Entry<String, List<Frame>> row : rows.entrySet()) {
            g.setFont(big.main());
            g.setColor(color(TextColor.ANSI.YELLOW_BRIGHT));
            g.drawString(row.getKey(), MARGIN, y + 22);
            for (int i = 0; i < row.getValue().size(); i++) {
                Frame frame = row.getValue().get(i);
                int x = MARGIN + i * ENEMY_CELL;
                g.setColor(color(TextColor.ANSI.BLACK));
                g.fillRect(x, y + TITLE_H, ENEMY_CELL - 8, ENEMY_CELL - 8);
                g.drawImage(frame.image(), x + 4, y + TITLE_H + 4, frame.image().getWidth() * ENEMY_SCALE,
                        frame.image().getHeight() * ENEMY_SCALE, null);
                g.setFont(small.main());
                g.setColor(color(TextColor.ANSI.BLACK_BRIGHT));
                g.drawString(frame.label(), x + 4, y + TITLE_H + ENEMY_CELL + 6);
            }
            y += rowHeight;
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
        System.out.println(out + " : " + rows.size() + " adversaire(s) avec images");
    }

    private static Map<String, List<Entry>> sections() {
        Map<String, List<Entry>> sections = new LinkedHashMap<>();
        List<Entry> map = new ArrayList<>();
        for (NodeType type : NodeType.values()) {
            String id = type.name().toLowerCase(java.util.Locale.ROOT);
            map.add(new Entry(type.displayName(), id, "map/" + id + ".png", color(TextColor.ANSI.BLACK_BRIGHT)));
        }
        sections.put("Carte du monde (16×16)", map);
        sections.put("Objets (16×16)", collectibles(ItemRegistry.items()));
        sections.put("Trinkets (16×16)", collectibles(ItemRegistry.trinkets()));
        List<Card> cards = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Card card : new CardPool().cards()) {
            if (seen.add(card.name())) {
                cards.add(card);
            }
        }
        cards.add(new DeadCard());
        cards.add(new DeadCard("Exercice 3b"));
        cards.sort(Comparator.comparingInt(ArtSheet::typeOrder));
        List<Entry> cardEntries = new ArrayList<>();
        for (Card card : cards) {
            String id = Ids.slug(card.name());
            cardEntries.add(new Entry(card.name(), id, "cards/" + id + ".png", element(card.element())));
        }
        sections.put("Cartes (24×24) : sorts, soins, terrains, cartes mortes", cardEntries);
        return sections;
    }

    private static List<Entry> collectibles(List<? extends Collectible> all) {
        return all.stream().sorted(Comparator.comparing((Collectible c) -> c.rarity()))
                .map(c -> new Entry(c.name(), c.id(), "items/" + c.id() + ".png", rarity(c.rarity()))).toList();
    }

    private static int typeOrder(Card card) {
        return switch (card) {
            case SpellCard s -> 0;
            case HealCard h -> 1;
            case TerrainCard t -> 2;
            case DeadCard d -> 3;
        };
    }

    private static Color rarity(Rarity rarity) {
        return color(switch (rarity) {
            case COMMON -> TextColor.ANSI.WHITE_BRIGHT;
            case UNCOMMON -> TextColor.ANSI.GREEN_BRIGHT;
            case RARE -> TextColor.ANSI.BLUE_BRIGHT;
            case LEGENDARY -> TextColor.ANSI.YELLOW_BRIGHT;
            case SECRET -> TextColor.ANSI.MAGENTA_BRIGHT;
        });
    }

    private static Color element(Element element) {
        if (element == null) {
            return color(TextColor.ANSI.BLACK_BRIGHT);
        }
        return color(switch (element) {
            case FIRE -> TextColor.ANSI.RED_BRIGHT;
            case WATER -> TextColor.ANSI.BLUE_BRIGHT;
            case NATURE -> TextColor.ANSI.GREEN_BRIGHT;
            case NEUTRAL -> TextColor.ANSI.WHITE;
            case ALGEBRAIC -> TextColor.ANSI.CYAN_BRIGHT;
            case GEOMETRIC -> TextColor.ANSI.YELLOW;
            case SPIRIT -> TextColor.ANSI.MAGENTA_BRIGHT;
            case GLITCH -> TextColor.ANSI.BLACK_BRIGHT;
        });
    }

    private static Color color(TextColor color) {
        return CellPalette.foreground(color);
    }

    private static String fit(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 1) + "…";
    }

    private static void centered(Graphics2D g, String text, int x, int baseline) {
        g.drawString(text, x + (CELL_W - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }
}

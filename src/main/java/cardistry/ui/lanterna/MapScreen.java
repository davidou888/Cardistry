package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.run.map.MapNode;
import cardistry.run.map.MapView;
import cardistry.run.map.NodeType;
import cardistry.run.map.RunMap;
import cardistry.ui.MapArt;
import cardistry.ui.MapLayout;
import com.googlecode.lanterna.TextColor;

/**
 * Écran de la carte : le niveau, le chemin parcouru, et le choix du prochain nœud.
 *
 * <p>Dans la fenêtre (calque d'images), la carte va de gauche à droite : icônes pixel art, chemins
 * en pointillés courbes ({@link MapLayout}), nœud actuel cerclé d'or, nœuds accessibles qui pulsent.
 * En terminal (ou sans images), elle reste en lettres, du bas vers le haut ({@link MapArt}).
 */
final class MapScreen {

    static final String HELP = "←/→ choisir · Entrée y aller · I inventaire · D deck · C fiche · Q quitter";
    private static final int TOP = 3;
    private static final int LEFT = 4;

    private MapScreen() {
    }

    /** @param pulse compteur d'animation (les cercles des nœuds accessibles battent avec lui) */
    static void draw(Painter p, MapView view, int selected, long seed, String toast, int pulse) {
        if (p.hasImages()) {
            drawPixels(p, view, selected, seed, toast, pulse);
        } else {
            draw(p, view, selected, seed, toast);
        }
    }

    // --- Fenêtre : carte en pixels ---

    static final String HELP_PIXELS = "←/→ ↑/↓ choisir · Entrée y aller · I · D deck · C fiche · Q quitter";
    /** Lignes au-dessus de la carte (cadre, stats) et en dessous (prochain nœud, légende, cadre). */
    private static final int MAP_TOP_ROWS = 2;
    private static final int MAP_BOTTOM_ROWS = 4;
    private static final int MAP_SIDE_COLS = 3;
    /** Chemins : inatteignable (sombre), à venir (gris clair), ouvert depuis la position (texte). */
    private static final TextColor UNREACHABLE = new TextColor.RGB(0x4A, 0x48, 0x54);
    private static final TextColor AHEAD = new TextColor.RGB(0x9A, 0x96, 0xA2);

    /** Zone de la carte en unités du calque : x, y, largeur, hauteur. */
    static int[] mapArea(Painter p) {
        int x = MAP_SIDE_COLS * cardistry.ui.window.ImageLayer.UNITS_PER_COL;
        int y = MAP_TOP_ROWS * cardistry.ui.window.ImageLayer.UNITS_PER_ROW
                + cardistry.ui.window.ImageLayer.UNITS_PER_ROW / 2;
        return new int[] {x, y, p.unitWidth() - 2 * x,
                (p.rows() - MAP_BOTTOM_ROWS) * cardistry.ui.window.ImageLayer.UNITS_PER_ROW - y};
    }

    private static void drawPixels(Painter p, MapView view, int selected, long seed, String toast, int pulse) {
        RunMap map = view.map();
        p.frame("Carte · Niveau " + map.level() + "/3", "Étage " + view.state().floor() + "/" + RunMap.FLOORS
                + " · Seed " + seed, HELP_PIXELS);
        drawStats(p, view);
        MapNode chosen = view.choices().isEmpty() ? null
                : view.choices().get(Math.min(selected, view.choices().size() - 1));
        java.util.Set<MapNode> reachable = reachable(view);

        int[] area = mapArea(p);
        MapLayout layout = MapLayout.of(map, seed, area[2], area[3]);
        for (MapLayout.Path path : layout.paths()) {
            int from = view.visited().indexOf(path.from());
            boolean travelled = from >= 0 && from + 1 < view.visited().size()
                    && view.visited().get(from + 1).equals(path.to());
            boolean open = path.from().equals(view.position());
            boolean toChosen = open && path.to().equals(chosen);
            boolean ahead = reachable.contains(path.from());
            TextColor color = travelled ? Palette.ACCENT : toChosen ? TextColor.ANSI.WHITE_BRIGHT
                    : open ? Palette.TEXT : ahead ? AHEAD : UNREACHABLE;
            int dot = travelled || toChosen ? 4 : open || ahead ? 3 : 2;
            for (MapLayout.Point point : path.dots()) {
                p.fill(area[0] + point.x() - dot / 2, area[1] + point.y() - dot / 2, dot, dot, color);
            }
        }
        for (MapNode node : map.nodes()) {
            MapLayout.Point at = layout.position(node);
            int x = area[0] + at.x();
            int y = area[1] + at.y();
            int size = layout.size(node);
            boolean here = node.equals(view.position());
            boolean past = view.visited().contains(node) && !here;
            float alpha = past ? 0.45f : reachable.contains(node) ? 1f : 0.28f;
            boolean drawn = cardistry.ui.window.Images.mapNode(node.type())
                    .map(path -> p.stamp(x, y, size, path, alpha)).orElse(false);
            if (!drawn) { // icône absente : la lettre du nœud
                p.text(x / cardistry.ui.window.ImageLayer.UNITS_PER_COL,
                        y / cardistry.ui.window.ImageLayer.UNITS_PER_ROW, String.valueOf(node.type().symbol()),
                        past ? Palette.DIM : color(node.type()), true);
            }
            if (here) {
                ring(p, x, y, size / 2 + 5, Palette.ACCENT, false);
            } else if (node.equals(chosen)) {
                ring(p, x, y, size / 2 + 4 + pulse % 2, TextColor.ANSI.WHITE_BRIGHT, false);
            } else if (view.choices().contains(node)) {
                ring(p, x, y, size / 2 + 3 + pulse % 2, TextColor.ANSI.CYAN_BRIGHT, true);
            }
        }

        int infoRow = p.rows() - 3;
        if (chosen != null) {
            p.text(2, infoRow, Painter.fit("→ Étage " + chosen.floor() + " : " + chosen.type().displayName() + " — "
                    + hint(chosen.type()), p.cols() - 4), Palette.ACCENT, true);
        }
        if (toast != null) {
            p.text(2, p.rows() - 2, Painter.fit("★ " + toast, p.cols() - 4), Palette.ACCENT, true);
        } else {
            drawLegend(p, p.rows() - 2);
        }
    }

    /** Ligne 1 : PV, clopes, inventaire, Choomies. */
    private static void drawStats(Painter p, MapView view) {
        var state = view.state();
        String hp = "PV " + num(state.player().hp()) + "/" + num(state.maxHp());
        p.text(2, 1, hp, Palette.health(state.player().hp() / state.maxHp()));
        String middle = " · Clopes " + state.cigarettes() + "/" + state.cigaretteMax() + " · Trinkets "
                + state.trinkets().size() + "/" + state.trinketSlots() + " · Objets " + state.items().size() + "/"
                + state.itemSlots() + " · ";
        p.text(2 + hp.length(), 1, middle, Palette.TEXT);
        p.text(2 + hp.length() + middle.length(), 1, state.choomies() + " Choomies", Palette.ACCENT);
    }

    /** Légende discrète sur une ligne : icône + nom de chaque type de nœud. */
    private static void drawLegend(Painter p, int row) {
        int col = 2;
        for (NodeType type : NodeType.values()) {
            int at = col;
            boolean icon = cardistry.ui.window.Images.mapNode(type)
                    .map(path -> p.image(at, row, 2, 1, path, null, 0.8f)).orElse(false);
            if (!icon) {
                p.text(col, row, String.valueOf(type.symbol()), color(type));
            }
            p.text(col + 3, row, type.displayName(), Palette.DIM);
            col += 3 + type.displayName().length() + 2;
        }
    }

    /** Nœuds encore atteignables depuis la position (tous avant le premier choix). */
    static java.util.Set<MapNode> reachable(MapView view) {
        java.util.Set<MapNode> out = new java.util.HashSet<>();
        java.util.ArrayDeque<MapNode> todo = new java.util.ArrayDeque<>();
        if (view.position() == null) {
            todo.addAll(view.map().floor(1));
        } else {
            todo.add(view.position());
        }
        while (!todo.isEmpty()) {
            MapNode node = todo.poll();
            if (out.add(node)) {
                todo.addAll(view.map().choicesAfter(node));
            }
        }
        return out;
    }

    /** Cercle en gros pixels (2 unités), plein ou en tirets. */
    private static void ring(Painter p, int cx, int cy, int radius, TextColor color, boolean dashed) {
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int angle = 0; angle < 360; angle++) {
            if (dashed && (angle / 18) % 2 == 1) {
                continue;
            }
            int x = (int) Math.round((cx + radius * Math.cos(Math.toRadians(angle))) / 2) * 2;
            int y = (int) Math.round((cy + radius * Math.sin(Math.toRadians(angle))) / 2) * 2;
            if (done.add(((long) x << 32) | (y & 0xffffffffL))) {
                p.fill(x - 1, y - 1, 2, 2, color);
            }
        }
    }

    // --- Terminal : carte en lettres ---

    static void draw(Painter p, MapView view, int selected, long seed, String toast) {
        RunMap map = view.map();
        p.frame("Carte · Niveau " + map.level() + "/3", "Étage " + view.state().floor() + "/" + RunMap.FLOORS
                + " · Seed " + seed, HELP);
        char[][] grid = MapArt.grid(map);
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                char c = grid[row][col];
                if (c == '/' || c == '\\' || c == '|') {
                    p.text(LEFT + col, TOP + row, String.valueOf(c), Palette.DIM);
                }
            }
        }
        for (MapNode node : map.nodes()) {
            boolean visited = view.visited().contains(node);
            boolean choice = view.choices().contains(node);
            boolean isSelected = choice && view.choices().indexOf(node) == selected;
            boolean here = node.equals(view.position());
            int x = LEFT + MapArt.col(node);
            int y = TOP + MapArt.row(node);
            TextColor color = visited && !here ? Palette.DIM : color(node.type());
            p.text(x, y, String.valueOf(node.type().symbol()), color, choice || here);
            if (isSelected) {
                p.text(x - 1, y, "[", Palette.ACCENT, true);
                p.text(x + 1, y, "]", Palette.ACCENT, true);
            } else if (here) {
                p.text(x - 1, y, "(", Palette.ACCENT, true);
                p.text(x + 1, y, ")", Palette.ACCENT, true);
            } else if (choice) {
                p.text(x - 1, y, "·", Palette.INFO);
            }
        }
        drawSide(p, view, selected);
        if (toast != null) {
            p.text(LEFT, TOP + MapArt.HEIGHT + 1, Painter.fit("★ " + toast, p.cols() - LEFT - 2), Palette.ACCENT, true);
        }
    }

    private static void drawSide(Painter p, MapView view, int selected) {
        int col = LEFT + MapArt.WIDTH + 8;
        int row = TOP;
        p.text(col, row++, "Légende", Palette.TEXT, true);
        for (NodeType type : NodeType.values()) {
            p.text(col, row, String.valueOf(type.symbol()), color(type), true);
            p.text(col + 2, row++, type.displayName(), Palette.TEXT);
        }
        p.text(col, row++, "( ) toi   [ ] choix", Palette.DIM);
        row++;
        var state = view.state();
        p.text(col, row++, "PV " + num(state.player().hp()) + "/" + num(state.maxHp()), Palette.TEXT);
        p.text(col, row++, "Clopes " + state.cigarettes() + "/" + state.cigaretteMax(), Palette.TEXT);
        p.text(col, row++, state.choomies() + " Choomies", Palette.ACCENT);
        p.text(col, row++, "Trinkets " + state.trinkets().size() + "/" + state.trinketSlots()
                + " · Objets " + state.items().size() + "/" + state.itemSlots(), Palette.TEXT);
        row++;
        if (!view.choices().isEmpty()) {
            MapNode next = view.choices().get(Math.min(selected, view.choices().size() - 1));
            p.text(col, row++, "→ Étage " + next.floor() + " : " + next.type().displayName(), Palette.ACCENT, true);
            p.text(col, row, Painter.fit(hint(next.type()), p.cols() - col - 2), Palette.DIM);
        }
    }

    static String hint(NodeType type) {
        return switch (type) {
            case COMBAT -> "Un adversaire commun.";
            case ELITE -> "Plus dur, meilleure récompense.";
            case SHOP -> "Cartes, objets, trinkets.";
            case EVENT -> "Une rencontre, un choix.";
            case REST -> "Une pause avant le boss.";
            case BOSS -> "Le boss du niveau.";
        };
    }

    static TextColor color(NodeType type) {
        return switch (type) {
            case COMBAT -> Palette.TEXT;
            case ELITE -> Palette.DAMAGE;
            case SHOP -> Palette.ACCENT;
            case EVENT -> Palette.INFO;
            case REST -> Palette.HEAL;
            case BOSS -> Palette.SECRET;
        };
    }
}

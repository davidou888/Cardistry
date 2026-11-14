package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.run.map.MapNode;
import cardistry.run.map.MapView;
import cardistry.run.map.NodeType;
import cardistry.run.map.RunMap;
import cardistry.ui.MapArt;
import com.googlecode.lanterna.TextColor;

/** Écran de la carte : le niveau, le chemin parcouru, et le choix du prochain nœud. */
final class MapScreen {

    static final String HELP = "←/→ choisir · Entrée y aller · I inventaire · Q quitter";
    private static final int TOP = 3;
    private static final int LEFT = 4;

    private MapScreen() {
    }

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
            case EVENT -> "Bientôt : un événement.";
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

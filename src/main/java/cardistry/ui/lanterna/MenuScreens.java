package cardistry.ui.lanterna;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.model.Element;
import cardistry.model.ElementRules;
import cardistry.model.Enemy;
import cardistry.ui.Format;
import java.util.List;

/** Écrans hors combat : titre, règles, fin de partie, terminal trop petit. */
final class MenuScreens {

    static final int MIN_COLS = 80;
    static final int MIN_ROWS = 24;

    private MenuScreens() {
    }

    /** Entrées du menu de l'écran titre. */
    static final List<String> TITLE_ENTRIES = List.of("Jouer", "Règles", "Options", "Quitter");

    static void title(Painter p, long seed, List<cardistry.run.Secret> codes) {
        title(p, seed, codes, 0);
    }

    static void title(Painter p, long seed, List<cardistry.run.Secret> codes, int selected) {
        p.frame("Cardistry", "Seed " + seed, "↑/↓ choisir · Entrée valider · R règles · Q quitter");
        List<String> banner = Resources.banner("title");
        int width = banner.stream().mapToInt(String::length).max().orElse(0);
        int top = Math.max(2, p.rows() / 2 - 7);
        for (int i = 0; i < banner.size(); i++) {
            p.text((p.cols() - width) / 2, top + i, banner.get(i), Palette.ACCENT, true);
        }
        int row = top + banner.size() + 2;
        p.centered(row, "Un jeu de cartes élémentaire, au tour par tour", Palette.TEXT, false);
        List<String> elements = elementCycles();
        for (int i = 0; i < elements.size(); i++) {
            p.centered(row + 2 + i, elements.get(i), Palette.DIM, false);
        }
        row += elements.size() - 1; // le menu descend d'autant
        StringBuilder menu = new StringBuilder();
        int selectedCol = 0;
        for (int i = 0; i < TITLE_ENTRIES.size(); i++) {
            if (i > 0) {
                menu.append("     ");
            }
            if (i == selected) {
                selectedCol = menu.length();
            }
            menu.append(i == selected ? "▶ " : "  ").append(TITLE_ENTRIES.get(i));
        }
        int menuCol = Math.max(1, (p.cols() - menu.length()) / 2);
        p.text(menuCol, row + 5, menu.toString(), Palette.TEXT, false);
        String chosen = "▶ " + TITLE_ENTRIES.get(selected);
        p.text(menuCol + selectedCol, row + 5, chosen, Palette.ACCENT, true);
        for (int i = 0; i < codes.size(); i++) {
            p.centered(row + 7 + i, "✦ Code accepté : " + codes.get(i).displayName() + " équipé pour cette partie",
                    Palette.SECRET, true);
        }
    }

    /** Image d'un personnage ({@code player/<id>.png}), sinon le placeholder commun. */
    static String heroImage(cardistry.hero.Hero hero) {
        String path = "player/" + hero.id() + ".png";
        return cardistry.ui.window.Images.get(path).isPresent() ? path : CombatWindowScreen.PLAYER_IMAGE;
    }

    /** Choix du personnage : une colonne par personnage (image dans la fenêtre), capacité dessous. */
    static void heroes(Painter p, List<cardistry.hero.Hero> heroes, int selected, long seed) {
        p.frame("Choisis ton personnage", "Seed " + seed, "←/→ choisir · 1-" + heroes.size()
                + " · Entrée valider · Échap retour");
        int width = Math.min(30, (p.cols() - 4 - 2 * (heroes.size() - 1)) / heroes.size());
        int left = (p.cols() - (heroes.size() * width + 2 * (heroes.size() - 1))) / 2;
        // Fenêtre : portrait carré au-dessus du nom, aussi grand que la hauteur le permet.
        int portraitRows = p.hasImages() ? Math.max(0, Math.min(p.rows() - 20, (width - 4) / 2)) : 0;
        int top = Math.max(2, (p.rows() - (portraitRows + 14)) / 2);
        for (int i = 0; i < heroes.size(); i++) {
            cardistry.hero.Hero hero = heroes.get(i);
            boolean isSelected = i == selected;
            int col = left + i * (width + 2);
            int height = Math.min(p.rows() - top - 2, portraitRows + 14);
            p.box(col, top, width, height, isSelected ? Palette.ACCENT : Palette.FRAME, isSelected);
            int row = top + 1;
            if (portraitRows >= 3) {
                p.image(col + (width - portraitRows * 2) / 2, row, portraitRows * 2, portraitRows, heroImage(hero),
                        null, isSelected ? 1f : 0.45f);
                row += portraitRows;
            }
            String name = (isSelected ? "▶ " : "") + "[" + (i + 1) + "] " + hero.name();
            p.text(col + Math.max(1, (width - name.length()) / 2), row, name,
                    isSelected ? Palette.ACCENT : Palette.TEXT, true);
            double hp = cardistry.core.GameState.newGame(hero).maxHp();
            String stats = num(hp) + " PV";
            p.text(col + (width - stats.length()) / 2, row + 1, stats, Palette.HEAL);
            String ability = Painter.fit(hero.abilityName(), width - 2);
            p.text(col + (width - ability.length()) / 2, row + 3, ability, Palette.SECRET, true);
            List<String> lines = Format.wrap(hero.description(), width - 4);
            for (int k = 0; k < lines.size() && row + 5 + k < top + height - 1; k++) {
                p.text(col + 2, row + 5 + k, lines.get(k), isSelected ? Palette.TEXT : Palette.DIM);
            }
        }
    }

    /**
     * Les éléments en cycles, une ligne par cycle : « Feu > Plante > Eau > Feu », puis les éléments qui
     * battent tout (« Error: null bat tout »).
     */
    static List<String> elementCycles() {
        List<String> lines = new java.util.ArrayList<>();
        java.util.Set<Element> seen = java.util.EnumSet.noneOf(Element.class);
        for (Element start : Element.PLAYABLE) {
            if (start.beatsEverything() || seen.contains(start)) {
                continue;
            }
            StringBuilder line = new StringBuilder(Format.capitalize(start.displayName()));
            Element current = start;
            while (true) {
                seen.add(current);
                Element from = current;
                java.util.Optional<Element> next = Element.PLAYABLE.stream()
                        .filter(e -> e != from && !e.beatsEverything() && from.beats(e)).findFirst();
                if (next.isEmpty()) {
                    break;
                }
                line.append(" > ").append(Format.capitalize(next.get().displayName()));
                current = next.get();
                if (current == start) {
                    break;
                }
            }
            lines.add(line.toString());
        }
        for (Element element : Element.PLAYABLE) {
            if (element.beatsEverything()) {
                lines.add(Format.capitalize(element.displayName()) + " bat tout");
            }
        }
        return lines;
    }

    private static String elementsLine() {
        StringBuilder line = new StringBuilder();
        for (Element winner : Element.PLAYABLE) {
            if (winner.beatsEverything()) {
                continue; // résumé à la fin : « X bat tout »
            }
            for (Element loser : Element.PLAYABLE) {
                if (winner.beats(loser)) {
                    if (!line.isEmpty()) {
                        line.append(" · ");
                    }
                    line.append(Format.capitalize(winner.displayName())).append(" > ")
                            .append(Format.capitalize(loser.displayName()));
                }
            }
        }
        for (Element winner : Element.PLAYABLE) {
            if (winner.beatsEverything()) {
                line.append(" · ").append(Format.capitalize(winner.displayName())).append(" bat tout");
            }
        }
        return line.toString();
    }

    static void rules(Painter p, GameState state) {
        p.frame("Règles", "", "Entrée commencer · Q quitter");
        int row = 2;
        row = paragraph(p, 3, row, "Trois niveaux de 8 étages : choisis ton chemin sur la carte, un boss attend"
                + " au bout de chaque niveau. L'adversaire annonce son prochain coup.", true);
        row++;
        row = paragraph(p, 3, row, "À chaque tour, joue une carte ou pioche. Puis l'adversaire attaque.", true);
        row = paragraph(p, 5, row, "• Sortilège : inflige des dégâts à l'adversaire.", false);
        row = paragraph(p, 5, row, "• Soin : te rend des PV (jusqu'à tes PV max).", false);
        row = paragraph(p, 5, row, "• Terrain : change le terrain, ce qui modifie l'attaque adverse.", false);
        row++;
        row = paragraph(p, 3, row, "Éléments : " + elementsLine(), true);
        row = paragraph(p, 5, row, "• Sort qui bat l'adversaire ×" + num(ElementRules.ADVANTAGE)
                + ", sort battu ×" + num(ElementRules.DISADVANTAGE) + ".", false);
        row = paragraph(p, 5, row, "• Adversaire sur un terrain de son élément : attaque ×"
                + num(ElementRules.TERRAIN_STRENGTHENS) + " ; sur un terrain qui le bat : ×"
                + num(ElementRules.TERRAIN_WEAKENS) + ".", false);
        row = paragraph(p, 5, row, "• Soin sur un terrain de son élément ×" + num(ElementRules.HEAL_SAME_TERRAIN)
                + " ; sur un terrain qui le bat ×" + num(ElementRules.HEAL_HOSTILE_TERRAIN) + ".", false);
        row++;
        row = paragraph(p, 3, row, "Clopes : F fume (gratuit, +5 PV, −1 Ivresse, réduit la Gueule de bois), G roule (+1, fin du tour).",
                false);
        row = paragraph(p, 3, row, "Ivresse (boissons) : Pompette +10 % dégâts, Bourré +25 %, Cuite +50 % mais Gueule de"
                + " bois au combat suivant.", false);
        paragraph(p, 3, row, "En combat, le bas de l'écran montre l'effet réel de la carte sélectionnée.", false);
    }

    /** Texte découpé pour tenir dans le cadre ; les lignes suivantes sont alignées après la puce. */
    private static int paragraph(Painter p, int col, int row, String text, boolean bold) {
        int indent = text.startsWith("• ") ? 2 : 0;
        List<String> lines = Format.wrap(text, p.cols() - col - 2);
        for (int i = 0; i < lines.size(); i++) {
            p.text(col + (i == 0 ? 0 : indent), row + i, lines.get(i), Palette.TEXT, bold);
        }
        return row + lines.size();
    }

    static void gameOver(Painter p, GameState state, long seed, List<String> unlockedSecrets) {
        boolean won = state.isWon();
        p.frame("Cardistry", "Seed " + seed, "Appuie sur une touche pour revenir au titre");
        int top = Math.max(2, p.rows() / 2 - 5);
        String title = won ? "★  VICTOIRE  ★" : "—  DÉFAITE  —";
        p.centered(top, title, won ? Palette.ACCENT : Palette.DAMAGE, true);
        p.centered(top + 2, won ? "Tu as vaincu tous les adversaires." : "Tu n'as plus de points de vie.",
                Palette.TEXT, false);
        long defeated = state.enemies().stream().filter(e -> !e.isAlive()).count();
        p.centered(top + 4, "Tours joués : " + state.turn(), Palette.TEXT, false);
        p.centered(top + 5, state.isOpenEnded()
                ? "Adversaires vaincus : " + defeated + " · atteint : niveau " + state.level() + ", étage " + state.floor()
                : "Adversaires vaincus : " + defeated + "/" + state.enemies().size(), Palette.TEXT, false);
        p.centered(top + 6, "PV restants : " + num(Math.max(0, state.player().hp())) + "/" + num(state.maxHp()),
                Palette.TEXT, false);
        int row = top + 8;
        for (String secret : unlockedSecrets) {
            p.centered(row++, "✦ Secret débloqué : " + secret + " ✦", Palette.SECRET, true);
        }
        p.centered(unlockedSecrets.isEmpty() ? row : row + 1, "Rejouer cette partie : --seed " + seed, Palette.DIM, false);
    }

    /** Sieste : une pause avant le boss, un choix parmi les options. */
    static void rest(Painter p, GameState state, List<cardistry.run.RestOption> options, int selected, long seed) {
        p.frame("Sieste", "Niveau " + state.level() + " · Seed " + seed, "↑/↓ choisir · Entrée valider · Q quitter");
        int top = Math.max(2, p.rows() / 2 - 8);
        String[] bed = {
            "        z Z z",
            "   _____________",
            "  |\\  (-_-)    \\",
            "  | \\___________\\",
            "  |  |___________|",
        };
        int bedWidth = java.util.Arrays.stream(bed).mapToInt(String::length).max().orElse(0);
        for (int i = 0; i < bed.length; i++) {
            p.text((p.cols() - bedWidth) / 2, top + i, bed[i], Palette.HEAL);
        }
        int row = top + bed.length + 2;
        p.centered(row, "Une pause avant le boss. PV " + num(state.player().hp()) + "/" + num(state.maxHp())
                + " · Clopes " + state.cigarettes() + "/" + state.cigaretteMax(), Palette.TEXT, false);
        row += 2;
        for (int i = 0; i < options.size(); i++) {
            cardistry.run.RestOption option = options.get(i);
            String line = (i == selected ? "> " : "  ") + "[" + (i + 1) + "] " + option.displayName() + " : "
                    + option.description();
            p.centered(row + i * 2, line, i == selected ? Palette.ACCENT : Palette.TEXT, i == selected);
        }
    }

    /** Menu pause. */
    static void pause(Painter p, int selected, long seed) {
        p.frame("Pause", "Seed " + seed, "↑/↓ choisir · Entrée valider · Échap reprendre");
        int top = Math.max(3, p.rows() / 2 - 4);
        p.centered(top, "— PAUSE —", Palette.ACCENT, true);
        for (int i = 0; i < LanternaUi.PAUSE_ENTRIES.size(); i++) {
            boolean isSelected = i == selected;
            p.centered(top + 3 + i * 2, (isSelected ? "▶ " : "  ") + LanternaUi.PAUSE_ENTRIES.get(i) + "  ",
                    isSelected ? Palette.ACCENT : Palette.TEXT, isSelected);
        }
    }

    /** Options : volumes et muet (appliqués en direct, sauvegardés). */
    static void options(Painter p, cardistry.ui.Settings settings, int selected, long seed) {
        p.frame("Options", "Seed " + seed, "↑/↓ choisir · ←/→ régler · Entrée valider · Échap retour");
        int top = Math.max(3, p.rows() / 2 - 5);
        p.centered(top, "— OPTIONS —", Palette.ACCENT, true);
        String[] labels = {
            "Musique  " + gauge(settings.music()),
            "Effets   " + gauge(settings.effects()),
            "Muet     " + (settings.muted() ? "[x] oui" : "[ ] non"),
            "Retour",
        };
        int width = java.util.Arrays.stream(labels).mapToInt(String::length).max().orElse(0) + 2;
        int col = Math.max(2, (p.cols() - width) / 2);
        for (int i = 0; i < labels.length; i++) {
            boolean isSelected = i == selected;
            p.text(col, top + 3 + i * 2, (isSelected ? "▶ " : "  ") + labels[i],
                    isSelected ? Palette.ACCENT : Palette.TEXT, isSelected);
        }
        if (settings.muted()) {
            p.centered(top + 12, "Son coupé : les volumes sont gardés pour plus tard.", Palette.DIM, false);
        }
    }

    /** « ◀ ██████░░░░ ▶ 6/10 ». */
    static String gauge(int level) {
        return "◀ " + "█".repeat(level) + "░".repeat(cardistry.ui.Settings.MAX_LEVEL - level) + " ▶ " + level + "/"
                + cardistry.ui.Settings.MAX_LEVEL;
    }

    static void tooSmall(Painter p) {
        p.text(0, 0, "Terminal trop petit : " + p.cols() + "×" + p.rows(), Palette.WARNING, true);
        p.text(0, 1, "Agrandis-le à " + MIN_COLS + "×" + MIN_ROWS + " minimum.", Palette.TEXT);
        p.text(0, 2, "(Q pour quitter)", Palette.DIM);
    }
}

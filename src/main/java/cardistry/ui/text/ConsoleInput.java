package cardistry.ui.text;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.items.Collectible;
import cardistry.items.Trinket;
import cardistry.run.Secret;
import cardistry.ui.Input;
import cardistry.ui.ItemKeys;
import cardistry.ui.TitleChoice;
import java.util.List;

public final class ConsoleInput implements Input {

    private final Console console;

    public ConsoleInput(Console console) {
        this.console = console;
    }

    /** Commande d'annulation du dernier tour (trinket Ctrl+Z) : Ctrl+Z est pris par le terminal. */
    static final String UNDO = "z";

    /** Code secret de l'écran titre (CLAUDE.md). En mode texte, Ctrl+Z n'est pas lisible. */
    static final String CLAUDE_CODE = "claude";

    @Override
    public TitleChoice title(long seed) {
        console.println("Bienvenue dans " + Ansi.yellow("Cardistry") + " !");
        console.println("Seed : " + seed);
        console.println();
        List<Secret> codes = new java.util.ArrayList<>();
        while (true) {
            console.println("(Appuie sur Entrée pour continuer, ou s puis Entrée pour passer l'intro)");
            String line = console.readLine().trim();
            if (line.equalsIgnoreCase(CLAUDE_CODE)) {
                if (!codes.contains(Secret.CLAUDE_MD)) {
                    codes.add(Secret.CLAUDE_MD);
                }
                console.println(Ansi.MAGENTA + "Code accepté : " + Secret.CLAUDE_MD.displayName()
                        + " équipé pour cette partie." + Ansi.END_COLOR);
                continue;
            }
            return new TitleChoice(!line.equalsIgnoreCase("s"), codes);
        }
    }

    @Override
    public void waitForContinue() {
        console.println("(Appuie sur Entrée)");
        console.readLine();
    }

    /** 0 = piocher, n = jouer la carte n, a/z/e = utiliser un objet. Redemande si la saisie est invalide. */
    @Override
    public Action nextAction(GameState state) {
        while (true) {
            console.print("Que veux-tu jouer ? ");
            String line = console.readLine().trim();
            if (line.equalsIgnoreCase(UNDO) && state.hasTrinket(cardistry.items.ItemRegistry.CTRL_Z)) {
                return new Action.Undo();
            }
            if (line.equalsIgnoreCase("f")) {
                return new Action.Smoke();
            }
            if (line.equalsIgnoreCase("g")) {
                return new Action.RollCigarette();
            }
            if (line.length() == 1 && ItemKeys.slot(line.charAt(0), state.itemSlots()) >= 0) {
                return new Action.UseItem(ItemKeys.slot(line.charAt(0), state.itemSlots()));
            }
            try {
                int choice = Integer.parseInt(line);
                return choice == 0 ? new Action.DrawCard() : new Action.PlayCard(choice - 1);
            } catch (NumberFormatException e) {
                console.println("Entre un nombre entre 0 et " + state.handSize()
                        + (state.items().isEmpty() ? "" : ", ou la lettre d'un objet"));
            }
        }
    }

    /** 1..n = prendre, 0 = refuser ; si l'inventaire est plein, demande quoi remplacer. */
    @Override
    public RewardChoice chooseReward(GameState state, List<Collectible> options) {
        while (true) {
            int choice = readNumber("Ton choix (0 pour refuser) ? ", 0, options.size());
            if (choice == 0) {
                return new RewardChoice.Skip();
            }
            Collectible taken = options.get(choice - 1);
            List<? extends Collectible> owned = taken instanceof Trinket ? state.trinkets() : state.items();
            int slots = taken instanceof Trinket ? state.trinketSlots() : state.itemSlots();
            if (owned.size() < slots) {
                return RewardChoice.Take.into(choice - 1);
            }
            console.println("Inventaire plein. Que remplace " + taken.name() + " ?");
            for (int i = 0; i < owned.size(); i++) {
                console.println("[" + (i + 1) + "] " + owned.get(i).name() + " : " + owned.get(i).description());
            }
            int replaced = readNumber("Numéro (0 pour revenir) ? ", 0, owned.size());
            if (replaced > 0) {
                return new RewardChoice.Take(choice - 1, replaced - 1);
            }
        }
    }

    /** 1..n : nœud de la carte (la liste est affichée par le rendu de la carte). */
    @Override
    public int chooseNode(cardistry.run.map.MapView view) {
        if (view.choices().size() == 1) {
            console.println("(Appuie sur Entrée pour y aller)");
            console.readLine();
            return 0;
        }
        return readNumber("Où vas-tu ? ", 1, view.choices().size()) - 1;
    }

    private boolean metaPhraseShown = false;

    @Override
    public int chooseDrawnCard(GameState state, List<cardistry.model.card.Card> options) {
        console.println();
        console.println("Pioche : choisis la carte à garder (les autres sont défaussées).");
        if (!metaPhraseShown) {
            console.println("« Un jeu de cartes dans ton jeu de cartes. »");
            metaPhraseShown = true;
        }
        for (int i = 0; i < options.size(); i++) {
            console.println("[" + (i + 1) + "] " + TextRenderer.describe(options.get(i)));
        }
        return readNumber("Carte à garder ? ", 1, options.size()) - 1;
    }

    @Override
    public int chooseTrinketToDiscard(GameState state) {
        console.println("Tu as " + state.trinkets().size() + " trinkets pour " + state.trinketSlots()
                + " emplacements : choisis-en un à jeter.");
        for (int i = 0; i < state.trinkets().size(); i++) {
            Trinket trinket = state.trinkets().get(i);
            console.println("[" + (i + 1) + "] " + trinket.name() + " : " + trinket.description());
        }
        return readNumber("Trinket à jeter ? ", 1, state.trinkets().size()) - 1;
    }

    private int readNumber(String prompt, int min, int max) {
        while (true) {
            console.print(prompt);
            try {
                int value = Integer.parseInt(console.readLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // redemande
            }
            console.println("Entre un nombre entre " + min + " et " + max);
        }
    }
}

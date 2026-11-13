package cardistry.ui.text;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.core.RewardChoice;
import cardistry.items.Collectible;
import cardistry.items.Trinket;
import cardistry.ui.Input;
import cardistry.ui.ItemKeys;
import java.util.List;

public final class ConsoleInput implements Input {

    private final Console console;

    public ConsoleInput(Console console) {
        this.console = console;
    }

    @Override
    public boolean wantsIntro() {
        console.println("(Appuie sur Entrée pour continuer, ou s puis Entrée pour passer l'intro)");
        return !console.readLine().trim().equalsIgnoreCase("s");
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

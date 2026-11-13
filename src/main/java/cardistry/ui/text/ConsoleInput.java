package cardistry.ui.text;

import cardistry.core.Action;
import cardistry.core.GameState;
import cardistry.ui.Input;

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

    /** 0 = piocher, n = jouer la carte n. Redemande tant que la saisie n'est pas un nombre. */
    @Override
    public Action nextAction(GameState state) {
        while (true) {
            console.print("Que veux-tu jouer ? ");
            String line = console.readLine().trim();
            try {
                int choice = Integer.parseInt(line);
                return choice == 0 ? new Action.DrawCard() : new Action.PlayCard(choice - 1);
            } catch (NumberFormatException e) {
                console.println("Entre un nombre entre 0 et " + state.handSize());
            }
        }
    }
}

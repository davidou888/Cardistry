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
        console.println("(Appuie sur Entrée pour continuer or s to skip)");
        return !console.readLine().equals("s");
    }

    @Override
    public void waitForContinue() {
        console.println("(Appuie sur Entrée)");
        console.readLine();
    }

    @Override
    public Action nextAction(GameState state) {
        console.print("Que veux-tu jouer ? ");
        int choice = Integer.parseInt(console.readLine().trim());
        return choice == 0 ? new Action.DrawCard() : new Action.PlayCard(choice - 1);
    }
}

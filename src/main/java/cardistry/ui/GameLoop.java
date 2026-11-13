package cardistry.ui;

import cardistry.core.CombatEngine;
import cardistry.core.GameState;

/** Relie le moteur, l'affichage et la saisie. */
public final class GameLoop {

    private GameLoop() {
    }

    public static void run(CombatEngine engine, Renderer renderer, Input input) {
        GameState state = engine.state();
        engine.start();
        if (input.wantsIntro()) {
            renderer.showIntro(state);
        }
        while (!engine.isOver()) {
            engine.startTurn();
            renderer.showStatus(state);
            input.waitForContinue();
            renderer.showHand(state);
            while (!engine.submit(input.nextAction(state))) {
                // action refusée : le renderer a affiché pourquoi, on redemande
            }
        }
        renderer.showGameOver(state);
        input.waitForExit();
    }
}

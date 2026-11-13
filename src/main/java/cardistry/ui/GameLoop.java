package cardistry.ui;

import cardistry.core.ActionResult;
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
            // Action refusée ou gratuite (objet) : on redemande jusqu'à la fin du tour.
            while (engine.submit(input.nextAction(state)) != ActionResult.TURN_ENDED) {
                if (engine.isOver()) {
                    break;
                }
            }
        }
        renderer.showGameOver(state);
        input.waitForExit();
    }
}

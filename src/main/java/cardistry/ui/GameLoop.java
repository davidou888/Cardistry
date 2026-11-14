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
        engine.setDecisions(input);
        engine.start();
        if (input.wantsIntro()) {
            renderer.showIntro(state);
        }
        while (!engine.isOver()) {
            boolean playerActs = engine.startTurn();
            if (engine.isOver()) {
                break;
            }
            if (!playerActs) {
                // Tour sauté (ou adversaire tué en début de tour) : rien à choisir.
                renderer.showStatus(state);
                resolveRewards(engine, input, state);
                continue;
            }
            renderer.showStatus(state);
            input.waitForContinue();
            renderer.showHand(state);
            // Action refusée ou gratuite (objet) : on redemande jusqu'à la fin du tour.
            while (engine.submit(input.nextAction(state)) != ActionResult.TURN_ENDED) {
                if (engine.isOver()) {
                    break;
                }
            }
            resolveRewards(engine, input, state);
        }
        renderer.showGameOver(state);
        input.waitForExit();
    }

    private static void resolveRewards(CombatEngine engine, Input input, GameState state) {
        while (engine.hasPendingReward()) {
            engine.resolveReward(input.chooseReward(state, engine.pendingReward()));
        }
    }
}

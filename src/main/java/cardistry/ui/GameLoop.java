package cardistry.ui;

import cardistry.core.Action;
import cardistry.core.ActionResult;
import cardistry.run.Run;
import cardistry.run.Secret;
import cardistry.run.map.MapView;
import java.util.List;
import java.util.function.Supplier;

/** Relie la partie, l'affichage et la saisie. */
public final class GameLoop {

    private GameLoop() {
    }

    /** Partie sans suivi des déblocages. */
    public static void run(Run run, Renderer renderer, Input input) {
        run(run, renderer, input, List::of);
    }

    /**
     * @param unlockedThisRun secrets débloqués pendant la partie, affichés sur l'écran de fin
     */
    public static void run(Run run, Renderer renderer, Input input, Supplier<List<Secret>> unlockedThisRun) {
        TitleChoice title = input.title(run.seed());
        title.codes().forEach(run::addCode);
        run.setDecisions(input);
        run.start();
        if (title.showRules()) {
            renderer.showIntro(run.state());
        }
        while (!run.isOver()) {
            switch (run.phase()) {
                case MAP -> chooseNode(run, renderer, input);
                case COMBAT -> playTurn(run, renderer, input);
                case REWARD -> resolveRewards(run, input);
                case OVER -> {
                }
            }
        }
        renderer.showGameOver(run.state(), unlockedThisRun.get().stream().map(Secret::displayName).toList());
        input.waitForExit();
    }

    private static void chooseNode(Run run, Renderer renderer, Input input) {
        MapView view = new MapView(run.state(), run.map().orElseThrow(), run.visited(), run.position(), run.choices());
        renderer.showMap(view);
        int index = input.chooseNode(view);
        if (index >= 0 && index < view.choices().size()) {
            run.chooseNode(index);
        }
    }

    private static void playTurn(Run run, Renderer renderer, Input input) {
        boolean playerActs = run.startTurn();
        if (run.isOver()) {
            return;
        }
        renderer.showStatus(run.state());
        if (!playerActs) {
            // Tour sauté (ou adversaire tué en début de tour) : rien à choisir.
            return;
        }
        input.waitForContinue();
        renderer.showHand(run.state());
        // Action refusée ou gratuite (objet, Ctrl+Z) : on redemande jusqu'à la fin du tour.
        while (true) {
            Action action = input.nextAction(run.state());
            ActionResult result = run.submit(action);
            if (result == ActionResult.TURN_ENDED || run.isOver()) {
                break;
            }
            if (action instanceof Action.Undo && result == ActionResult.FREE_ACTION) {
                // Retour au début du tour précédent : on réaffiche tout.
                renderer.showStatus(run.state());
                renderer.showHand(run.state());
            }
        }
    }

    private static void resolveRewards(Run run, Input input) {
        while (run.hasPendingReward()) {
            run.resolveReward(input.chooseReward(run.state(), run.pendingReward()));
        }
    }
}

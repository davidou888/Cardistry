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
        run(run, renderer, input, unlockedThisRun, null);
    }

    /**
     * @param audio chef d'orchestre du son (musique selon l'écran) ; {@code null} = pas de son
     */
    public static void run(Run run, Renderer renderer, Input input, Supplier<List<Secret>> unlockedThisRun,
            cardistry.ui.audio.AudioDirector audio) {
        if (audio != null) {
            audio.title();
        }
        TitleChoice title = input.title(run.seed());
        title.codes().forEach(run::addCode);
        run.setDecisions(input);
        run.start();
        if (title.showRules()) {
            renderer.showIntro(run.state());
        }
        while (!run.isOver()) {
            if (audio != null) {
                audio.phase(run.phase(), run.state());
            }
            switch (run.phase()) {
                case MAP -> chooseNode(run, renderer, input);
                case COMBAT -> playTurn(run, renderer, input);
                case REWARD -> resolveRewards(run, input);
                case CARD_REWARD -> {
                    int index = input.chooseCardReward(run.state(), run.pendingCardReward());
                    run.resolveCardReward(index < run.pendingCardReward().size() ? index : -1);
                }
                case REST -> rest(run, input);
                case EVENT -> story(run, renderer, input);
                case SHOP -> run.shop(input.chooseShopAction(run.state(), run.shop().orElseThrow(),
                        run.cardRemovalPrice()));
                case OVER -> {
                }
            }
        }
        if (audio != null) {
            audio.phase(Run.Phase.OVER, run.state());
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

    private static void story(Run run, Renderer renderer, Input input) {
        cardistry.story.Story story = run.story().orElseThrow();
        if (run.chooseStory(input.chooseStory(run.state(), story))) {
            renderer.showStoryResult(run.state(), run.lastStoryResult().orElseThrow());
        }
    }

    private static void rest(Run run, Input input) {
        cardistry.run.RestOption option = input.chooseRest(run.state(), run.restOptions());
        if (option == cardistry.run.RestOption.REMOVE_CARD) {
            int index = input.chooseCardToRemove(run.state());
            if (index < 0 || index >= run.state().deck().size()) {
                return; // annulé : on redemande
            }
            run.rest(option, index);
        } else {
            run.rest(option);
        }
    }

    private static void resolveRewards(Run run, Input input) {
        while (run.hasPendingReward()) {
            run.resolveReward(input.chooseReward(run.state(), run.pendingReward()));
        }
    }
}

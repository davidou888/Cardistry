package cardistry.story;

import cardistry.core.GameState;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Un choix d'événement.
 *
 * @param label texte du choix (« Secouer la machine »)
 * @param hint précision affichée à côté (« 50 % : des pièces, sinon tu te fais mal »), ou vide
 * @param available condition pour pouvoir le choisir
 * @param requirement ce qu'il manque quand il n'est pas disponible (« il te faut 20 Choomies »)
 * @param outcome conséquence
 */
public record StoryChoice(String label, String hint, Predicate<GameState> available, String requirement,
        Consumer<StoryContext> outcome) {

    /** Choix toujours disponible. */
    public static StoryChoice of(String label, String hint, Consumer<StoryContext> outcome) {
        return new StoryChoice(label, hint, s -> true, "", outcome);
    }

    /** Choix disponible seulement si {@code condition}. */
    public static StoryChoice when(String label, String hint, Predicate<GameState> condition, String requirement,
            Consumer<StoryContext> outcome) {
        return new StoryChoice(label, hint, condition, requirement, outcome);
    }

    public boolean isAvailable(GameState state) {
        return available.test(state);
    }
}

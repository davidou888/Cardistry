package cardistry.story;

import java.util.List;

/**
 * Un événement (nœud {@code ?} de la carte) : un titre, un texte, des choix et leurs conséquences.
 * Déclaré dans {@link StoryRegistry}.
 */
public record Story(String id, String title, String text, List<StoryChoice> choices) {

    public Story {
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("Un événement a au moins un choix");
        }
        choices = List.copyOf(choices);
    }
}

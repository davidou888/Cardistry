package cardistry.model;

import java.util.List;

/** Registre des adversaires, dans l'ordre où on les affronte. */
public final class EnemyRoster {

    private EnemyRoster() {
    }

    /** Crée de nouvelles instances à chaque appel. */
    public static List<Enemy> createDefault() {
        return List.of(
                new Enemy("Hydre", Element.FIRE, 20.0, 5.0),
                new Enemy("Sirène", Element.WATER, 30.0, 10.0),
                new Enemy("Goblin", Element.NATURE, 40.0, 15.0));
    }
}

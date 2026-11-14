package cardistry.hero;

import cardistry.core.event.GameHooks;
import cardistry.core.stats.StatModifier;
import java.util.List;

/**
 * Personnage jouable : un nom, une capacité (des modificateurs de stats et des hooks, comme un
 * trinket qu'on ne peut ni perdre ni remplacer).
 *
 * @param id identifiant ({@code louis}), aussi le nom de son image {@code images/player/<id>.png}
 * @param name nom affiché
 * @param abilityName nom de la capacité
 * @param description effet de la capacité, affiché au choix du personnage
 * @param modifiers stats de départ (PV max, malus de Gueule de bois…)
 * @param hooks réactions aux événements du combat
 */
public record Hero(String id, String name, String abilityName, String description, List<StatModifier> modifiers,
        GameHooks hooks) {

    public Hero {
        modifiers = List.copyOf(modifiers);
    }

    /** Source de ses modificateurs dans les stats. */
    public static String modifierSource(String id) {
        return "hero:" + id;
    }
}

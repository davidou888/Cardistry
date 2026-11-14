package cardistry.ui;

import cardistry.run.Secret;
import java.util.List;

/**
 * Ce que le joueur a choisi sur l'écran titre.
 *
 * @param showRules afficher les règles avant de commencer
 * @param codes secrets activés par un code (équipés pour cette partie seulement)
 */
public record TitleChoice(boolean showRules, List<Secret> codes) {

    public TitleChoice {
        codes = List.copyOf(codes);
    }

    public static TitleChoice play() {
        return new TitleChoice(false, List.of());
    }
}

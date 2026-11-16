package cardistry.ui;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.model.Familiar;

/** Textes du familier (l'alien de Sassy), partagés par les trois affichages. */
public final class FamiliarText {

    private FamiliarText() {
    }

    /** « L'alien subit 6 dégâts » (« … et tombe assommé » à 0 PV). */
    public static String hitLine(Familiar familiar, double amount) {
        return "L'" + familiar.name().toLowerCase() + " subit " + num(amount) + " dégâts"
                + (familiar.isKnockedOut() ? " et tombe assommé" : "");
    }

    /** « ALIEN 14/20 » ou « ALIEN assommé ». */
    public static String status(Familiar familiar) {
        return familiar.name().toUpperCase() + " "
                + (familiar.isKnockedOut() ? "assommé" : num(Math.max(0, familiar.hp())) + "/" + num(familiar.maxHp()));
    }

    /** Suffixe de l'intent quand l'attaque vise le familier : « → vise l'alien » ; vide sinon. */
    public static String intentTarget(GameState state) {
        return state.familiarTargeted()
                ? " → vise l'" + state.familiar().orElseThrow().name().toLowerCase() : "";
    }
}

package cardistry.ui.text;

/** Accès de test à la description des cartes du mode texte. */
public final class TextRendererAccess {

    private TextRendererAccess() {
    }

    public static String describe(cardistry.model.card.Card card) {
        return TextRenderer.describe(card);
    }
}

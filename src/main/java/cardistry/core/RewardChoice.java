package cardistry.core;

/** Réponse du joueur à une offre de récompense. */
public sealed interface RewardChoice {

    /**
     * Prendre la proposition {@code option}.
     *
     * @param replaceSlot emplacement (trinket ou objet, selon le type pris) à libérer si
     *     l'inventaire est plein ; {@code -1} s'il y a de la place
     */
    record Take(int option, int replaceSlot) implements RewardChoice {

        public static Take into(int option) {
            return new Take(option, -1);
        }
    }

    /** Refuser la récompense. */
    record Skip() implements RewardChoice {
    }
}

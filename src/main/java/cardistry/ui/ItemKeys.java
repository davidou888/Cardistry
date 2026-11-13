package cardistry.ui;

/** Touches des emplacements d'objets : A, Z, E… (rangée du haut d'un clavier AZERTY). */
public final class ItemKeys {

    private static final String LETTERS = "azertyuiop";

    private ItemKeys() {
    }

    /** Touche de l'emplacement {@code slot} (0 → 'a'). */
    public static char key(int slot) {
        return LETTERS.charAt(slot);
    }

    /** Emplacement correspondant à la touche, ou -1. */
    public static int slot(char key, int slots) {
        int index = LETTERS.indexOf(Character.toLowerCase(key));
        return index >= 0 && index < slots ? index : -1;
    }

    /** "A/Z/E" pour 3 emplacements. */
    public static String label(int slots) {
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < slots; i++) {
            if (i > 0) {
                label.append('/');
            }
            label.append(Character.toUpperCase(key(i)));
        }
        return label.toString();
    }
}

package cardistry.ui;

/** Lit et applique les réglages (son mis à jour tout de suite, sauvegarde). Fourni par {@code Main}. */
public interface OptionsController {

    Settings settings();

    /** Applique {@code settings} tout de suite (volumes) et les sauvegarde. */
    void apply(Settings settings);

    /** Réglages en mémoire seulement (tests, ou pas de sauvegarde). */
    static OptionsController inMemory(Settings initial) {
        return new OptionsController() {
            private Settings current = initial;

            @Override
            public Settings settings() {
                return current;
            }

            @Override
            public void apply(Settings settings) {
                current = settings;
            }
        };
    }
}

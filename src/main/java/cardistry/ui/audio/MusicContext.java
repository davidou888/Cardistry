package cardistry.ui.audio;

/** Contexte musical : chaque écran a sa piste ({@code music.<clé>} dans la table). */
public enum MusicContext {
    TITLE("title"),
    MAP("map"),
    COMBAT("combat"),
    ELITE("elite"),
    BOSS("boss"),
    SHOP("shop"),
    REST("rest"),
    EVENT("event"),
    GAME_OVER("game-over"),
    VICTORY("victory");

    private final String key;

    MusicContext(String key) {
        this.key = key;
    }

    /** Nom dans la table : {@code music.<clé>}. */
    public String tableName() {
        return "music." + key;
    }
}

package cardistry.ui;

/**
 * Réglages du joueur, sauvegardés à côté des déblocages ({@code platform.SettingsFile}).
 *
 * @param music volume de la musique, 0 à 10
 * @param effects volume des effets, 0 à 10
 * @param muted tout le son coupé
 */
public record Settings(int music, int effects, boolean muted) {

    public static final int MAX_LEVEL = 10;

    public Settings {
        music = Math.max(0, Math.min(MAX_LEVEL, music));
        effects = Math.max(0, Math.min(MAX_LEVEL, effects));
    }

    /** Réglages par défaut : musique 6/10, effets 8/10, son actif. */
    public static Settings defaults() {
        return new Settings(6, 8, false);
    }

    public Settings withMusic(int value) {
        return new Settings(value, effects, muted);
    }

    public Settings withEffects(int value) {
        return new Settings(music, value, muted);
    }

    public Settings withMuted(boolean value) {
        return new Settings(music, effects, value);
    }
}

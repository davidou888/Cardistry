package cardistry.ui.audio;

/**
 * Réglage de volume de 0 à {@link #MAX}, sur une échelle perceptive : chaque cran en dessous du
 * maximum retire {@link #DB_PER_STEP} dB (5/10 = −10 dB, perçu comme « deux fois moins fort ») ;
 * 0 = silence.
 */
public final class Volume {

    public static final int MAX = 10;
    public static final double DB_PER_STEP = 2.0;
    public static final int DEFAULT_MUSIC = 6;
    public static final int DEFAULT_EFFECTS = 8;

    private Volume() {
    }

    /** Gain linéaire d'un réglage. */
    public static float gain(int level) {
        int clamped = Math.max(0, Math.min(MAX, level));
        if (clamped == 0) {
            return 0f;
        }
        return (float) Math.pow(10, -(MAX - clamped) * DB_PER_STEP / 20.0);
    }
}

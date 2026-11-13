package cardistry.model;

import java.text.Normalizer;
import java.util.Locale;

/** Identifiants stables dérivés des noms affichés. */
public final class Ids {

    private Ids() {
    }

    /** "Petit pétou" → "petit-petou". */
    public static String slug(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}

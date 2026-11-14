package cardistry.ui;

/**
 * Cris des Choomah : tout ce qu'ils « disent » (attaques, descriptions, journal) s'affiche en
 * « ooOOOaAAAOaAOOA ». Les chiffres restent lisibles ; la transformation est déterministe.
 */
public final class Scream {

    private static final String SOUNDS = "OoAaOOAU";

    private Scream() {
    }

    /** « vol de 5 Choomies » → « oAO aO 5 AOOAoOa » (lettres criées, chiffres et ponctuation gardés). */
    public static String of(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int state = 7;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                state = (state * 31 + c) & 0x7fffffff;
                out.append(SOUNDS.charAt(state % SOUNDS.length()));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}

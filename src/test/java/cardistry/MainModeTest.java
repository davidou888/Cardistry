package cardistry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Choix de l'affichage : fenêtre par défaut, --terminal, --no-ui, sans affichage graphique. */
class MainModeTest {

    @Test
    void fenetreParDefautSurTousLesOs() {
        assertEquals(Main.Mode.WINDOW, Main.mode(List.of(), false, true));
        assertEquals(Main.Mode.WINDOW, Main.mode(List.of("--window"), false, false));
        assertEquals(Main.Mode.WINDOW, Main.mode(List.of("--crt", "--no-images"), false, false));
    }

    @Test
    void terminal() {
        assertEquals(Main.Mode.TERMINAL, Main.mode(List.of("--terminal"), false, true));
        assertEquals(Main.Mode.TEXT, Main.mode(List.of("--terminal"), false, false), "pas un vrai terminal");
    }

    @Test
    void texte() {
        assertEquals(Main.Mode.TEXT, Main.mode(List.of("--no-ui"), false, true));
        assertEquals(Main.Mode.TEXT, Main.mode(List.of("--no-ui", "--terminal"), false, true));
    }

    @Test
    void sansAffichageGraphique() {
        assertEquals(Main.Mode.TERMINAL, Main.mode(List.of(), true, true));
        assertEquals(Main.Mode.TEXT, Main.mode(List.of(), true, false));
    }
}

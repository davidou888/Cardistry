package cardistry.ui.lanterna;

import com.googlecode.lanterna.TextColor;
import java.util.ArrayList;
import java.util.List;

/** Ce qui s'affiche en plus de l'état du jeu : sélection, journal, effets d'animation en cours. */
final class ViewState {

    /** Texte flottant (« -15 », « +10 ») qui monte au-dessus d'une cible. */
    record Floating(String text, TextColor color, boolean onEnemy, int rise) {
    }

    record LogLine(String text, TextColor color) {
    }

    static final int LOG_CAPACITY = 50;

    int selected = 0;
    boolean confirmingQuit = false;
    /** Bandeau de notification (palier de set…), affiché jusqu'au tour suivant ; null si aucun. */
    String toast;
    final List<LogLine> log = new ArrayList<>();

    // --- Effets d'animation (remis à zéro après chaque animation) ---
    Double enemyHpShown;
    Double playerHpShown;
    int enemyOffsetX;
    TextColor enemyFlash;
    boolean enemyHidden;
    boolean enemyDim;
    TextColor playerFlash;
    TextColor terrainFlash;
    int highlightSlot = -1;
    final List<Floating> floating = new ArrayList<>();

    void log(String text, TextColor color) {
        log.add(new LogLine(text, color));
        if (log.size() > LOG_CAPACITY) {
            log.remove(0);
        }
    }

    void resetEffects() {
        enemyHpShown = null;
        playerHpShown = null;
        enemyOffsetX = 0;
        enemyFlash = null;
        enemyHidden = false;
        enemyDim = false;
        playerFlash = null;
        terrainFlash = null;
        highlightSlot = -1;
        floating.clear();
    }
}

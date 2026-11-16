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
    /** Journal ouvert en panneau (fenêtre, touche J) ; fermé par défaut. */
    boolean journalOpen = false;
    /** Lignes remontées dans le journal ouvert (0 = les plus récentes). */
    int journalScroll = 0;

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
    /** Animation du sprite de l'adversaire (attaque, dégâts, mort) ; idle par défaut. */
    String spriteAnimation = cardistry.ui.sprite.Sprite.IDLE;
    int spriteFrame = 0;
    /** Familier : PV affichés pendant une animation, clignotement, animation (attack, smoke) et frame. */
    Double familiarHpShown;
    TextColor familiarFlash;
    String familiarAnimation = cardistry.ui.sprite.Sprite.IDLE;
    int familiarFrame = 0;
    /** Animation du personnage (attack, hurt, death) ; idle par défaut, avec la frame idle commune. */
    String playerAnimation = cardistry.ui.sprite.Sprite.IDLE;
    int playerFrame = 0;
    int playerOffsetX;
    /** Frame idle en cours (avance toute seule pendant l'attente d'une touche). */
    int idleFrame = 0;
    /** Page des textes trop longs (descriptions) : avance toutes les {@code PAGE_MS}, revient à 0 à chaque touche. */
    int pageTick = 0;

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
        spriteAnimation = cardistry.ui.sprite.Sprite.IDLE;
        spriteFrame = 0;
        playerAnimation = cardistry.ui.sprite.Sprite.IDLE;
        playerFrame = 0;
        playerOffsetX = 0;
        familiarHpShown = null;
        familiarFlash = null;
        familiarAnimation = cardistry.ui.sprite.Sprite.IDLE;
        familiarFrame = 0;
    }
}

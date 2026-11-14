package cardistry.run;

import cardistry.core.GameState;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Surveille les conditions de déblocage des secrets pendant une partie et les sauvegarde dès
 * qu'elles sont remplies (un secret débloqué en cours de partie n'entre dans le pool qu'à la
 * partie suivante) :
 * <ul>
 *   <li>NullPointerException : gagner un combat avec 0 carte en main ;</li>
 *   <li>Ctrl+Z : perdre contre le dernier adversaire alors qu'il lui reste ≤ {@link #CTRL_Z_ENEMY_HP} PV ;</li>
 *   <li>CLAUDE.md : gagner une partie alors que les deux autres sont débloqués.</li>
 * </ul>
 * Les codes (secret équipé pour une partie) ne débloquent rien.
 */
public final class UnlockTracker implements Consumer<GameEvent> {

    /** PV maximum du dernier adversaire pour débloquer Ctrl+Z en perdant. */
    public static final double CTRL_Z_ENEMY_HP = 5;

    private final Supplier<GameState> state;
    private final UnlockStore store;
    private final EventBus bus;
    private final Set<Secret> unlocked;
    private final List<Secret> unlockedThisRun = new ArrayList<>();

    /**
     * @param state état de la partie en cours (il change après un Ctrl+Z)
     * @param bus où publier {@link GameEvent.SecretUnlocked}
     */
    public UnlockTracker(Supplier<GameState> state, UnlockStore store, EventBus bus) {
        this.state = state;
        this.store = store;
        this.bus = bus;
        this.unlocked = EnumSet.noneOf(Secret.class);
        this.unlocked.addAll(store.load());
    }

    @Override
    public void accept(GameEvent event) {
        GameState s = state.get();
        switch (event) {
            case GameEvent.EnemyKilled e -> {
                if (s.hand().isEmpty() && s.player().isAlive()) {
                    unlock(Secret.NULL_POINTER_EXCEPTION);
                }
            }
            case GameEvent.GameLost e -> {
                if (s.isFinalCombat() && s.currentEnemy().hp() <= CTRL_Z_ENEMY_HP) {
                    unlock(Secret.CTRL_Z);
                }
            }
            case GameEvent.GameWon e -> {
                if (unlocked.contains(Secret.NULL_POINTER_EXCEPTION) && unlocked.contains(Secret.CTRL_Z)) {
                    unlock(Secret.CLAUDE_MD);
                }
            }
            default -> {
            }
        }
    }

    private void unlock(Secret secret) {
        if (unlocked.add(secret)) {
            unlockedThisRun.add(secret);
            store.save(unlocked);
            bus.publish(new GameEvent.SecretUnlocked(secret.displayName()));
        }
    }

    /** Secrets débloqués pendant cette partie, dans l'ordre (pour l'écran de fin). */
    public List<Secret> unlockedThisRun() {
        return List.copyOf(unlockedThisRun);
    }
}

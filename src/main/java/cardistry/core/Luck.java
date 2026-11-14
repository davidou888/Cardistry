package cardistry.core;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * Tirages de chance <b>en faveur du joueur</b> (CLAUDE.md : stat {@link cardistry.core.stats.StatType#LUCKY_ROLLS}).
 * Sans la stat, un seul tirage ; avec, deux tirages et on garde le plus favorable (le premier en cas
 * d'égalité). Concernés : Googz, NullPointerException, rareté des récompenses. Pas la pioche ni le
 * choix des récompenses proposées.
 */
public final class Luck {

    private Luck() {
    }

    /**
     * @param better {@code better.compare(a, b) > 0} si {@code a} est plus favorable au joueur
     */
    public static <T> T favorable(GameState state, Supplier<T> roll, Comparator<? super T> better) {
        T first = roll.get();
        if (!state.luckyRolls()) {
            return first;
        }
        T second = roll.get();
        return better.compare(second, first) > 0 ? second : first;
    }

    /** {@code true} avec la probabilité {@code probability} (deux chances avec CLAUDE.md). */
    public static boolean chance(GameState state, Rng rng, double probability) {
        return favorable(state, () -> rng.nextDouble() < probability, Boolean::compare);
    }
}

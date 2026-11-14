package cardistry.core;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seule source d'aléatoire du jeu. Une seed par run : même seed + mêmes choix = même partie.
 * Ne jamais utiliser {@code Random}, {@code Math.random()} ou équivalent ailleurs.
 */
public final class Rng {

    private final long seed;
    private final SplittableRandom random;

    public Rng(long seed) {
        this.seed = seed;
        this.random = new SplittableRandom(seed);
    }

    /** Rng avec une seed tirée au hasard (positive, courte à recopier). */
    public static Rng withRandomSeed() {
        return new Rng(ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }

    public long seed() {
        return seed;
    }

    /**
     * Sous-flux nommé, dérivé de la seed seulement (pas de l'état actuel) : même seed + même nom =
     * même suite de tirages, quoi qu'on ait tiré ailleurs (ex. la carte d'un niveau ne dépend pas des
     * rerolls du shop).
     */
    public Rng fork(String name) {
        long mixed = seed * 0x9E3779B97F4A7C15L + name.hashCode();
        mixed ^= mixed >>> 31;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        return new Rng(mixed);
    }

    /** Entier dans [0, bound). */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public double nextDouble() {
        return random.nextDouble();
    }

    /** Élément tiré uniformément dans une liste non vide. */
    public <T> T pick(List<T> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Liste vide");
        }
        return items.get(nextInt(items.size()));
    }
}

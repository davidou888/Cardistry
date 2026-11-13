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

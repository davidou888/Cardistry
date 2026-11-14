package cardistry.items;

import cardistry.core.GameState;
import cardistry.core.Luck;
import cardistry.core.Rng;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tirage des récompenses : pondéré par {@link Rarity#dropWeight()}, sans doublon, sans trinket
 * déjà équipé. Un trinket Secret n'est tiré que s'il a été débloqué, avec un poids de
 * {@link #UNLOCKED_SECRET_WEIGHT}.
 *
 * <p>Avec CLAUDE.md, la rareté de chaque proposition est tirée deux fois et on garde la plus rare :
 * deux tirages pondérés, on garde celui de rareté la plus haute (le premier en cas d'égalité), ce
 * qui revient à tirer la rareté deux fois puis l'objet dans cette rareté.
 */
public final class LootTable {

    /** Poids d'un secret débloqué (un commun pèse {@link Rarity#COMMON}{@code .dropWeight()}). */
    public static final int UNLOCKED_SECRET_WEIGHT = 2;

    private final Supplier<List<Collectible>> source;
    private final Set<String> unlockedSecrets;

    /** Table sur tout le registre, sans secret débloqué. */
    public LootTable() {
        this(ItemRegistry::all);
    }

    /** Table sur tout le registre ; {@code unlockedSecrets} = identifiants des secrets débloqués. */
    public LootTable(Set<String> unlockedSecrets) {
        this(ItemRegistry::all, unlockedSecrets);
    }

    /** Table sur une liste de candidats donnée (tests, tables spéciales). */
    public LootTable(Supplier<List<Collectible>> source) {
        this(source, Set.of());
    }

    public LootTable(Supplier<List<Collectible>> source, Set<String> unlockedSecrets) {
        this.source = source;
        this.unlockedSecrets = Set.copyOf(unlockedSecrets);
    }

    /** Poids de tirage de {@code c} (0 = jamais tiré). */
    public int weight(Collectible c) {
        if (c.rarity() == Rarity.SECRET) {
            return unlockedSecrets.contains(c.id()) ? UNLOCKED_SECRET_WEIGHT : 0;
        }
        return c.rarity().dropWeight();
    }

    /** Jusqu'à {@code count} propositions distinctes (moins si les candidats manquent). */
    public List<Collectible> offer(GameState state, Rng rng, int count) {
        return offer(state, rng, count, Rarity.COMMON);
    }

    /**
     * Jusqu'à {@code count} propositions distinctes, toutes au moins de rareté {@code minimum}
     * (élite : Peu commun, boss : Rare ; les secrets débloqués restent possibles).
     */
    public List<Collectible> offer(GameState state, Rng rng, int count, Rarity minimum) {
        List<Collectible> candidates = new ArrayList<>();
        for (Collectible c : source.get()) {
            if (c.rarity().compareTo(minimum) < 0) {
                continue;
            }
            boolean owned = c instanceof Trinket && state.hasTrinket(c.id());
            boolean duplicate = candidates.stream().anyMatch(o -> o.id().equals(c.id()));
            if (weight(c) > 0 && !owned && !duplicate) {
                candidates.add(c);
            }
        }
        Comparator<Integer> rarer = Comparator.comparing(i -> candidates.get(i).rarity());
        List<Collectible> picked = new ArrayList<>();
        while (picked.size() < count && !candidates.isEmpty()) {
            int index = Luck.favorable(state, () -> pickIndex(candidates, rng), rarer);
            picked.add(candidates.remove(index));
        }
        return picked;
    }

    private int pickIndex(List<Collectible> candidates, Rng rng) {
        int total = candidates.stream().mapToInt(this::weight).sum();
        int roll = rng.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weight(candidates.get(i));
            if (roll < 0) {
                return i;
            }
        }
        throw new AssertionError("Tirage hors des poids");
    }
}

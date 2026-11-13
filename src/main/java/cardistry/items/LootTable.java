package cardistry.items;

import cardistry.core.GameState;
import cardistry.core.Rng;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tirage des récompenses : pondéré par {@link Rarity#dropWeight()}, sans doublon, sans trinket
 * déjà équipé, jamais de rareté à poids nul (Secret).
 */
public final class LootTable {

    private final Supplier<List<Collectible>> source;

    /** Table sur tout le registre. */
    public LootTable() {
        this(ItemRegistry::all);
    }

    /** Table sur une liste de candidats donnée (tests, tables spéciales). */
    public LootTable(Supplier<List<Collectible>> source) {
        this.source = source;
    }

    /** Jusqu'à {@code count} propositions distinctes (moins si les candidats manquent). */
    public List<Collectible> offer(GameState state, Rng rng, int count) {
        List<Collectible> candidates = new ArrayList<>();
        for (Collectible c : source.get()) {
            boolean owned = c instanceof Trinket && state.hasTrinket(c.id());
            boolean duplicate = candidates.stream().anyMatch(o -> o.id().equals(c.id()));
            if (c.rarity().dropWeight() > 0 && !owned && !duplicate) {
                candidates.add(c);
            }
        }
        List<Collectible> picked = new ArrayList<>();
        while (picked.size() < count && !candidates.isEmpty()) {
            picked.add(candidates.remove(pickIndex(candidates, rng)));
        }
        return picked;
    }

    private static int pickIndex(List<Collectible> candidates, Rng rng) {
        int total = candidates.stream().mapToInt(c -> c.rarity().dropWeight()).sum();
        int roll = rng.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= candidates.get(i).rarity().dropWeight();
            if (roll < 0) {
                return i;
            }
        }
        throw new AssertionError("Tirage hors des poids");
    }
}

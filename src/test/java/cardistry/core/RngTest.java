package cardistry.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RngTest {

    private static List<Integer> sequence(long seed) {
        Rng rng = new Rng(seed);
        return IntStream.range(0, 100).map(i -> rng.nextInt(1000)).boxed().toList();
    }

    @Test
    void memeSeedMemeSuite() {
        assertEquals(sequence(123), sequence(123));
    }

    @Test
    void seedsDifferentesSuitesDifferentes() {
        assertNotEquals(sequence(1), sequence(2));
    }

    @Test
    void conserveSaSeed() {
        assertEquals(99, new Rng(99).seed());
    }

    @Test
    void seedAleatoirePositive() {
        long seed = Rng.withRandomSeed().seed();
        assertTrue(seed >= 0 && seed < 1_000_000_000L);
    }

    @Test
    void pickRefuseUneListeVide() {
        assertThrows(IllegalArgumentException.class, () -> new Rng(0).pick(List.of()));
    }
}

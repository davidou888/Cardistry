package cardistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.items.ItemRegistry;
import cardistry.run.Secret;
import cardistry.run.UnlockStore;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Options de ligne de commande liées aux secrets. */
class MainOptionsTest {

    @Test
    void resetUnlocksEffaceAvantDeLire() {
        UnlockStore store = new UnlockStore.InMemory();
        store.save(EnumSet.allOf(Secret.class));
        Main.Game game = Main.prepare(List.of("--reset-unlocks"), store);
        assertTrue(store.load().isEmpty());
        assertTrue(game.run().unlockedSecrets().isEmpty());
    }

    @Test
    void sansResetLesDeblocagesSontLus() {
        UnlockStore store = new UnlockStore.InMemory();
        store.save(EnumSet.of(Secret.CTRL_Z));
        Main.Game game = Main.prepare(List.of(), store);
        assertEquals(Set.of(Secret.CTRL_Z), game.run().unlockedSecrets());
        assertEquals(Set.of(Secret.CTRL_Z), store.load());
    }

    @Test
    void seedNullEquipeNullPointerExceptionPourCettePartie() {
        UnlockStore store = new UnlockStore.InMemory();
        Main.Game game = Main.prepare(List.of("--seed", "null"), store);
        assertEquals(List.of(Secret.NULL_POINTER_EXCEPTION), game.run().codes());
        game.run().start();
        assertTrue(game.run().state().hasTrinket(ItemRegistry.NULL_POINTER_EXCEPTION));
        assertTrue(store.load().isEmpty(), "aucun déblocage durable");
        assertTrue(game.tracker().unlockedThisRun().isEmpty());
    }

    @Test
    void seedNumeriqueSansCode() {
        Main.Game game = Main.prepare(List.of("--seed", "42"), new UnlockStore.InMemory());
        assertEquals(42, game.run().seed());
        assertTrue(game.run().codes().isEmpty());
        assertFalse(Main.isNullSeed(List.of("--seed", "42")));
        assertTrue(Main.isNullSeed(List.of("--fast", "--seed", "null")));
    }
}

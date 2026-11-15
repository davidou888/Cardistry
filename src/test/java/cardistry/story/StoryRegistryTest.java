package cardistry.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.GameState;
import cardistry.model.card.Card;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Les trois événements placeholder, choix par choix, avec un contexte qui enregistre tout. */
class StoryRegistryTest {

    /** Contexte factice : le tirage de chance renvoie {@code lucky}. */
    private static final class Recorder implements StoryContext {

        final GameState state = GameState.newGame();
        final boolean lucky;
        final List<String> said = new ArrayList<>();
        int choomies;
        double healed;
        double hurt;
        int cigarettes;
        int hangovers;
        final List<Card> cards = new ArrayList<>();
        final List<Double> chances = new ArrayList<>();

        Recorder(boolean lucky) {
            this.lucky = lucky;
        }

        @Override public GameState state() { return state; }
        @Override public void say(String line) { said.add(line); }
        @Override public boolean chance(double p) { chances.add(p); return lucky; }
        @Override public void choomies(int amount) { choomies += amount; }
        @Override public void heal(double amount) { healed += amount; }
        @Override public void hurt(double amount) { hurt += amount; }
        @Override public int cigarettes(int count) { cigarettes += count; return count; }
        @Override public void hangover() { hangovers++; }
        @Override public void addCard(Card card) { cards.add(card); }
    }

    private static Recorder play(String id, int choice, boolean lucky) {
        Recorder ctx = new Recorder(lucky);
        StoryRegistry.byId(id).orElseThrow().choices().get(choice).outcome().accept(ctx);
        assertFalse(ctx.said.isEmpty(), "chaque choix dit ce qui se passe");
        return ctx;
    }

    @Test
    void troisEvenementsDistincts() {
        assertEquals(3, StoryRegistry.all().size());
        assertEquals(3, StoryRegistry.all().stream().map(Story::id).distinct().count());
        for (Story story : StoryRegistry.all()) {
            assertFalse(story.title().isBlank());
            assertFalse(story.text().isBlank());
            assertTrue(story.choices().size() >= 2);
        }
    }

    @Test
    void distributeur() {
        Recorder win = play("distributeur", 0, true);
        assertEquals(List.of(0.5), win.chances);
        assertEquals(25, win.choomies);
        assertEquals(0, win.hurt);
        Recorder lose = play("distributeur", 0, false);
        assertEquals(0, lose.choomies);
        assertEquals(6, lose.hurt);
        Recorder leave = play("distributeur", 1, true);
        assertEquals(0, leave.choomies + leave.hurt);
    }

    @Test
    void inconnu() {
        Recorder drink = play("inconnu", 0, true);
        assertEquals(15, drink.healed);
        assertEquals(1, drink.hangovers);
        Recorder tip = play("inconnu", 1, true);
        assertEquals(List.of("Boule de feu"), tip.cards.stream().map(Card::name).toList());
        Recorder no = play("inconnu", 2, true);
        assertEquals(0, no.healed + no.hangovers + no.cards.size());
    }

    @Test
    void machineAClopes() {
        Story machine = StoryRegistry.byId("machine-a-clopes").orElseThrow();
        GameState poor = GameState.newGame(); // 15 Choomies
        assertFalse(machine.choices().get(0).isAvailable(poor));
        assertEquals("il te faut 20 Choomies", machine.choices().get(0).requirement());
        Recorder buy = play("machine-a-clopes", 0, true);
        assertEquals(-20, buy.choomies);
        assertEquals(4, buy.cigarettes);
        Recorder hit = play("machine-a-clopes", 1, true);
        assertEquals(List.of(0.3), hit.chances);
        assertEquals(2, hit.cigarettes);
        Recorder ouch = play("machine-a-clopes", 1, false);
        assertEquals(4, ouch.hurt);
        assertTrue(machine.choices().get(1).isAvailable(poor));
    }

    /** Une carte donnée par un événement est la même que celle du pool (type, élément, rareté). */
    @Test
    void cartesDesEvenementsIdentiquesAuPool() {
        List<Card> pool = new cardistry.model.card.CardPool().cards();
        for (Story story : StoryRegistry.all()) {
            for (StoryChoice choice : story.choices()) {
                for (boolean lucky : new boolean[] {true, false}) {
                    Recorder ctx = new Recorder(lucky);
                    choice.outcome().accept(ctx);
                    for (Card card : ctx.cards) {
                        Card model = pool.stream().filter(c -> c.qualifiedId().equals(card.qualifiedId())).findFirst()
                                .orElseThrow(() -> new AssertionError("carte hors du pool : " + card.qualifiedId()));
                        assertEquals(model.getClass(), card.getClass(), card.name());
                        assertEquals(model.element(), card.element(), card.name());
                        assertEquals(model.rarity(), card.rarity(), card.name());
                    }
                }
            }
        }
    }
}

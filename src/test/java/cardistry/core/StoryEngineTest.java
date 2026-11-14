package cardistry.core;

import static cardistry.model.Element.FIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.core.stats.StatModifier;
import cardistry.core.stats.StatType;
import cardistry.model.card.CardPool;
import cardistry.model.card.SpellCard;
import cardistry.story.Story;
import cardistry.story.StoryChoice;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Événements dans le moteur : ouverture, choix, conséquences réelles. */
class StoryEngineTest {

    private final List<GameEvent> events = new ArrayList<>();
    private GameState state;
    private CombatEngine engine;

    private void open(Story story) {
        EventBus bus = new EventBus();
        bus.subscribe(events::add);
        state = GameState.newGame();
        engine = new CombatEngine(state, new CardPool(), new Rng(1), bus);
        engine.start();
        engine.startStory(story);
    }

    private static Story story(StoryChoice... choices) {
        return new Story("test", "Test", "Un test.", List.of(choices));
    }

    @Test
    void choixEtResultat() {
        open(story(StoryChoice.of("Payer", "", ctx -> {
            ctx.choomies(10);
            ctx.cigarettes(3);
            ctx.addCard(new SpellCard("Boule de feu", FIRE, 10));
            ctx.say("Fait.");
        })));
        assertTrue(engine.story().isPresent());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.StoryStarted));
        assertTrue(engine.chooseStory(0));
        assertTrue(engine.story().isEmpty());
        assertEquals(25, state.choomies());
        assertEquals(GameState.STARTING_CIGARETTES + 3, state.cigarettes());
        assertEquals(13, state.deck().size());
        GameEvent.StoryResolved result = new GameEvent.StoryResolved("Test", "Payer", List.of("Fait."));
        assertTrue(events.contains(result));
        assertEquals(result, engine.lastStoryResult().orElseThrow());
    }

    @Test
    void choixIndisponibleRefuse() {
        open(story(StoryChoice.when("Riche", "", s -> s.choomies() >= 100, "il te faut 100 Choomies",
                ctx -> ctx.choomies(-100)), StoryChoice.of("Partir", "", ctx -> ctx.say("Bye."))));
        assertFalse(engine.chooseStory(0));
        assertTrue(events.contains(new GameEvent.ActionRejected(GameEvent.ActionRejected.Reason.CHOICE_UNAVAILABLE)));
        assertTrue(engine.story().isPresent(), "l'événement reste ouvert");
        assertFalse(engine.chooseStory(7));
        assertTrue(engine.chooseStory(1));
    }

    @Test
    void lesDegatsNeTuentJamais() {
        open(story(StoryChoice.of("Aïe", "", ctx -> ctx.hurt(500))));
        engine.chooseStory(0);
        assertEquals(1, state.player().hp(), 1e-9);
        assertFalse(state.isLost());
    }

    @Test
    void soinAvecEfficaciteEtPlafond() {
        open(story(StoryChoice.of("Soin", "", ctx -> ctx.heal(20))));
        state.player().takeDamage(50);
        state.playerStats().add(StatModifier.percent(StatType.HEAL_EFFICIENCY, 0.5, "test"));
        engine.chooseStory(0);
        assertEquals(80, state.player().hp(), 1e-9);
    }

    @Test
    void gueuleDeBoisAuProchainCombat() {
        open(story(StoryChoice.of("Boire", "", ctx -> ctx.hangover())));
        engine.chooseStory(0);
        assertEquals(1, state.pendingIntensity(Curse.HANGOVER));
        assertTrue(events.contains(new GameEvent.CurseGained(Curse.HANGOVER)));
    }

    @Test
    void chanceDoubleeParClaudeMd() {
        int plain = 0;
        int lucky = 0;
        for (long seed = 0; seed < 400; seed++) {
            plain += win(seed, false);
            lucky += win(seed, true);
        }
        assertTrue(lucky > plain * 1.4, "CLAUDE.md : " + lucky + " contre " + plain);
    }

    private int win(long seed, boolean claude) {
        GameState s = GameState.newGame();
        CombatEngine e = new CombatEngine(s, new CardPool(), new Rng(seed), new EventBus());
        e.start();
        if (claude) {
            e.equip(cardistry.items.ItemRegistry.trinket(cardistry.items.ItemRegistry.CLAUDE_MD));
        }
        e.startStory(story(StoryChoice.of("Tenter", "", ctx -> ctx.choomies(ctx.chance(0.3) ? 1 : 0))));
        e.chooseStory(0);
        return s.choomies() - GameState.STARTING_CHOOMIES;
    }

    @Test
    void pasDEvenementPendantUnCombatNiDeuxALaFois() {
        open(story(StoryChoice.of("Ok", "", ctx -> ctx.say("ok"))));
        assertThrows(IllegalStateException.class, () -> engine.startStory(story(StoryChoice.of("x", "", c -> { }))));
        assertThrows(IllegalStateException.class, engine::openShop);
    }
}

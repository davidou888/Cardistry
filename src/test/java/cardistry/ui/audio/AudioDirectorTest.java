package cardistry.ui.audio;

import static cardistry.model.Element.FIRE;
import static cardistry.model.Element.NATURE;
import static cardistry.model.Element.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Curse;
import cardistry.core.GameState;
import cardistry.core.event.EventBus;
import cardistry.core.event.GameEvent;
import cardistry.items.ItemRegistry;
import cardistry.model.Enemy;
import cardistry.model.Player;
import cardistry.model.card.CardPool;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.model.enemy.EnemyRegistry;
import cardistry.run.Run;
import cardistry.ui.GameLoop;
import cardistry.ui.Input;
import cardistry.ui.Renderer;
import cardistry.ui.TitleChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Ce qui sonne : effets selon les événements, musique selon l'écran. Aucun son joué. */
class AudioDirectorTest {

    /** Audio qui enregistre au lieu de jouer. */
    static final class Recorder implements Audio {
        final List<String> effects = new ArrayList<>();
        final List<MusicContext> music = new ArrayList<>();

        @Override
        public void effect(String name) {
            effects.add(name);
        }

        @Override
        public void music(MusicContext context) {
            if (music.isEmpty() || music.get(music.size() - 1) != context) {
                music.add(context);
            }
        }

        @Override
        public void setLevels(int m, int e, boolean muted) {
        }

        @Override
        public void close() {
        }
    }

    private static Optional<String> fx(GameEvent event) {
        return AudioDirector.effectFor(event);
    }

    @Test
    void cartesParType() {
        assertEquals(Optional.of("sfx.card.fire"), fx(new GameEvent.CardPlayed(new SpellCard("a", FIRE, 1, cardistry.items.Rarity.COMMON))));
        assertEquals(Optional.of("sfx.card.water"), fx(new GameEvent.CardPlayed(new SpellCard("a", WATER, 1, cardistry.items.Rarity.COMMON))));
        assertEquals(Optional.of("sfx.card.plant"), fx(new GameEvent.CardPlayed(new SpellCard("a", NATURE, 1, cardistry.items.Rarity.COMMON))));
        assertEquals(Optional.of("sfx.card.heal"), fx(new GameEvent.CardPlayed(new HealCard("a", FIRE, 1, cardistry.items.Rarity.COMMON))));
        assertEquals(Optional.of("sfx.card.terrain"), fx(new GameEvent.CardPlayed(new TerrainCard("a", FIRE, cardistry.items.Rarity.COMMON))));
    }

    @Test
    void combat() {
        Enemy enemy = new Enemy("E", FIRE, 10, 1);
        Player player = new Player(10);
        assertEquals(Optional.of("sfx.hit.enemy"), fx(new GameEvent.DamageDealt(enemy, 5, 1,
                GameEvent.DamageDealt.Source.SPELL)));
        assertEquals(Optional.of("sfx.hit.player"), fx(new GameEvent.DamageDealt(player, 5, 1,
                GameEvent.DamageDealt.Source.ENEMY_ATTACK)));
        assertEquals(Optional.empty(), fx(new GameEvent.DamageDealt(player, 0, 1, GameEvent.DamageDealt.Source.EFFECT)));
        assertEquals(Optional.of("sfx.dodge"), fx(new GameEvent.EnemyDodged(enemy)));
        assertEquals(Optional.of("sfx.enemy.death"), fx(new GameEvent.EnemyKilled(enemy)));
        Enemy sloth = EnemyRegistry.create(EnemyRegistry.PARESSEUX, 1);
        Enemy choomah = EnemyRegistry.create(EnemyRegistry.CHOOMAH, 1);
        assertEquals(Optional.of("sfx.badoing"), fx(new GameEvent.EnemyMoveUsed(sloth,
                cardistry.model.enemy.EnemyMove.of("badoing", "Badoing, badoing !", "x"))));
        assertEquals(Optional.empty(), fx(new GameEvent.EnemyMoveUsed(sloth, sloth.peekIntent())), "griffe : rien");
        assertEquals(Optional.of("sfx.choomah"), fx(new GameEvent.EnemyMoveUsed(choomah, choomah.peekIntent())));
    }

    @Test
    void runEtObjets() {
        assertEquals(Optional.of("sfx.shop.poor"), fx(new GameEvent.ActionRejected(
                GameEvent.ActionRejected.Reason.NOT_ENOUGH_CHOOMIES)));
        assertEquals(Optional.of("sfx.rejected"), fx(new GameEvent.ActionRejected(
                GameEvent.ActionRejected.Reason.HAND_FULL)));
        assertEquals(Optional.of("sfx.smoke"), fx(new GameEvent.CigaretteSmoked(false)));
        assertEquals(Optional.of("sfx.smoke.googz"), fx(new GameEvent.CigaretteSmoked(true)));
        assertEquals(Optional.of("sfx.roll"), fx(new GameEvent.CigaretteRolled()));
        assertEquals(Optional.of("sfx.drink"), fx(new GameEvent.ItemUsed(ItemRegistry.item("spritz"))));
        assertEquals(Optional.of("sfx.item"), fx(new GameEvent.ItemUsed(ItemRegistry.item("mate"))));
        assertEquals(Optional.of("sfx.hangover"), fx(new GameEvent.CurseStarted(Curse.HANGOVER)));
        assertEquals(Optional.of("sfx.secret"), fx(new GameEvent.SecretUnlocked("Ctrl+Z")));
        assertEquals(Optional.of("sfx.choomies"), fx(new GameEvent.ChoomiesChanged(20, 5)));
        assertEquals(Optional.empty(), fx(new GameEvent.ChoomiesChanged(10, -5)), "un vol n'a pas le son des gains");
        assertEquals(Optional.of("sfx.event.choice"), fx(new GameEvent.StoryResolved("t", "c", List.of())));
        assertEquals(Optional.of("sfx.map.node"), fx(new GameEvent.NodeEntered(1, 1, "Combat")));
        assertEquals(Optional.of("sfx.undo"), fx(new GameEvent.TurnUndone(2)));
        assertEquals(Optional.empty(), fx(new GameEvent.TurnStarted(1)));
    }

    @Test
    void musiqueSelonLaPhase() {
        GameState state = new GameState(new Player(100), List.of(EnemyRegistry.create(EnemyRegistry.VAFI, 1)),
                new TerrainCard("EPFL", FIRE, cardistry.items.Rarity.COMMON), List.of(), new cardistry.core.stats.Stats());
        assertEquals(Optional.of(MusicContext.ELITE), AudioDirector.musicFor(Run.Phase.COMBAT, state));
        assertEquals(Optional.of(MusicContext.MAP), AudioDirector.musicFor(Run.Phase.MAP, state));
        assertEquals(Optional.empty(), AudioDirector.musicFor(Run.Phase.REWARD, state), "la musique du combat continue");
        assertEquals(Optional.of(MusicContext.SHOP), AudioDirector.musicFor(Run.Phase.SHOP, state));
        assertEquals(Optional.of(MusicContext.REST), AudioDirector.musicFor(Run.Phase.REST, state));
        assertEquals(Optional.of(MusicContext.EVENT), AudioDirector.musicFor(Run.Phase.EVENT, state));
        assertEquals(Optional.of(MusicContext.GAME_OVER), AudioDirector.musicFor(Run.Phase.OVER, state));
    }

    /** Une partie entière pilotée par la boucle de jeu : titre, carte, combats, boss… victoire. */
    @Test
    void partieComplete() {
        EventBus bus = new EventBus();
        Recorder audio = new Recorder();
        AudioDirector director = new AudioDirector(audio);
        bus.subscribe(director);
        Run run = new Run(3, Set.of(), bus,
                () -> GameState.newGame(java.util.Collections.nCopies(12, new SpellCard("Météore", FIRE, 10_000, cardistry.items.Rarity.COMMON))),
                () -> new CardPool(List.of(new SpellCard("Météore", FIRE, 10_000, cardistry.items.Rarity.COMMON))));
        Renderer silent = new Renderer() {
            @Override
            public void onEvent(GameEvent event) {
            }

            @Override
            public void showIntro(GameState state) {
            }

            @Override
            public void showStatus(GameState state) {
            }

            @Override
            public void showHand(GameState state) {
            }
        };
        Input input = new Input() {
            @Override
            public TitleChoice title(long seed) {
                return new TitleChoice(false, List.of());
            }

            @Override
            public void waitForContinue() {
            }

            @Override
            public cardistry.core.Action nextAction(GameState state) {
                return new cardistry.core.Action.PlayCard(0);
            }

            @Override
            public int chooseTrinketToDiscard(GameState state) {
                return 0;
            }
        };
        GameLoop.run(run, silent, input, List::of, director);
        assertTrue(run.state().isWon());
        assertEquals(MusicContext.TITLE, audio.music.get(0));
        assertEquals(MusicContext.MAP, audio.music.get(1));
        assertTrue(audio.music.contains(MusicContext.BOSS));
        assertEquals(MusicContext.VICTORY, audio.music.get(audio.music.size() - 1));
        assertTrue(audio.effects.contains("sfx.card.fire"));
        assertTrue(audio.effects.contains("sfx.enemy.death"));
        assertTrue(audio.effects.contains("sfx.level"));
    }
}

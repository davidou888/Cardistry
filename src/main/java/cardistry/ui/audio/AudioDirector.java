package cardistry.ui.audio;

import java.util.Optional;
import java.util.function.Consumer;

import cardistry.core.Curse;
import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.items.Tag;
import cardistry.model.Enemy;
import cardistry.model.card.HealCard;
import cardistry.model.card.SpellCard;
import cardistry.model.card.TerrainCard;
import cardistry.run.Run;

/**
 * Chef d'orchestre : écoute le bus d'événements (les effets) et l'interface (l'écran affiché → la
 * musique, les touches → les sons d'interface). La logique du jeu ne déclenche jamais de son.
 */
public final class AudioDirector implements Consumer<GameEvent> {

    private final Audio audio;

    public AudioDirector(Audio audio) {
        this.audio = audio;
    }

    public Audio audio() {
        return audio;
    }

    @Override
    public void accept(GameEvent event) {
        effectFor(event).ifPresent(audio::effect);
    }

    /** Son d'interface. */
    public void ui(UiSound sound) {
        audio.effect(sound.tableName());
    }

    /** Écran titre. */
    public void title() {
        audio.music(MusicContext.TITLE);
    }

    /** Ce que la partie attend → la musique ; récompenses : la musique du combat continue. */
    public void phase(Run.Phase phase, GameState state) {
        musicFor(phase, state).ifPresent(audio::music);
    }

    /** Musique d'une phase ; vide = ne pas changer. */
    public static Optional<MusicContext> musicFor(Run.Phase phase, GameState state) {
        return switch (phase) {
            case MAP -> Optional.of(MusicContext.MAP);
            case COMBAT -> Optional.of(!state.hasEnemy() ? MusicContext.COMBAT : switch (state.currentEnemy().rank()) {
                case COMMON -> MusicContext.COMBAT;
                case ELITE -> MusicContext.ELITE;
                case BOSS -> MusicContext.BOSS;
            });
            case REWARD, CARD_REWARD, STAT_POINTS -> Optional.empty();
            case REST -> Optional.of(MusicContext.REST);
            case SHOP -> Optional.of(MusicContext.SHOP);
            case EVENT -> Optional.of(MusicContext.EVENT);
            case ALTAR -> Optional.of(MusicContext.EVENT);
            case OVER -> Optional.of(state.isWon() ? MusicContext.VICTORY : MusicContext.GAME_OVER);
        };
    }

    /** Effet d'un événement de jeu (nom dans la table), s'il en a un. */
    public static Optional<String> effectFor(GameEvent event) {
        String name = switch (event) {
            case GameEvent.CardPlayed e -> switch (e.card()) {
                case SpellCard spell -> switch (spell.element()) {
                    case FIRE -> "sfx.card.fire";
                    case WATER -> "sfx.card.water";
                    case NATURE, NEUTRAL, ALGEBRAIC, GEOMETRIC,SPIRIT, GLITCH  -> "sfx.card.plant";
                };
                case HealCard heal -> "sfx.card.heal";
                case TerrainCard terrain -> "sfx.card.terrain";
                default -> null;
            };
            case GameEvent.DamageDealt e -> e.amount() <= 0 ? null
                    : e.target() instanceof Enemy ? "sfx.hit.enemy" : "sfx.hit.player";
            case GameEvent.Healed e -> e.amount() > 0 ? "sfx.heal" : null;
            case GameEvent.EnemyDodged e -> "sfx.dodge";
            case GameEvent.EnemyKilled e -> "sfx.enemy.death";
            case GameEvent.EnemyNulled e -> "sfx.null";
            case GameEvent.ItemBought e -> "sfx.shop.buy";
            case GameEvent.ShopRestocked e -> "sfx.shop.reroll";
            case GameEvent.ActionRejected e -> e.reason() == GameEvent.ActionRejected.Reason.NOT_ENOUGH_CHOOMIES
                    ? "sfx.shop.poor" : "sfx.rejected";
            case GameEvent.CigaretteSmoked e -> e.transformedIntoPetitPetou() ? "sfx.smoke.googz" : "sfx.smoke";
            case GameEvent.CigaretteRolled e -> "sfx.roll";
            case GameEvent.ItemUsed e -> e.item().tags().contains(Tag.DRINK) ? "sfx.drink" : "sfx.item";
            case GameEvent.CurseStarted e -> e.curse() == Curse.HANGOVER ? "sfx.hangover" : null;
            case GameEvent.StoryResolved e -> "sfx.event.choice";
            case GameEvent.SecretUnlocked e -> "sfx.secret";
            case GameEvent.EnemyMoveUsed e -> e.move().id().equals("badoing") ? "sfx.badoing"
                    : e.enemy().voice() == Enemy.Voice.SCREAM ? "sfx.choomah" : null;
            case GameEvent.NodeEntered e -> "sfx.map.node";
            case GameEvent.CardDrawn e -> "sfx.draw";
            case GameEvent.CardAddedToDeck e -> "sfx.deck.add";
            case GameEvent.ChoomiesChanged e -> e.delta() > 0 ? "sfx.choomies" : null;
            case GameEvent.RewardTaken e -> "sfx.reward";
            case GameEvent.LevelStarted e -> "sfx.level";
            case GameEvent.TurnUndone e -> "sfx.undo";
            case GameEvent.RestTaken e -> "sfx.rest";
            case GameEvent.StatusApplied e -> "sfx.status." + e.type().id();
            default -> null;
        };
        return Optional.ofNullable(name);
    }
}

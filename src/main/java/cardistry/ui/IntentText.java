package cardistry.ui;

import static cardistry.ui.Format.num;

import cardistry.core.GameState;
import cardistry.model.Enemy;
import cardistry.model.enemy.EnemyMove;
import cardistry.model.enemy.MoveEffect;
import java.util.ArrayList;
import java.util.List;

/** Textes des attaques adverses (intent, journal), partagés par les deux interfaces. */
public final class IntentText {

    private IntentText() {
    }

    /**
     * Ce que fera l'attaque annoncée, chiffres réels compris (niveau, terrain, réduction), ex.
     * « Charge de parking : 12 ». Pour un adversaire qui crie (Choomah), le nom est déjà un cri.
     */
    public static String intent(GameState state, Enemy enemy) {
        if (state.hasEnemy() && state.currentEnemy() == enemy) {
            java.util.Optional<cardistry.core.status.Status> fons = state.statuses(cardistry.core.status.StatusTarget.ENEMY)
                    .get(cardistry.core.status.StatusType.FONS);
            if (fons.isPresent()) {
                return "Endormi (" + StatusText.turns(fons.get().turnsLeft()) + ")";
            }
        }
        EnemyMove move = enemy.peekIntent();
        String effects = effects(state, move);
        if (enemy.voice() == Enemy.Voice.SCREAM) {
            effects = Scream.of(effects);
        }
        return effects.isEmpty() ? move.name() : move.name() + " : " + effects;
    }

    /**
     * Ligne de journal pour un événement lié aux attaques adverses (ou {@code null} si l'événement
     * n'en est pas un). Le nom de l'adversaire est en tête, le reste est crié par un Choomah.
     */
    public static String journal(cardistry.core.event.GameEvent event) {
        return switch (event) {
            case cardistry.core.event.GameEvent.EnemyMoveUsed e -> e.move().description().equals(e.move().name())
                    ? e.enemy().name() + " : " + e.move().name()
                    : e.enemy().name() + " : " + e.move().name() + " — " + e.move().description();
            case cardistry.core.event.GameEvent.EnemyMissed e -> e.enemy().name() + " " + say(e.enemy(),
                    e.healed() > 0 ? "rate son coup et se soigne de " + num(e.healed()) + " PV" : "rate son coup");
            case cardistry.core.event.GameEvent.DodgeReady e -> e.enemy().name() + " "
                    + say(e.enemy(), "esquivera ton prochain sort");
            case cardistry.core.event.GameEvent.EnemyDodged e -> e.enemy().name() + " " + say(e.enemy(), "esquive ton sort !");
            case cardistry.core.event.GameEvent.EnemyHealed e -> e.enemy().name() + " "
                    + say(e.enemy(), "se soigne de " + num(e.amount()) + " PV");
            case cardistry.core.event.GameEvent.CardCursed e -> e.removed().name() + " devient « " + e.dead().name()
                    + " » (injouable pendant ce combat)";
            case cardistry.core.event.GameEvent.ChoomiesStolen e -> e.enemy().name() + " "
                    + say(e.enemy(), "te vole " + e.amount() + " Choomies !");
            case cardistry.core.event.GameEvent.CigarettesStolen e -> e.enemy().name() + " "
                    + say(e.enemy(), "te prend " + plural(e.amount(), "clope") + " !");
            case cardistry.core.event.GameEvent.ChoomiesChanged e -> e.delta() > 0
                    ? "+" + e.delta() + " Choomies (" + e.choomies() + ")" : null;
            default -> null;
        };
    }

    /** Texte dit par l'adversaire : crié s'il crie (Choomah). */
    public static String say(Enemy enemy, String text) {
        return enemy.voice() == Enemy.Voice.SCREAM ? Scream.of(text) : text;
    }

    /** Effets chiffrés d'une attaque, séparés par « , » ; vide si elle ne fait rien. */
    public static String effects(GameState state, EnemyMove move) {
        List<String> parts = new ArrayList<>();
        for (MoveEffect effect : move.effects()) {
            parts.add(effect(state, effect));
        }
        return String.join(", ", parts);
    }

    private static String effect(GameState state, MoveEffect effect) {
        return switch (effect) {
            case MoveEffect.Hit hit -> {
                String damage = num(state.hitDamage(hit));
                String text = hit.hits() > 1 ? hit.hits() + " × " + damage : damage;
                if (hit.missChance() > 0) {
                    text += " (" + percent(hit.missChance()) + " de rater)";
                }
                yield text;
            }
            case MoveEffect.Pickpocket steal -> "vol de " + steal.choomies() + " Choomies, sinon "
                    + plural(steal.cigarettes(), "clope") + ", sinon " + num(state.hitDamage(steal.fallbackDamage()));
            case MoveEffect.Tax tax -> plural(tax.cigarettes(), "clope") + ", sinon " + tax.fallbackHits() + " × "
                    + num(state.hitDamage(tax.damage()));
            case MoveEffect.Gamble gamble -> percent(gamble.chance()) + " " + effect(state, gamble.onSuccess())
                    + ", sinon " + effect(state, gamble.onFailure());
            case MoveEffect.Dodge dodge -> "esquive ton prochain sort";
            case MoveEffect.CurseCard curse -> "une carte devient « " + curse.cardName() + " »";
            case MoveEffect.HealSelf heal -> "se soigne de "
                    + num(heal.amount() * (state.hasEnemy() ? state.currentEnemy().damageScale() : 1));
            case MoveEffect.Inflict inflict -> (inflict.chance() < 1 ? percent(inflict.chance()) + " de " : "")
                    + cardistry.core.status.StatusType.byId(inflict.status()).displayName()
                    + (inflict.turns() > 0 ? " " + StatusText.turns(inflict.turns()) : "");
        };
    }

    private static String percent(double chance) {
        return num(chance * 100) + " %";
    }

    private static String plural(int count, String word) {
        return count + " " + word + (count > 1 ? "s" : "");
    }
}

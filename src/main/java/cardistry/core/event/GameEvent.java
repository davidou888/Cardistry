package cardistry.core.event;

import cardistry.model.Combatant;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.TerrainCard;

/**
 * Tout ce qui se passe dans la partie. Le moteur les publie sur l'{@link EventBus} ;
 * l'UI (et plus tard les trinkets) les consomment.
 */
public sealed interface GameEvent {

    record GameStarted() implements GameEvent {
    }

    record TurnStarted(int turn) implements GameEvent {
    }

    record CardPlayed(Card card) implements GameEvent {
    }

    record CardDrawn(Card card) implements GameEvent {
    }

    /** {@code amount} = dégâts finaux, {@code multiplier} déjà appliqué. */
    record DamageDealt(Combatant target, double amount, double multiplier) implements GameEvent {
    }

    /** {@code amount} = PV rendus finaux, {@code multiplier} déjà appliqué. */
    record Healed(Combatant target, double amount, double multiplier) implements GameEvent {
    }

    record TerrainChanged(TerrainCard previous, TerrainCard current) implements GameEvent {
    }

    /** L'adversaire attaque ; suivi d'un {@link DamageDealt} sur le joueur. */
    record EnemyAttacked(Enemy enemy) implements GameEvent {
    }

    record EnemyKilled(Enemy enemy) implements GameEvent {
    }

    record EnemyArrived(Enemy enemy) implements GameEvent {
    }

    record ActionRejected(Reason reason) implements GameEvent {

        public enum Reason {
            /** Numéro hors des slots de la main. */
            INVALID_CHOICE,
            /** Pioche impossible : main pleine. */
            HAND_FULL,
            /** Le slot choisi ne contient pas de carte. */
            EMPTY_SLOT
        }
    }

    record GameWon() implements GameEvent {
    }

    record GameLost() implements GameEvent {
    }
}

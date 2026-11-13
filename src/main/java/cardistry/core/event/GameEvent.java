package cardistry.core.event;

import cardistry.items.Item;
import cardistry.model.Combatant;
import cardistry.model.Enemy;
import cardistry.model.card.Card;
import cardistry.model.card.TerrainCard;
import java.util.List;

/**
 * Tout ce qui se passe dans la partie. Le moteur les publie sur l'{@link EventBus} ;
 * l'UI (et plus tard les trinkets) les consomment.
 */
public sealed interface GameEvent {

    record GameStarted(long seed) implements GameEvent {
    }

    record TurnStarted(int turn) implements GameEvent {
    }

    record CardPlayed(Card card) implements GameEvent {
    }

    record CardDrawn(Card card) implements GameEvent {
    }

    record ItemUsed(Item item) implements GameEvent {
    }

    /** {@code amount} = dégâts finaux, {@code multiplier} déjà appliqué. */
    record DamageDealt(Combatant target, double amount, double multiplier) implements GameEvent {
    }

    /** {@code amount} = PV réellement rendus (multiplicateur appliqué, plafonné aux PV max). */
    record Healed(Combatant target, double amount, double multiplier) implements GameEvent {
    }

    record TerrainChanged(TerrainCard previous, TerrainCard current) implements GameEvent {
    }

    /** L'adversaire attaque ; suivi d'un {@link DamageDealt} sur le joueur. */
    record EnemyAttacked(Enemy enemy) implements GameEvent {
    }

    record EnemyKilled(Enemy enemy) implements GameEvent {
    }

    /**
     * Début d'un combat (un par adversaire), y compris le premier.
     *
     * @param number numéro du combat, à partir de 1
     * @param total nombre de combats de la partie
     */
    record CombatStarted(Enemy enemy, int number, int total) implements GameEvent {
    }

    /** Nouvelle main distribuée en début de combat (l'ancienne est défaussée). */
    record HandDealt(List<Card> cards) implements GameEvent {

        public HandDealt {
            cards = List.copyOf(cards);
        }
    }

    record ActionRejected(Reason reason) implements GameEvent {

        public enum Reason {
            /** Numéro hors des slots de la main. */
            INVALID_CHOICE,
            /** Pioche impossible : main pleine. */
            HAND_FULL,
            /** Le slot choisi ne contient pas de carte. */
            EMPTY_SLOT,
            /** Pas d'objet à cet emplacement. */
            NO_ITEM
        }
    }

    record GameWon() implements GameEvent {
    }

    record GameLost() implements GameEvent {
    }
}

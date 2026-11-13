package cardistry.core.event;

import cardistry.core.Curse;
import cardistry.core.TemporaryEffect;
import cardistry.items.Collectible;
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

    /** Une carte de la main a été remplacée par une autre (effet). */
    record CardReplaced(Card removed, Card replacement) implements GameEvent {
    }

    /**
     * {@code amount} = dégâts finaux, {@code multiplier} déjà appliqué.
     *
     * @param source ce qui a causé les dégâts
     */
    record DamageDealt(Combatant target, double amount, double multiplier, Source source) implements GameEvent {

        public enum Source {
            /** Sortilège joué depuis la main. */
            SPELL,
            /** Effet d'objet ou de trinket. */
            EFFECT,
            /** Attaque de l'adversaire. */
            ENEMY_ATTACK
        }
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

    /** L'Ivresse change ; {@code threshold} = seuil de Gueule de bois actuel. */
    record DrunkennessChanged(int drunkenness, int threshold) implements GameEvent {
    }

    /** Nouvel effet temporaire (actif tout de suite ou en attente). */
    record EffectAdded(TemporaryEffect effect) implements GameEvent {
    }

    /** Un effet en attente se déclenche. */
    record EffectActivated(TemporaryEffect effect) implements GameEvent {
    }

    /** Un effet se termine (durée, charges épuisées ou fin de combat). */
    record EffectEnded(TemporaryEffect effect) implements GameEvent {
    }

    /** Le joueur saute ce tour ; {@code causes} = noms des effets responsables. */
    record PlayerTurnSkipped(List<String> causes) implements GameEvent {

        public PlayerTurnSkipped {
            causes = List.copyOf(causes);
        }
    }

    /** L'attaque de l'adversaire est annulée par un effet. */
    record AttackCancelled(Enemy enemy, String cause) implements GameEvent {
    }

    /** Malédiction obtenue : elle s'activera au prochain combat. */
    record CurseGained(Curse curse) implements GameEvent {
    }

    /** Malédiction active pour ce combat. */
    record CurseStarted(Curse curse) implements GameEvent {
    }

    /** Malédiction terminée (fin du combat). */
    record CurseEnded(Curse curse) implements GameEvent {
    }

    /** Récompenses proposées après la mort d'un adversaire ; le moteur attend un choix. */
    record RewardOffered(List<Collectible> options) implements GameEvent {

        public RewardOffered {
            options = List.copyOf(options);
        }
    }

    /** Récompense prise ; {@code replaced} est l'item retiré pour lui faire de la place, ou null. */
    record RewardTaken(Collectible taken, Collectible replaced) implements GameEvent {
    }

    record RewardSkipped() implements GameEvent {
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
            NO_ITEM,
            /** Proposition de récompense inexistante. */
            INVALID_REWARD,
            /** Inventaire plein : il faut choisir quoi remplacer. */
            INVENTORY_FULL
        }
    }

    record GameWon() implements GameEvent {
    }

    record GameLost() implements GameEvent {
    }
}

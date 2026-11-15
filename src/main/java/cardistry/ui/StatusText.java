package cardistry.ui;

import cardistry.core.GameState;
import cardistry.core.event.GameEvent;
import cardistry.core.status.Status;
import cardistry.core.status.StatusTarget;
import cardistry.core.status.StatusType;
import java.util.List;

/** Textes des statuts (badges, journal), partagés par la fenêtre, le terminal et le mode texte. */
public final class StatusText {

    private StatusText() {
    }

    /** Abréviation de 3 lettres (badge sans icône). */
    public static String abbreviation(StatusType type) {
        return switch (type) {
            case INTOX -> "Int";
            case FONS -> "Fon";
            case HYPNOTIZED -> "Hyp";
            case BONDAGE -> "Bon";
            case BURN -> "Brû";
        };
    }

    /** Badge texte : « Int3 ». */
    public static String badge(Status status) {
        return abbreviation(status.type()) + status.turnsLeft();
    }

    /** Version longue : « Intox (2 tours) ». */
    public static String describe(Status status) {
        return status.type().displayName() + " (" + turns(status.turnsLeft()) + ")";
    }

    /** « Intox (2 tours), Fons (1 tour) », ou vide. */
    public static String describeAll(List<Status> statuses) {
        return String.join(", ", statuses.stream().map(StatusText::describe).toList());
    }

    public static String turns(int n) {
        return n + (n > 1 ? " tours" : " tour");
    }

    /** « Tu » ou le nom de l'adversaire actuel. */
    private static String who(GameState state, StatusTarget target) {
        return target == StatusTarget.PLAYER ? "Tu" : state.currentEnemy().name();
    }

    /** Ligne de journal d'un événement de statut, ou {@code null} si ce n'en est pas un. */
    public static String journal(GameState state, GameEvent event) {
        return switch (event) {
            case GameEvent.StatusApplied e -> e.refreshed()
                    ? e.type().displayName() + " de " + whom(state, e.target()) + " : remis à " + turns(e.turns())
                    : (e.target() == StatusTarget.PLAYER ? "Tu subis " : who(state, e.target()) + " subit ")
                            + e.type().displayName() + " (" + turns(e.turns()) + ")";
            case GameEvent.StatusRefused e -> e.type().displayName() + " sur " + whom(state, e.target())
                    + " : sans effet (" + e.reason() + ")";
            case GameEvent.StatusEnded e -> e.type().displayName() + " de " + whom(state, e.target()) + " : terminé";
            default -> null;
        };
    }

    /** « toi » ou le nom de l'adversaire. */
    private static String whom(GameState state, StatusTarget target) {
        return target == StatusTarget.PLAYER ? "toi" : state.currentEnemy().name();
    }
}

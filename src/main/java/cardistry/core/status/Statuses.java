package cardistry.core.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Statuts portés par un combattant (au plus un de chaque type), dans l'ordre d'arrivée. */
public final class Statuses {

    /** Issue d'une tentative d'application. */
    public enum Outcome {
        APPLIED,
        /** Déjà actif : durée remise au max. */
        REFRESHED,
        /** Déjà actif et non réapplicable (Intox). */
        REFUSED
    }

    private final List<Status> active = new ArrayList<>();

    public List<Status> all() {
        return Collections.unmodifiableList(active);
    }

    public Optional<Status> get(StatusType type) {
        return active.stream().filter(s -> s.type() == type).findFirst();
    }

    public boolean has(StatusType type) {
        return get(type).isPresent();
    }

    public boolean isEmpty() {
        return active.isEmpty();
    }

    /** Applique {@code type} pour {@code turns} tours, selon sa règle de réapplication. */
    public Outcome apply(StatusType type, int turns, StatusTarget inflictedBy) {
        Optional<Status> current = get(type);
        if (current.isEmpty()) {
            active.add(new Status(type, turns, inflictedBy));
            return Outcome.APPLIED;
        }
        if (type.reapply() == StatusType.Reapply.REFUSE) {
            return Outcome.REFUSED;
        }
        current.get().reset(turns);
        return Outcome.REFRESHED;
    }

    /** Fin du tour de la cible : chaque statut perd un tour ; renvoie ceux qui se terminent. */
    public List<Status> countDown() {
        List<Status> ended = new ArrayList<>();
        for (Status status : List.copyOf(active)) {
            if (status.countDown()) {
                active.remove(status);
                ended.add(status);
            }
        }
        return ended;
    }

    /** Retire un statut ; {@code true} s'il était actif. */
    public boolean remove(StatusType type) {
        return active.removeIf(s -> s.type() == type);
    }

    /** Retire tout (fin du combat). */
    public List<Status> clear() {
        List<Status> removed = List.copyOf(active);
        active.clear();
        return removed;
    }
}

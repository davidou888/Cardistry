package cardistry.run;

import java.util.EnumSet;
import java.util.Set;

/** Où sont gardés les secrets débloqués d'une partie à l'autre (fichier, ou mémoire pour les tests). */
public interface UnlockStore {

    /** Secrets débloqués ; vide si rien n'est sauvegardé ou si la sauvegarde est illisible. */
    Set<Secret> load();

    /** Remplace la sauvegarde ; une erreur d'écriture ne doit pas faire planter le jeu. */
    void save(Set<Secret> unlocked);

    /** Efface tous les déblocages ({@code --reset-unlocks}). */
    default void reset() {
        save(EnumSet.noneOf(Secret.class));
    }

    /** Sauvegarde en mémoire (tests, ou quand le dossier de sauvegarde est inutilisable). */
    final class InMemory implements UnlockStore {

        private Set<Secret> unlocked = EnumSet.noneOf(Secret.class);

        @Override
        public Set<Secret> load() {
            return unlocked.isEmpty() ? EnumSet.noneOf(Secret.class) : EnumSet.copyOf(unlocked);
        }

        @Override
        public void save(Set<Secret> unlocked) {
            this.unlocked = unlocked.isEmpty() ? EnumSet.noneOf(Secret.class) : EnumSet.copyOf(unlocked);
        }
    }
}

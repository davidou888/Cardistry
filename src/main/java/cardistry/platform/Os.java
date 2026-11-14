package cardistry.platform;

import java.util.Locale;

/** Système d'exploitation, pour les quelques comportements qui en dépendent (sauvegarde, lancement). */
public enum Os {
    LINUX,
    MAC,
    WINDOWS;

    /** OS de cette machine. */
    public static Os current() {
        return detect(System.getProperty("os.name", ""));
    }

    /** Linux par défaut (BSD et autres Unix se comportent pareil pour le jeu). */
    public static Os detect(String osName) {
        String name = osName.toLowerCase(Locale.ROOT);
        if (name.startsWith("windows")) {
            return WINDOWS;
        }
        if (name.startsWith("mac") || name.startsWith("darwin")) {
            return MAC;
        }
        return LINUX;
    }
}

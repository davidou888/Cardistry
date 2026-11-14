package cardistry.run;

import cardistry.items.ItemRegistry;
import java.util.Optional;

/**
 * Les trinkets Secret (spoilers : docs/SECRETS.md). Un secret débloqué rejoint le pool des
 * récompenses à partir de la partie suivante ; un code le donne équipé pour une seule partie.
 */
public enum Secret {
    NULL_POINTER_EXCEPTION(ItemRegistry.NULL_POINTER_EXCEPTION),
    CTRL_Z(ItemRegistry.CTRL_Z),
    CLAUDE_MD(ItemRegistry.CLAUDE_MD);

    private final String trinketId;

    Secret(String trinketId) {
        this.trinketId = trinketId;
    }

    /** Identifiant du trinket (aussi utilisé dans le fichier de sauvegarde). */
    public String trinketId() {
        return trinketId;
    }

    /** Nom affiché, celui du trinket. */
    public String displayName() {
        return ItemRegistry.trinket(trinketId).name();
    }

    public static Optional<Secret> byTrinketId(String id) {
        for (Secret secret : values()) {
            if (secret.trinketId.equals(id)) {
                return Optional.of(secret);
            }
        }
        return Optional.empty();
    }
}

package cardistry.ui.sprite;

import cardistry.model.Enemy;
import cardistry.model.enemy.EnemyRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Sprites des adversaires, lus dans {@code src/main/resources/sprites/<id>.txt} (mis en cache). */
public final class Sprites {

    /** Sprite utilisé quand un adversaire n'a pas de fichier. */
    public static final String UNKNOWN = "inconnu";

    private static final Map<String, Optional<Sprite>> CACHE = new ConcurrentHashMap<>();

    private Sprites() {
    }

    /** Sprite d'un adversaire ({@link Enemy#spriteId()}), sinon le sprite inconnu. */
    public static Sprite of(Enemy enemy) {
        return byId(enemy.spriteId()).or(() -> byId(UNKNOWN)).orElseGet(() -> Sprite.placeholder(enemy.name()));
    }

    /** Sprite du fichier {@code sprites/<id>.txt}, s'il existe. */
    public static Optional<Sprite> byId(String id) {
        return CACHE.computeIfAbsent(id, Sprites::load);
    }

    /**
     * Un adversaire par sprite, dans l'ordre du registre (visionneuse {@code --sprites}) : le nom
     * affiché est celui du premier adversaire qui l'utilise.
     */
    public static List<Map.Entry<String, Sprite>> catalog() {
        Map<String, Sprite> byId = new LinkedHashMap<>();
        for (String id : EnemyRegistry.ids()) {
            Enemy enemy = EnemyRegistry.create(id, 1);
            byId.putIfAbsent(enemy.spriteId(), of(enemy));
        }
        byId.putIfAbsent(UNKNOWN, byId(UNKNOWN).orElse(Sprite.placeholder("?")));
        return new ArrayList<>(byId.entrySet());
    }

    private static Optional<Sprite> load(String id) {
        try (InputStream in = Sprites.class.getResourceAsStream("/sprites/" + id + ".txt")) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(Sprite.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

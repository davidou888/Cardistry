package cardistry.ui.lanterna;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bannières ASCII, lues dans src/main/resources (les sprites : {@code ui.sprite.Sprites}). */
final class Resources {

    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private Resources() {
    }

    static List<String> banner(String name) {
        List<String> banner = load("/banners/" + name + ".txt");
        return banner != null ? banner : List.of(name.toUpperCase(Locale.ROOT));
    }

    /** "Sirène" → "sirene". */
    static String key(String name) {
        return cardistry.model.Ids.slug(name);
    }

    private static List<String> load(String path) {
        return CACHE.computeIfAbsent(path, p -> {
            try (InputStream in = Resources.class.getResourceAsStream(p)) {
                if (in == null) {
                    return null;
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                        .map(String::stripTrailing).toList();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}

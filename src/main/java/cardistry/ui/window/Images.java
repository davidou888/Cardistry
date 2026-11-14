package cardistry.ui.window;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Images PNG du jeu, lues dans {@code src/main/resources/images/} et mises en cache :
 * <ul>
 *   <li>{@code images/enemies/<sprite>.png} : un adversaire ; {@code <sprite>.<animation>.<n>.png} :
 *       la frame n (à partir de 1) d'une animation (idle, attack, hurt, death) ;</li>
 *   <li>{@code images/items/<id>.png} : l'icône d'un objet ou d'un trinket.</li>
 * </ul>
 * Image absente ou illisible : rien (l'interface garde le sprite ASCII).
 */
public final class Images {

    private static final Map<String, Optional<BufferedImage>> CACHE = new ConcurrentHashMap<>();

    private Images() {
    }

    /** Chemin de l'image d'un adversaire pour une frame : la frame si elle existe, sinon l'image de base. */
    public static Optional<String> enemy(String spriteId, String animation, int frame) {
        String framePath = "enemies/" + spriteId + "." + animation + "." + (frame + 1) + ".png";
        if (get(framePath).isPresent()) {
            return Optional.of(framePath);
        }
        String idle = "enemies/" + spriteId + ".idle." + (frame + 1) + ".png";
        if (animation.equals("idle") && get(idle).isPresent()) {
            return Optional.of(idle);
        }
        String base = "enemies/" + spriteId + ".png";
        return get(base).isPresent() ? Optional.of(base) : Optional.empty();
    }

    /** Nombre de frames idle en images (0 s'il n'y en a pas). */
    public static int idleFrames(String spriteId) {
        int count = 0;
        while (get("enemies/" + spriteId + ".idle." + (count + 1) + ".png").isPresent()) {
            count++;
        }
        return count;
    }

    /** Chemin de l'icône d'un objet ou trinket, si elle existe. */
    public static Optional<String> item(String id) {
        String path = "items/" + id + ".png";
        return get(path).isPresent() ? Optional.of(path) : Optional.empty();
    }

    /** Image au chemin {@code path} (relatif à {@code images/}). */
    public static Optional<BufferedImage> get(String path) {
        return CACHE.computeIfAbsent(path, Images::load);
    }

    private static Optional<BufferedImage> load(String path) {
        try (InputStream in = Images.class.getResourceAsStream("/images/" + path)) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(ImageIO.read(in));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}

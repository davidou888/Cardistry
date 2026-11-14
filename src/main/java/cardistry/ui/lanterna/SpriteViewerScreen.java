package cardistry.ui.lanterna;

import cardistry.ui.sprite.Sprite;
import com.googlecode.lanterna.TextColor;
import java.util.List;
import java.util.Map;

/** Visionneuse de sprites ({@code --sprites}) : un sprite et ses animations. */
final class SpriteViewerScreen {

    static final String HELP = "←/→ sprite · 1 idle · 2 attaque · 3 dégâts · 4 mort · Q quitter";

    private SpriteViewerScreen() {
    }

    /**
     * @param flash dégâts reçus sans frame « hurt » : sprite en blanc vif
     * @param hidden mort sans frame « death » : clignotement
     */
    static void draw(Painter p, List<Map.Entry<String, Sprite>> catalog, int index, String animation, int frame,
            boolean flash, boolean hidden) {
        Map.Entry<String, Sprite> entry = catalog.get(index);
        Sprite sprite = entry.getValue();
        p.frame("Sprites", (index + 1) + "/" + catalog.size(), HELP);
        p.centered(2, sprite.name(), Palette.ACCENT, true);
        p.centered(3, "sprites/" + entry.getKey() + ".txt", Palette.DIM, false);
        p.centered(4, describe(sprite), Palette.TEXT, false);

        List<Sprite.Frame> frames = sprite.frames(animation);
        Sprite.Frame shown = frames.isEmpty()
                ? sprite.idle().get(Math.floorMod(frame, sprite.idle().size()))
                : frames.get(Math.floorMod(frame, frames.size()));
        String label = animation + (frames.isEmpty() ? " (générique)" : " " + (Math.floorMod(frame, frames.size()) + 1)
                + "/" + frames.size());
        p.centered(6, label, Palette.INFO, false);
        // Fenêtre avec une image : l'image à gauche, le repli ASCII à droite.
        java.util.Optional<String> image = p.hasImages()
                ? cardistry.ui.window.Images.enemy(entry.getKey(), animation, Math.max(0, frame)) : java.util.Optional.empty();
        if (image.isPresent()) {
            int half = p.cols() / 2;
            p.text(half / 2 - 2, 7, "image", Palette.DIM);
            p.text(half + half / 2 - 7, 7, "ASCII (repli)", Palette.DIM);
            if (!hidden) {
                p.image(Math.max(1, half / 2 - Sprite.MAX_WIDTH / 2), 8, Sprite.MAX_WIDTH, Sprite.MAX_HEIGHT,
                        image.get(), flash ? TextColor.ANSI.WHITE_BRIGHT : null, 1f);
                SpriteArt.draw(p, shown, half + Math.max(0, (half - sprite.width()) / 2), 8, p.rows() - 3, Palette.TEXT,
                        flash ? TextColor.ANSI.WHITE_BRIGHT : null, p.cols() - 1);
            }
            return;
        }
        if (!hidden) {
            int col = (p.cols() - sprite.width()) / 2;
            int top = 8 + Math.max(0, (Sprite.MAX_HEIGHT - sprite.height()) / 2);
            SpriteArt.draw(p, shown, col, top, p.rows() - 3, Palette.TEXT, flash ? TextColor.ANSI.WHITE_BRIGHT : null,
                    p.cols() - 1);
        }
    }

    /** « idle ×2 · attack · hurt » (« ×N » quand il y a plusieurs frames). */
    static String describe(Sprite sprite) {
        StringBuilder text = new StringBuilder();
        for (String animation : Sprite.ANIMATIONS) {
            int count = sprite.frames(animation).size();
            if (count == 0) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(" · ");
            }
            text.append(animation).append(count > 1 ? " ×" + count : "");
        }
        return text.toString();
    }
}

package cardistry.ui.text;

import cardistry.ui.sprite.Sprite;
import java.util.List;
import java.util.Map;

/** Visionneuse de sprites en mode texte : chaque sprite et toutes ses frames, Entrée pour passer au suivant. */
public final class SpriteViewerText {

    private SpriteViewerText() {
    }

    public static void run(Console console) {
        List<Map.Entry<String, Sprite>> catalog = cardistry.ui.sprite.Sprites.catalog();
        for (int i = 0; i < catalog.size(); i++) {
            Sprite sprite = catalog.get(i).getValue();
            console.println();
            console.println(Ansi.yellow(sprite.name()) + "  (" + (i + 1) + "/" + catalog.size() + ", sprites/"
                    + catalog.get(i).getKey() + ".txt)");
            for (String animation : Sprite.ANIMATIONS) {
                List<Sprite.Frame> frames = sprite.frames(animation);
                for (int f = 0; f < frames.size(); f++) {
                    console.println("-- " + animation + " " + (f + 1) + "/" + frames.size());
                    frames.get(f).lines().forEach(console::println);
                }
            }
            if (i < catalog.size() - 1) {
                console.print("(Entrée : sprite suivant, q : quitter) ");
                String line = console.readLine();
                if (line == null || line.strip().equalsIgnoreCase("q")) {
                    return;
                }
            }
        }
    }
}

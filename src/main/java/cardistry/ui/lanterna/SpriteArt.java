package cardistry.ui.lanterna;

import cardistry.ui.sprite.Sprite;
import com.googlecode.lanterna.TextColor;

/** Dessin d'une frame de sprite, caractère par caractère avec sa carte de couleurs. */
final class SpriteArt {

    private SpriteArt() {
    }

    /**
     * @param lastRow dernière ligne où dessiner
     * @param base couleur par défaut (élément de l'adversaire)
     * @param flash si non null, toute la frame dans cette couleur (dégâts reçus)
     * @param maxCol première colonne à ne pas dépasser (cadre)
     */
    static void draw(Painter p, Sprite.Frame frame, int col, int top, int lastRow, TextColor base, TextColor flash,
            int maxCol) {
        for (int row = 0; row < frame.lines().size() && top + row <= lastRow; row++) {
            String line = frame.lines().get(row);
            for (int i = 0; i < line.length(); i++) {
                int x = col + i;
                char c = line.charAt(i);
                if (c == ' ' || x < 1 || x >= maxCol) {
                    continue;
                }
                TextColor color = flash != null ? flash : Palette.sprite(frame.colorAt(i, row), base);
                p.text(x, top + row, String.valueOf(c), color, flash != null);
            }
        }
    }
}

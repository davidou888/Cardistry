package cardistry.ui.lanterna;

import cardistry.core.status.Status;
import cardistry.ui.StatusText;
import java.util.List;

/**
 * Badges de statut à côté d'une barre de PV : icône 8×8 (2 colonnes) + tours restants dans la
 * fenêtre ; « Int3 » en texte sinon ({@code --terminal}, {@code --no-images}, icône absente).
 */
final class StatusBadges {

    private StatusBadges() {
    }

    static String icon(Status status) {
        return "status/" + status.type().id() + ".png";
    }

    private static boolean withImage(Painter p, Status status) {
        return p.hasImages() && cardistry.ui.window.Images.get(icon(status)).isPresent();
    }

    private static int entryWidth(Painter p, Status status) {
        int turns = String.valueOf(status.turnsLeft()).length();
        return withImage(p, status) ? 2 + turns : StatusText.badge(status).length();
    }

    /** Largeur de tous les badges, espaces compris (0 sans statut). */
    static int width(Painter p, List<Status> statuses) {
        int width = 0;
        for (Status status : statuses) {
            width += (width == 0 ? 0 : 1) + entryWidth(p, status);
        }
        return width;
    }

    /**
     * Dessine les badges à partir de {@code col}, sans dépasser {@code maxWidth} colonnes (au-delà :
     * « +N »).
     */
    static void draw(Painter p, int col, int row, List<Status> statuses, int maxWidth) {
        int used = 0;
        for (int i = 0; i < statuses.size(); i++) {
            Status status = statuses.get(i);
            int w = entryWidth(p, status);
            int rest = statuses.size() - i - 1;
            int reserve = rest > 0 ? 3 : 0; // « +N » si la suite ne tient pas
            if (used + (used == 0 ? 0 : 1) + w + reserve > maxWidth && !(rest == 0 && used + 1 + w <= maxWidth)) {
                String more = "+" + (statuses.size() - i);
                if (used + 1 + more.length() <= maxWidth) {
                    p.text(col + used + (used == 0 ? 0 : 1), row, more, Palette.DIM);
                }
                return;
            }
            int at = col + used + (used == 0 ? 0 : 1);
            String turns = String.valueOf(status.turnsLeft());
            if (withImage(p, status) && p.image(at, row, 2, 1, icon(status), null, 1f)) {
                p.text(at + 2, row, turns, Palette.status(status.type()), true);
            } else {
                p.text(at, row, StatusText.badge(status), Palette.status(status.type()), true);
            }
            used = at - col + w;
        }
    }
}

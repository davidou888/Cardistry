package cardistry.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cardistry.core.Rng;
import cardistry.run.map.MapGenerator;
import cardistry.run.map.MapNode;
import cardistry.run.map.RunMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapLayoutTest {

    /** Zone de la carte en 80×24 et en 100×32 (unités du calque). */
    private static final int[][] AREAS = {{518, 245}, {658, 357}};

    private static RunMap map(long seed) {
        return MapGenerator.generate(new Rng(seed).fork("carte-1"), 1);
    }

    private static List<List<MapLayout.Point>> dots(MapLayout layout) {
        return layout.paths().stream().map(MapLayout.Path::dots).toList();
    }

    @Test
    void memeSeedMemeDessin() {
        RunMap map = map(42);
        MapLayout a = MapLayout.of(map, 42, 658, 357);
        MapLayout b = MapLayout.of(map(42), 42, 658, 357);
        for (MapNode node : map.nodes()) {
            assertEquals(a.position(node), b.position(node));
        }
        assertEquals(dots(a), dots(b));
    }

    @Test
    void autreSeedAutreDessin() {
        RunMap map = map(42);
        assertNotEquals(dots(MapLayout.of(map, 42, 658, 357)), dots(MapLayout.of(map, 43, 658, 357)));
    }

    @Test
    void leDessinNeToucheNiLaCarteNiSonSousFlux() {
        RunMap before = map(7);
        MapLayout.of(before, 7, 658, 357);
        assertEquals(before.nodes(), map(7).nodes());
    }

    @Test
    void tailleDesIcones() {
        assertEquals(24, MapLayout.of(map(1), 1, 518, 245).iconSize(), "80×24 : icônes ×1,5");
        assertEquals(32, MapLayout.of(map(1), 1, 658, 357).iconSize(), "100×32 : icônes ×2");
        RunMap map = map(1);
        assertEquals(32 + MapLayout.BOSS_EXTRA, MapLayout.of(map, 1, 658, 357).size(map.boss()));
    }

    @Test
    void departAGaucheBossADroiteAuMilieu() {
        for (long seed = 0; seed < 50; seed++) {
            RunMap map = map(seed);
            MapLayout layout = MapLayout.of(map, seed, 658, 357);
            MapLayout.Point boss = layout.position(map.boss());
            assertEquals(357 / 2, boss.y(), 1);
            for (MapNode node : map.nodes()) {
                if (node != map.boss()) {
                    assertTrue(layout.position(node).x() < boss.x(), "le boss est le plus à droite");
                }
                for (MapNode next : map.choicesAfter(node)) {
                    assertTrue(layout.position(node).x() < layout.position(next).x(), "on avance vers la droite");
                }
            }
        }
    }

    @Test
    void iconesDansLaZoneEtSansChevauchement() {
        for (int[] area : AREAS) {
            for (long seed = 0; seed < 200; seed++) {
                RunMap map = map(seed);
                MapLayout layout = MapLayout.of(map, seed, area[0], area[1]);
                List<MapNode> nodes = map.nodes();
                for (int i = 0; i < nodes.size(); i++) {
                    MapLayout.Point p = layout.position(nodes.get(i));
                    int half = layout.size(nodes.get(i)) / 2;
                    assertTrue(p.x() - half >= 0 && p.x() + half <= area[0] && p.y() - half >= 0
                            && p.y() + half <= area[1], "seed " + seed + " : " + nodes.get(i) + " en " + p);
                    for (int j = i + 1; j < nodes.size(); j++) {
                        MapLayout.Point q = layout.position(nodes.get(j));
                        double gap = Math.max(Math.abs(p.x() - q.x()), Math.abs(p.y() - q.y()));
                        assertTrue(gap >= (half + layout.size(nodes.get(j)) / 2.0), "seed " + seed
                                + " : " + nodes.get(i) + " et " + nodes.get(j) + " se chevauchent");
                    }
                }
            }
        }
    }

    @Test
    void unCheminEnPointillesParLienHorsDesIcones() {
        for (long seed = 0; seed < 50; seed++) {
            RunMap map = map(seed);
            MapLayout layout = MapLayout.of(map, seed, 518, 245);
            long links = map.nodes().stream().mapToLong(n -> map.choicesAfter(n).size()).sum();
            assertEquals(links, layout.paths().size());
            for (MapLayout.Path path : layout.paths()) {
                assertFalse(path.dots().isEmpty(), "seed " + seed + " : lien sans point " + path.from());
                for (MapLayout.Point dot : path.dots()) {
                    for (MapNode end : List.of(path.from(), path.to())) {
                        MapLayout.Point p = layout.position(end);
                        assertTrue(Math.hypot(dot.x() - p.x(), dot.y() - p.y()) >= layout.size(end) / 2.0,
                                "point sous une icône");
                    }
                }
                for (int i = 1; i < path.dots().size(); i++) {
                    MapLayout.Point a = path.dots().get(i - 1);
                    MapLayout.Point b = path.dots().get(i);
                    double gap = Math.hypot(a.x() - b.x(), a.y() - b.y());
                    assertTrue(gap >= MapLayout.DOT_STEP - 2 && gap <= MapLayout.DOT_STEP + 3, "pointillé régulier : " + gap);
                }
            }
        }
    }

    @Test
    void lesCheminsNeSontPasDroits() {
        RunMap map = map(42);
        MapLayout layout = MapLayout.of(map, 42, 658, 357);
        int curved = 0;
        for (MapLayout.Path path : layout.paths()) {
            MapLayout.Point a = path.dots().get(0);
            MapLayout.Point b = path.dots().get(path.dots().size() - 1);
            double length = Math.hypot(b.x() - a.x(), b.y() - a.y());
            for (MapLayout.Point d : path.dots()) {
                double off = Math.abs((b.x() - a.x()) * (a.y() - d.y()) - (a.x() - d.x()) * (b.y() - a.y())) / length;
                if (off > 2) {
                    curved++;
                    break;
                }
            }
        }
        assertTrue(curved > layout.paths().size() / 2, curved + " chemins courbes sur " + layout.paths().size());
    }
}

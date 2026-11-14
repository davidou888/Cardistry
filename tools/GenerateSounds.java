import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Génère les sons PLACEHOLDER de Cardistry (bips et boucles chiptune simples), à remplacer par les
 * vrais. Lancer depuis la racine du projet : {@code java tools/GenerateSounds.java}, puis
 * {@code tools/generate-sounds.sh} pour convertir les musiques en OGG.
 * Effets : src/main/resources/audio/sfx/*.wav (22,05 kHz mono 16 bits).
 * Musiques : build/placeholder-music/*.wav (22,05 kHz mono), à convertir en OGG.
 */
public class GenerateSounds {

    static final int RATE = 22_050;
    static final Random NOISE = new Random(42); // déterministe : les fichiers ne changent pas d'une fois sur l'autre

    // --- Synthèse ---

    interface Wave {
        double at(double phase); // phase en tours
    }

    static final Wave SQUARE = p -> (p % 1) < 0.5 ? 1 : -1;
    static final Wave PULSE = p -> (p % 1) < 0.25 ? 1 : -1;
    static final Wave TRIANGLE = p -> 4 * Math.abs((p % 1) - 0.5) - 1;
    static final Wave SINE = p -> Math.sin(2 * Math.PI * p);

    /** Note : fréquence qui glisse de f0 à f1, enveloppe attaque/déclin. */
    static double[] tone(Wave wave, double f0, double f1, double seconds, double volume, double vibrato) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = f0 + (f1 - f0) * t;
            f *= 1 + vibrato * Math.sin(2 * Math.PI * 7 * i / RATE);
            phase += f / RATE;
            double env = Math.min(1, i / (0.004 * RATE)) * Math.pow(1 - t, 1.6);
            out[i] = wave.at(phase) * env * volume;
        }
        return out;
    }

    static double[] noise(double seconds, double volume, double decay) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        double last = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            last = last * 0.6 + (NOISE.nextDouble() * 2 - 1) * 0.4; // bruit un peu étouffé
            out[i] = last * volume * Math.pow(1 - t, decay) * Math.min(1, i / (0.002 * RATE));
        }
        return out;
    }

    static double[] silence(double seconds) {
        return new double[(int) (seconds * RATE)];
    }

    static double[] seq(double[]... parts) {
        int n = 0;
        for (double[] p : parts) {
            n += p.length;
        }
        double[] out = new double[n];
        int at = 0;
        for (double[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    static double[] mix(double[]... parts) {
        int n = 0;
        for (double[] p : parts) {
            n = Math.max(n, p.length);
        }
        double[] out = new double[n];
        for (double[] p : parts) {
            for (int i = 0; i < p.length; i++) {
                out[i] += p[i];
            }
        }
        return out;
    }

    static double midi(int note) {
        return 440 * Math.pow(2, (note - 69) / 12.0);
    }

    static double[] arp(Wave wave, double noteSeconds, double volume, int... notes) {
        List<double[]> parts = new ArrayList<>();
        for (int note : notes) {
            parts.add(tone(wave, midi(note), midi(note), noteSeconds, volume, 0));
        }
        return seq(parts.toArray(double[][]::new));
    }

    // --- Effets ---

    static void effects(Path dir) throws IOException {
        Files.createDirectories(dir);
        write(dir, "ui-cursor", tone(SQUARE, 1300, 1300, 0.025, 0.25, 0));
        write(dir, "ui-confirm", seq(tone(SQUARE, 660, 660, 0.04, 0.35, 0), tone(SQUARE, 990, 990, 0.07, 0.35, 0)));
        write(dir, "ui-back", seq(tone(SQUARE, 660, 660, 0.04, 0.3, 0), tone(SQUARE, 440, 440, 0.07, 0.3, 0)));
        write(dir, "map-node", arp(PULSE, 0.05, 0.35, 72, 76, 79));
        write(dir, "card-fire", mix(noise(0.25, 0.6, 1.2), tone(SQUARE, 500, 120, 0.25, 0.3, 0)));
        write(dir, "card-water", mix(tone(SINE, 300, 900, 0.25, 0.6, 0.05), tone(SINE, 600, 1400, 0.18, 0.25, 0)));
        write(dir, "card-plant", tone(TRIANGLE, 330, 550, 0.28, 0.7, 0.08));
        write(dir, "card-heal", arp(TRIANGLE, 0.07, 0.6, 72, 76, 79, 84));
        write(dir, "card-terrain", mix(noise(0.4, 0.4, 0.8), tone(TRIANGLE, 110, 70, 0.4, 0.5, 0)));
        write(dir, "hit-enemy", mix(noise(0.12, 0.7, 2), tone(SQUARE, 180, 90, 0.12, 0.35, 0)));
        write(dir, "hit-player", mix(noise(0.18, 0.6, 1.5), tone(TRIANGLE, 120, 50, 0.2, 0.7, 0)));
        write(dir, "dodge", mix(noise(0.15, 0.3, 1), tone(SINE, 400, 1600, 0.15, 0.4, 0)));
        write(dir, "enemy-death", arp(SQUARE, 0.08, 0.35, 67, 63, 60, 55, 48));
        write(dir, "null", glitch(0.35));
        write(dir, "shop-buy", seq(tone(PULSE, midi(88), midi(88), 0.05, 0.35, 0), tone(PULSE, midi(93), midi(93), 0.12, 0.35, 0)));
        write(dir, "shop-reroll", seq(noise(0.04, 0.4, 1), silence(0.03), noise(0.04, 0.4, 1), silence(0.03), noise(0.05, 0.4, 1)));
        write(dir, "shop-poor", tone(SQUARE, 110, 100, 0.22, 0.35, 0.02));
        write(dir, "rejected", tone(SQUARE, 200, 180, 0.08, 0.3, 0));
        write(dir, "smoke", noise(0.45, 0.25, 0.6));
        write(dir, "smoke-googz", mix(noise(0.45, 0.2, 0.6), seq(silence(0.15), arp(TRIANGLE, 0.06, 0.4, 84, 88, 91))));
        write(dir, "roll", seq(noise(0.05, 0.3, 1), silence(0.04), noise(0.05, 0.3, 1), silence(0.04), noise(0.08, 0.3, 1)));
        write(dir, "drink", seq(tone(SINE, 220, 160, 0.09, 0.6, 0), tone(SINE, 240, 170, 0.09, 0.6, 0), tone(SINE, 260, 180, 0.12, 0.6, 0)));
        write(dir, "item", tone(PULSE, 880, 1100, 0.08, 0.3, 0));
        write(dir, "hangover", tone(TRIANGLE, 300, 150, 0.6, 0.6, 0.1));
        write(dir, "event-choice", arp(TRIANGLE, 0.08, 0.5, 76, 83));
        write(dir, "secret", arp(PULSE, 0.06, 0.35, 72, 79, 84, 88, 91, 96));
        write(dir, "badoing", seq(boing(0.22), boing(0.22)));
        write(dir, "choomah-1", tone(SQUARE, 700, 1000, 0.25, 0.3, 0.15));
        write(dir, "choomah-2", tone(SQUARE, 900, 600, 0.3, 0.3, 0.2));
        write(dir, "choomah-3", seq(tone(SQUARE, 800, 1100, 0.12, 0.3, 0.1), tone(SQUARE, 1100, 700, 0.18, 0.3, 0.2)));
        write(dir, "heal", tone(SINE, 500, 800, 0.2, 0.5, 0));
        write(dir, "draw", noise(0.06, 0.35, 1.5));
        write(dir, "deck-add", arp(TRIANGLE, 0.06, 0.5, 79, 84));
        write(dir, "choomies", tone(PULSE, midi(91), midi(91), 0.06, 0.3, 0));
        write(dir, "reward", arp(PULSE, 0.08, 0.35, 72, 76, 79, 84));
        write(dir, "level", arp(SQUARE, 0.12, 0.3, 60, 64, 67, 72, 72));
        write(dir, "undo", tone(SINE, 1400, 300, 0.3, 0.5, 0));
        write(dir, "rest", arp(TRIANGLE, 0.2, 0.5, 72, 69, 65));
    }

    static double[] boing(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = 180 + 220 * Math.sin(Math.PI * t);
            phase += f / RATE;
            out[i] = SINE.at(phase) * 0.7 * Math.pow(1 - t, 0.8);
        }
        return out;
    }

    static double[] glitch(double seconds) {
        List<double[]> parts = new ArrayList<>();
        for (double t = 0; t < seconds; t += 0.03) {
            parts.add(tone(SQUARE, 200 + NOISE.nextInt(1800), 200 + NOISE.nextInt(1800), 0.03, 0.3, 0));
        }
        return seq(parts.toArray(double[][]::new));
    }

    // --- Musiques (boucles de 4 mesures) ---

    /** Boucle : basse (une note par temps), arpège (8 par mesure), et charleston de bruit. */
    static double[] loop(int bpm, int[] chords, boolean minor, Wave lead, double drums) {
        double beat = 60.0 / bpm;
        List<double[]> bars = new ArrayList<>();
        for (int root : chords) {
            int third = root + (minor ? 3 : 4);
            int fifth = root + 7;
            double[] bass = seq(tone(TRIANGLE, midi(root - 12), midi(root - 12), beat, 0.5, 0),
                    tone(TRIANGLE, midi(root - 12), midi(root - 12), beat, 0.45, 0),
                    tone(TRIANGLE, midi(fifth - 12), midi(fifth - 12), beat, 0.45, 0),
                    tone(TRIANGLE, midi(root - 12), midi(root - 12), beat, 0.45, 0));
            int[] pattern = {root + 12, third + 12, fifth + 12, third + 12, root + 24, fifth + 12, third + 12, fifth + 12};
            List<double[]> notes = new ArrayList<>();
            List<double[]> hats = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                notes.add(tone(lead, midi(pattern[i]), midi(pattern[i]), beat / 2, 0.16, 0.004));
                hats.add(drums > 0 ? seq(noise(0.03, drums * (i % 2 == 0 ? 1 : 0.5), 3), silence(beat / 2 - 0.03))
                        : silence(beat / 2));
            }
            bars.add(mix(bass, seq(notes.toArray(double[][]::new)), seq(hats.toArray(double[][]::new))));
        }
        return seq(bars.toArray(double[][]::new));
    }

    static void music(Path dir) throws IOException {
        Files.createDirectories(dir);
        write(dir, "title", loop(96, new int[] {57, 53, 60, 55}, true, TRIANGLE, 0));
        write(dir, "map", loop(104, new int[] {60, 65, 57, 67}, false, PULSE, 0.08));
        write(dir, "combat", loop(140, new int[] {57, 57, 53, 55}, true, SQUARE, 0.15));
        write(dir, "elite", loop(150, new int[] {52, 53, 52, 55}, true, SQUARE, 0.2));
        write(dir, "boss", loop(160, new int[] {50, 51, 50, 46}, true, SQUARE, 0.25));
        write(dir, "shop", loop(112, new int[] {60, 57, 62, 55}, false, PULSE, 0.06));
        write(dir, "rest", loop(72, new int[] {60, 57, 53, 55}, false, SINE, 0));
        write(dir, "event", loop(90, new int[] {62, 58, 60, 57}, true, TRIANGLE, 0.05));
        write(dir, "game-over", seq(arp(TRIANGLE, 0.35, 0.6, 67, 63, 60, 55), silence(0.6)));
        write(dir, "victory", seq(arp(PULSE, 0.15, 0.4, 60, 64, 67, 72, 67, 72, 76, 79), silence(0.5)));
    }

    // --- WAV ---

    static void write(Path dir, String name, double[] samples) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        double peak = 0;
        for (double s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        double scale = peak > 0.89 ? 0.89 / peak : 1; // crête sous −1 dBFS
        for (double s : samples) {
            short v = (short) Math.round(Math.max(-1, Math.min(1, s * scale)) * 32767);
            data.write(v & 0xFF);
            data.write((v >> 8) & 0xFF);
        }
        byte[] pcm = data.toByteArray();
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + pcm.length).put("WAVE".getBytes());
        header.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(RATE)
                .putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
        header.put("data".getBytes()).putInt(pcm.length);
        byte[] out = new byte[44 + pcm.length];
        System.arraycopy(header.array(), 0, out, 0, 44);
        System.arraycopy(pcm, 0, out, 44, pcm.length);
        Files.write(dir.resolve(name + ".wav"), out);
    }

    public static void main(String[] args) throws IOException {
        effects(Path.of("src/main/resources/audio/sfx"));
        music(Path.of("build/placeholder-music"));
        System.out.println("Effets : src/main/resources/audio/sfx ; musiques : build/placeholder-music (à convertir en OGG)");
    }
}

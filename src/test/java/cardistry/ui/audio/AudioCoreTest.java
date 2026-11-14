package cardistry.ui.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Table, décodage, volumes et mixeur : jamais de son joué. */
class AudioCoreTest {

    private static final double DELTA = 1e-4;

    // --- Table ---

    @Test
    void lectureDeLaTable() {
        SoundTable table = SoundTable.parse("""
                # commentaire
                music.combat = music/combat.ogg ; loop=4.5 ; volume=0.8
                music.victory = music/victory.ogg ; loop=no
                sfx.a = sfx/a1.wav, sfx/a2.wav ; volume=0.3
                sfx.vide =
                ligne sans égal
                sfx.mauvais = x.wav ; volume=beaucoup
                """);
        SoundTable.Entry combat = table.get("music.combat").orElseThrow();
        assertEquals(List.of("music/combat.ogg"), combat.files());
        assertEquals(4.5, combat.loopStart(), DELTA);
        assertEquals(0.8, combat.volume(), DELTA);
        assertTrue(combat.loops());
        assertFalse(table.get("music.victory").orElseThrow().loops());
        assertEquals(List.of("sfx/a1.wav", "sfx/a2.wav"), table.get("sfx.a").orElseThrow().files());
        assertTrue(table.get("sfx.vide").isEmpty());
        assertTrue(table.get("sfx.mauvais").isEmpty(), "ligne illisible ignorée");
        assertEquals(3, table.entries().size());
    }

    @Test
    void chaqueFichierDeLaTableExisteEtSeDecode() {
        SoundTable table = SoundTable.load();
        assertTrue(table.entries().size() > 40);
        for (SoundTable.Entry entry : table.entries().values()) {
            for (String file : entry.files()) {
                assertTrue(Pcm.load(file).isPresent(), "illisible : " + file);
            }
        }
    }

    /** Tous les sons demandés par le jeu ont une entrée dans la table. */
    @Test
    void tousLesSonsDuJeuSontDansLaTable() throws IOException {
        SoundTable table = SoundTable.load();
        Set<String> used = new TreeSet<>();
        Matcher m = Pattern.compile("\"(sfx\\.[a-z.\\-]+)\"").matcher(
                Files.readString(Path.of("src/main/java/cardistry/ui/audio/AudioDirector.java")));
        while (m.find()) {
            used.add(m.group(1));
        }
        for (UiSound sound : UiSound.values()) {
            used.add(sound.tableName());
        }
        for (MusicContext context : MusicContext.values()) {
            used.add(context.tableName());
        }
        assertTrue(used.size() > 40);
        for (String name : used) {
            assertTrue(table.get(name).isPresent(), "absent de sounds.txt : " + name);
        }
    }

    @Test
    void sonsFrequentsDiscrets() {
        SoundTable table = SoundTable.load();
        for (String name : List.of("sfx.ui.cursor", "sfx.draw", "sfx.choomies")) {
            assertTrue(table.get(name).orElseThrow().volume() <= 0.5, name);
        }
    }

    // --- Normalisation ---

    private static Pcm tone(double amplitude, double seconds, double silenceAfter) {
        int frames = (int) (Pcm.RATE * (seconds + silenceAfter));
        float[] samples = new float[frames * 2];
        for (int f = 0; f < (int) (Pcm.RATE * seconds); f++) {
            float v = (float) (Math.sin(f * 0.2) * amplitude);
            samples[f * 2] = v;
            samples[f * 2 + 1] = v;
        }
        return new Pcm(samples);
    }

    @Test
    void sonFaibleEtSonFortRamenesAuMemeNiveau() {
        Pcm quiet = tone(0.02, 0.5, 0).normalized();
        Pcm loud = tone(0.9, 0.5, 0).normalized();
        assertEquals(Pcm.TARGET_DB, quiet.loudnessDb(), 0.1);
        assertEquals(Pcm.TARGET_DB, loud.loudnessDb(), 0.1);
    }

    @Test
    void leSilenceAutourDuSonNeCompteePas() {
        assertEquals(tone(0.5, 0.2, 0).loudnessDb(), tone(0.5, 0.2, 3).loudnessDb(), 0.5);
    }

    @Test
    void laNormalisationNeSatureJamais() {
        // Un clic très bref : son niveau moyen est bas, mais sa crête est déjà au maximum.
        float[] samples = new float[Pcm.RATE * 2];
        samples[0] = 1f;
        samples[1] = 1f;
        Pcm click = new Pcm(samples).normalized();
        assertTrue(click.peak() <= Pcm.PEAK_LIMIT + 1e-6);
    }

    @Test
    void fichierSilencieuxLaisseTelQuel() {
        Pcm silent = new Pcm(new float[Pcm.RATE * 2]);
        assertTrue(silent.normalized() == silent);
        Pcm almost = tone(0.00002, 0.5, 0); // −94 dB : du bruit, pas un son
        assertTrue(almost.normalized() == almost);
    }

    @Test
    void tousLesSonsDuJeuSontAuMemeNiveauOuLimitesParLeurCrete() {
        SoundTable table = SoundTable.load();
        for (SoundTable.Entry entry : table.entries().values()) {
            for (String file : entry.files()) {
                Pcm pcm = Pcm.load(file).orElseThrow(() -> new AssertionError(file));
                if (pcm.peak() < 0.001) {
                    continue; // fichier silencieux (signalé ailleurs)
                }
                assertTrue(pcm.peak() <= Pcm.PEAK_LIMIT + 1e-6, file);
                boolean atTarget = Math.abs(pcm.loudnessDb() - Pcm.TARGET_DB) < 0.5;
                boolean peakLimited = pcm.peak() > Pcm.PEAK_LIMIT - 0.01 && pcm.loudnessDb() < Pcm.TARGET_DB;
                assertTrue(atTarget || peakLimited, file + " : " + pcm.loudnessDb() + " dB");
            }
        }
    }

    // --- Décodage ---

    @Test
    void wavMono22kEnStereo44k() throws Exception {
        // WAV fabriqué ici (22,05 kHz mono, 25 ms) : le test ne dépend pas des sons du jeu.
        int frames = 22_050 * 25 / 1000;
        byte[] data = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            short sample = (short) (Math.sin(i * 0.3) * 12_000);
            data[i * 2] = (byte) sample;
            data[i * 2 + 1] = (byte) (sample >> 8);
        }
        java.io.ByteArrayOutputStream wav = new java.io.ByteArrayOutputStream();
        javax.sound.sampled.AudioSystem.write(new javax.sound.sampled.AudioInputStream(
                new java.io.ByteArrayInputStream(data), new javax.sound.sampled.AudioFormat(22_050, 16, 1, true, false),
                frames), javax.sound.sampled.AudioFileFormat.Type.WAVE, wav);
        Pcm pcm = Pcm.decode(new java.io.ByteArrayInputStream(wav.toByteArray()));
        assertEquals(Pcm.RATE * 25 / 1000, pcm.frames(), 2);
        float[] s = pcm.samples();
        for (int f = 0; f < pcm.frames(); f++) {
            assertEquals(s[f * 2], s[f * 2 + 1], DELTA, "mono dupliqué sur les deux canaux");
        }
    }

    @Test
    void oggDecode() {
        Pcm pcm = Pcm.load("music/combat.ogg").orElseThrow();
        assertTrue(pcm.frames() > Pcm.RATE * 5, "plusieurs secondes de musique");
        float peak = 0;
        for (float v : pcm.samples()) {
            peak = Math.max(peak, Math.abs(v));
        }
        assertTrue(peak > 0.1 && peak <= 1.0, "signal décodé : " + peak);
    }

    @Test
    void fichierAbsentOuIllisible() {
        assertTrue(Pcm.load("sfx/absent.wav").isEmpty());
        assertTrue(Pcm.load("sounds.txt").isEmpty(), "pas un son");
    }

    @Test
    void reechantillonnageLineaire() {
        float[] stereo = {0, 0, 1, 1};
        float[] out = Pcm.resample(stereo, 22_050);
        assertEquals(8, out.length);
        assertEquals(0.5, out[2], DELTA);
        assertEquals(stereo, Pcm.resample(stereo, Pcm.RATE));
    }

    // --- Volumes ---

    @Test
    void echellePerceptive() {
        assertEquals(1.0, Volume.gain(10), DELTA);
        assertEquals(0.0, Volume.gain(0), DELTA);
        assertEquals(Math.pow(10, -10 / 20.0), Volume.gain(5), DELTA); // −10 dB
        assertEquals(Volume.gain(10), Volume.gain(42), DELTA);
        assertEquals(6, Volume.DEFAULT_MUSIC);
        assertEquals(8, Volume.DEFAULT_EFFECTS);
    }

    // --- Mixeur ---

    private static Pcm constant(float value, int frames) {
        float[] s = new float[frames * 2];
        java.util.Arrays.fill(s, value);
        return new Pcm(s);
    }

    @Test
    void effetsAdditionnesAvecLeursVolumes() {
        Mixer mixer = new Mixer();
        mixer.setEffectsGain(0.5f);
        mixer.play(constant(0.4f, 100), 1f);
        mixer.play(constant(0.2f, 50), 0.5f);
        float[] out = new float[200 * 2];
        mixer.mix(out, 200);
        assertEquals(0.4 * 0.5 + 0.2 * 0.5 * 0.5, out[0], DELTA);
        assertEquals(0.4 * 0.5, out[60 * 2], DELTA, "le second est fini");
        assertEquals(0, out[150 * 2], DELTA);
        assertFalse(mixer.isPlaying());
    }

    @Test
    void ecretage() {
        Mixer mixer = new Mixer();
        mixer.setEffectsGain(1f);
        for (int i = 0; i < 5; i++) {
            mixer.play(constant(0.5f, 10), 1f);
        }
        float[] out = new float[20];
        mixer.mix(out, 10);
        assertEquals(1.0, out[0], DELTA);
    }

    @Test
    void seizeEffetsAuPlus() {
        Mixer mixer = new Mixer();
        mixer.setEffectsGain(1f);
        for (int i = 0; i < Mixer.MAX_EFFECTS + 4; i++) {
            mixer.play(constant(0.01f, 10), 1f);
        }
        float[] out = new float[20];
        mixer.mix(out, 10);
        assertEquals(0.01 * Mixer.MAX_EFFECTS, out[0], DELTA);
    }

    @Test
    void musiqueEnBoucleAuPointDeBoucle() {
        Mixer mixer = new Mixer();
        mixer.setMusicGain(1f);
        float[] s = new float[10 * 2];
        for (int f = 0; f < 10; f++) {
            s[f * 2] = f / 10f;
            s[f * 2 + 1] = f / 10f;
        }
        mixer.music(new Pcm(s), 1f, 4.0 / Pcm.RATE, true); // boucle à l'image 4
        float[] out = new float[14 * 2];
        mixer.mix(out, 14);
        assertEquals(0.9, out[9 * 2], DELTA);
        assertEquals(0.4, out[10 * 2], DELTA, "reprend au point de boucle");
        assertEquals(0.7, out[13 * 2], DELTA);
    }

    @Test
    void jingleJoueUneFois() {
        Mixer mixer = new Mixer();
        mixer.setMusicGain(1f);
        mixer.music(constant(0.5f, 10), 1f, 0, false);
        float[] out = new float[20 * 2];
        mixer.mix(out, 20);
        assertEquals(0.5, out[9 * 2], DELTA);
        assertEquals(0, out[12 * 2], DELTA);
        assertFalse(mixer.isPlaying());
    }

    @Test
    void fonduEnchaine() {
        Mixer mixer = new Mixer();
        mixer.setMusicGain(1f);
        int frames = (int) (Mixer.CROSSFADE_SECONDS * Pcm.RATE);
        mixer.music(constant(1f, frames * 3), 1f, 0, true);
        float[] out = new float[frames * 2];
        mixer.mix(out, 1);
        assertEquals(1.0, out[0], DELTA, "la première piste démarre sans fondu");
        mixer.music(constant(0.5f, frames * 3), 1f, 0, true);
        mixer.mix(out, frames / 2);
        // À mi-fondu : 1 × 0.5 (sortante) + 0.5 × 0.5 (entrante)
        assertEquals(0.75, out[(frames / 2 - 1) * 2], 0.01);
        mixer.mix(out, frames);
        assertEquals(0.5, out[(frames - 1) * 2], 0.01, "seule la nouvelle reste");
    }

    @Test
    void fonduVersLeSilenceEtVolumeMusique() {
        Mixer mixer = new Mixer();
        mixer.setMusicGain(Volume.gain(5));
        mixer.music(constant(1f, Pcm.RATE * 4), 1f, 0, true);
        float[] out = new float[4];
        mixer.mix(out, 2);
        assertEquals(Volume.gain(5), out[0], DELTA);
        mixer.music(null, 0, 0, false);
        float[] all = new float[Pcm.RATE * 4 * 2];
        mixer.mix(all, Pcm.RATE * 2);
        assertFalse(mixer.isPlaying(), "silence après le fondu");
    }

    @Test
    void sansSortieAudioToutEstSilencieux() {
        Audio audio = Audio.open(6, 8, false, false, false); // cardistry.audio=off pendant les tests
        audio.effect("sfx.ui.cursor");
        audio.music(MusicContext.COMBAT);
        audio.setLevels(10, 10, true);
        audio.close();
        assertEquals("off", System.getProperty("cardistry.audio"));
    }
}

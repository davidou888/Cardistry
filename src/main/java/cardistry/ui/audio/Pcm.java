package cardistry.ui.audio;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Son décodé en mémoire : échantillons flottants stéréo entrelacés (gauche, droite…) à
 * {@link #RATE} Hz, quel que soit le fichier d'origine (WAV 8/16 bits, mono ou stéréo, toute
 * fréquence ; OGG Vorbis grâce à VorbisSPI).
 */
public final class Pcm {

    public static final int RATE = 44_100;

    private final float[] samples;

    public Pcm(float[] samples) {
        this.samples = samples;
    }

    /** Échantillons entrelacés gauche/droite. */
    public float[] samples() {
        return samples;
    }

    /** Nombre d'images (une image = un échantillon gauche + un droit). */
    public int frames() {
        return samples.length / 2;
    }

    /** Son du fichier {@code /audio/<path>} ; vide s'il manque ou est illisible. */
    public static Optional<Pcm> load(String path) {
        try (InputStream raw = Pcm.class.getResourceAsStream("/audio/" + path)) {
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(decode(new BufferedInputStream(raw)));
        } catch (IOException | UnsupportedAudioFileException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Décode un flux audio (WAV, OGG…) en PCM stéréo 44,1 kHz. */
    static Pcm decode(InputStream in) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(in)) {
            AudioFormat base = source.getFormat();
            AudioInputStream pcm = source;
            if (base.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                    && base.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
                // OGG Vorbis (VorbisSPI) : décodage en PCM 16 bits à la fréquence d'origine.
                AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.getSampleRate(), 16,
                        base.getChannels(), base.getChannels() * 2, base.getSampleRate(), false);
                pcm = AudioSystem.getAudioInputStream(decoded, source);
            }
            AudioFormat format = pcm.getFormat();
            byte[] bytes = readAll(pcm);
            int channels = format.getChannels();
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int frames = bytes.length / (bytesPerSample * channels);
            float[] stereo = new float[frames * 2];
            for (int f = 0; f < frames; f++) {
                for (int c = 0; c < 2; c++) {
                    int channel = Math.min(c, channels - 1);
                    int offset = (f * channels + channel) * bytesPerSample;
                    stereo[f * 2 + c] = sample(bytes, offset, format);
                }
            }
            return new Pcm(resample(stereo, format.getSampleRate()));
        }
    }

    private static byte[] readAll(AudioInputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        // Le décodeur OGG renvoie parfois 0 octet avant la fin : on lit jusqu'à -1.
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /** Un échantillon dans [-1, 1]. */
    private static float sample(byte[] bytes, int offset, AudioFormat format) {
        int bits = format.getSampleSizeInBits();
        if (bits == 8) {
            int value = bytes[offset];
            if (format.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
                value = (value & 0xFF) - 128;
            }
            return value / 128f;
        }
        int lo = format.isBigEndian() ? bytes[offset + 1] : bytes[offset];
        int hi = format.isBigEndian() ? bytes[offset] : bytes[offset + 1];
        int value = (hi << 8) | (lo & 0xFF);
        if (format.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            value = (value & 0xFFFF) - 32768;
        }
        return value / 32768f;
    }

    /** Rééchantillonnage linéaire vers {@link #RATE} (rien à faire si la fréquence y est déjà). */
    static float[] resample(float[] stereo, float rate) {
        if (Math.abs(rate - RATE) < 1) {
            return stereo;
        }
        int frames = stereo.length / 2;
        int outFrames = (int) Math.round(frames * (double) RATE / rate);
        float[] out = new float[outFrames * 2];
        double step = rate / (double) RATE;
        for (int f = 0; f < outFrames; f++) {
            double pos = f * step;
            int i = (int) pos;
            double t = pos - i;
            int j = Math.min(i + 1, frames - 1);
            for (int c = 0; c < 2; c++) {
                float a = stereo[Math.min(i, frames - 1) * 2 + c];
                float b = stereo[j * 2 + c];
                out[f * 2 + c] = (float) (a + (b - a) * t);
            }
        }
        return out;
    }
}

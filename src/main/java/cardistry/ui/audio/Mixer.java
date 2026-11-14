package cardistry.ui.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixeur logiciel, sans aucune sortie audio (testable) : additionne les effets en cours et la
 * musique dans un tampon stéréo flottant. Deux canaux :
 * <ul>
 *   <li>musique : une piste à la fois, en boucle à partir de son point de boucle (ou une seule fois),
 *       fondu enchaîné de {@link #CROSSFADE_SECONDS} s au changement ;</li>
 *   <li>effets : jusqu'à {@link #MAX_EFFECTS} sons simultanés (le plus ancien est coupé au-delà).</li>
 * </ul>
 * Toutes les méthodes sont synchronisées : le fil audio mixe pendant que le jeu commande.
 */
public final class Mixer {

    public static final int MAX_EFFECTS = 16;
    public static final double CROSSFADE_SECONDS = 1.5;

    private static class Voice {
        final Pcm pcm;
        final float volume;
        int position;

        Voice(Pcm pcm, float volume) {
            this.pcm = pcm;
            this.volume = volume;
        }
    }

    private static final class Track extends Voice {
        final int loopStart;
        final boolean loops;
        float fade;
        float fadeStep;

        Track(Pcm pcm, float volume, int loopStart, boolean loops, float fade, float fadeStep) {
            super(pcm, volume);
            this.loopStart = Math.min(Math.max(0, loopStart), Math.max(0, pcm.frames() - 1));
            this.loops = loops;
            this.fade = fade;
            this.fadeStep = fadeStep;
        }
    }

    private final List<Voice> effects = new ArrayList<>();
    private final List<Track> tracks = new ArrayList<>();
    private float musicGain = Volume.gain(Volume.DEFAULT_MUSIC);
    private float effectsGain = Volume.gain(Volume.DEFAULT_EFFECTS);

    public synchronized void setMusicGain(float gain) {
        musicGain = gain;
    }

    public synchronized void setEffectsGain(float gain) {
        effectsGain = gain;
    }

    /** Joue un effet ({@code volume} : celui de la table). */
    public synchronized void play(Pcm pcm, float volume) {
        if (effects.size() >= MAX_EFFECTS) {
            effects.remove(0);
        }
        effects.add(new Voice(pcm, volume));
    }

    /**
     * Change de musique : la piste en cours s'éteint en fondu pendant que la nouvelle monte.
     * {@code pcm == null} : fondu vers le silence.
     */
    public synchronized void music(Pcm pcm, float volume, double loopStartSeconds, boolean loops) {
        float step = (float) (1.0 / (CROSSFADE_SECONDS * Pcm.RATE));
        for (Track track : tracks) {
            track.fadeStep = -step;
        }
        if (pcm != null) {
            tracks.add(new Track(pcm, volume, (int) Math.round(loopStartSeconds * Pcm.RATE), loops,
                    tracks.isEmpty() ? 1f : 0f, tracks.isEmpty() ? 0f : step));
        }
    }

    /** {@code true} s'il reste quelque chose à jouer. */
    public synchronized boolean isPlaying() {
        return !effects.isEmpty() || !tracks.isEmpty();
    }

    /**
     * Remplit {@code out} ({@code frames} images stéréo entrelacées) avec le mélange, borné à [-1, 1].
     */
    public synchronized void mix(float[] out, int frames) {
        java.util.Arrays.fill(out, 0, frames * 2, 0f);
        for (int i = tracks.size() - 1; i >= 0; i--) {
            if (mixTrack(tracks.get(i), out, frames)) {
                tracks.remove(i);
            }
        }
        for (int i = effects.size() - 1; i >= 0; i--) {
            Voice voice = effects.get(i);
            float gain = voice.volume * effectsGain;
            float[] s = voice.pcm.samples();
            int n = Math.min(frames, voice.pcm.frames() - voice.position);
            for (int f = 0; f < n; f++) {
                int src = (voice.position + f) * 2;
                out[f * 2] += s[src] * gain;
                out[f * 2 + 1] += s[src + 1] * gain;
            }
            voice.position += n;
            if (voice.position >= voice.pcm.frames()) {
                effects.remove(i);
            }
        }
        for (int i = 0; i < frames * 2; i++) {
            out[i] = Math.max(-1f, Math.min(1f, out[i]));
        }
    }

    /** @return {@code true} si la piste est finie (fondu terminé, ou jouée une fois jusqu'au bout) */
    private boolean mixTrack(Track track, float[] out, int frames) {
        float[] s = track.pcm.samples();
        int total = track.pcm.frames();
        if (total == 0) {
            return true;
        }
        for (int f = 0; f < frames; f++) {
            if (track.position >= total) {
                if (!track.loops) {
                    return true;
                }
                track.position = track.loopStart;
            }
            track.fade = Math.max(0f, Math.min(1f, track.fade + track.fadeStep));
            float gain = track.volume * musicGain * track.fade;
            int src = track.position * 2;
            out[f * 2] += s[src] * gain;
            out[f * 2 + 1] += s[src + 1] * gain;
            track.position++;
        }
        return track.fadeStep < 0 && track.fade <= 0f;
    }
}

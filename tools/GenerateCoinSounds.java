import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.Random;

/** Original modal synthesis: overlapping metallic coin impacts, no sampled third-party audio. */
public class GenerateCoinSounds {
    static final int RATE = 44100;
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]); Files.createDirectories(output);
        write(output, "coins_jaderald", 7, 0.9, 1527);
        write(output, "coins_claw", 12, 1.25, 6278);
        write(output, "coins_alloy", 21, 1.65, 91723);
    }
    static void write(Path output, String name, int impacts, double duration, int seed) throws Exception {
        double[] mix = new double[(int) (RATE * duration)];
        Random random = new Random(seed);
        double[] modes = {1, 1.47, 2.09, 2.73, 3.71};
        for (int hit = 0; hit < impacts; hit++) {
            double start = hit == 0 ? 0.008 : (hit - 0.5 + random.nextDouble() * 0.45) / impacts * (duration - 0.38);
            double base = 1350 + random.nextDouble() * 650;
            double amplitude = 0.6 + random.nextDouble() * 0.4;
            double[] phase = random.doubles(modes.length, 0, Math.PI * 2).toArray();
            int offset = (int) (start * RATE);
            for (int i = 0; i < RATE * 0.45 && i + offset < mix.length; i++) {
                double t = i / (double) RATE;
                double sample = 0;
                for (int mode = 0; mode < modes.length; mode++) {
                    sample += Math.sin(2 * Math.PI * base * modes[mode] * t + phase[mode])
                        * Math.exp(-t * (17 + mode * 8)) / (1 + mode * 0.7);
                }
                sample += (random.nextDouble() * 2 - 1) * 0.4 * Math.exp(-t * 350);
                double attack = Math.min(1, t / 0.0008);
                mix[i + offset] += sample * attack * amplitude;
            }
        }
        double peak = 0;
        for (double sample : mix) peak = Math.max(peak, Math.abs(sample));
        byte[] pcm = new byte[mix.length * 2];
        for (int i = 0; i < mix.length; i++) {
            double fade = Math.min(1, (mix.length - 1 - i) / (RATE * 0.035));
            short value = (short) Math.round(mix[i] / peak * 0.72 * 32767 * fade);
            pcm[2 * i] = (byte) value; pcm[2 * i + 1] = (byte) (value >> 8);
        }
        try (var audio = new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(RATE, 16, 1, true, false), mix.length)) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, output.resolve(name + ".wav").toFile());
        }
        System.out.println(name + ": " + duration + " seconds, " + impacts + " coin impacts");
    }
}

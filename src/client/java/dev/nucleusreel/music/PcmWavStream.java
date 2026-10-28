package dev.nucleusreel.music;

import net.minecraft.client.sounds.AudioStream;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;

/** Finite PCM stream shared by user music and the bundled coin recordings. */
public final class PcmWavStream implements AudioStream {
    private final AudioInputStream input;
    private final AudioFormat format;
    private boolean closed;

    public PcmWavStream(InputStream source) throws IOException {
        BufferedInputStream buffered = new BufferedInputStream(source);
        AudioInputStream original;
        try { original = AudioSystem.getAudioInputStream(buffered); }
        catch (Exception exception) { buffered.close(); throw new IOException("Cannot decode WAV.", exception); }
        AudioFormat originalFormat = original.getFormat();
        if (originalFormat.getChannels() < 1 || originalFormat.getChannels() > 2) {
            original.close(); throw new IOException("Use a mono or stereo track.");
        }
        format = new AudioFormat(originalFormat.getSampleRate(), 16, originalFormat.getChannels(), true, false);
        try { input = AudioSystem.getAudioInputStream(format, original); }
        catch (IllegalArgumentException exception) { original.close(); throw new IOException("Use a PCM WAV file.", exception); }
    }
    @Override public AudioFormat getFormat() { return format; }
    @Override public synchronized ByteBuffer read(int size) throws IOException {
        if (closed || size <= 0) return ByteBuffer.allocateDirect(0);
        int frame = format.getFrameSize();
        int length = Math.max(frame, Math.min(size, 1024 * 1024) / frame * frame);
        byte[] bytes = new byte[length];
        int count = input.read(bytes);
        ByteBuffer result = ByteBuffer.allocateDirect(Math.max(0, count));
        if (count > 0) result.put(bytes, 0, count);
        return result.flip();
    }
    @Override public synchronized void close() throws IOException {
        if (!closed) { closed = true; input.close(); }
    }
}

package dev.nucleusreel.music;

import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Locale;

/** A bounded-memory stream. Short tracks repeat by reopening the file. */
public final class LocalTrackStream implements AudioStream {
    private final Path path;
    private final AudioFormat format;
    private AudioStream current;
    private boolean closed;

    public static Path resolve(Path folder, String name) throws IOException {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(":"))
            throw new IOException("Use a filename inside the music folder.");
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".ogg") && !lower.endsWith(".wav"))
            throw new IOException("Supported files: OGG Vorbis or PCM WAV.");
        Path root = folder.toRealPath();
        Path file = root.resolve(name).normalize().toRealPath();
        if (!file.getParent().equals(root) || !Files.isRegularFile(file))
            throw new IOException("Track must be inside the music folder.");
        if (Files.size(file) > 200L * 1024 * 1024) throw new IOException("Track is larger than 200 MB.");
        return file;
    }

    public LocalTrackStream(Path path) throws IOException {
        this.path = path;
        current = open();
        format = current.getFormat();
        if (format.getChannels() < 1 || format.getChannels() > 2) {
            current.close();
            throw new IOException("Use a mono or stereo track.");
        }
    }

    private AudioStream open() throws IOException {
        if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            var input = Files.newInputStream(path);
            try { return new JOrbisAudioStream(input); }
            catch (Exception exception) { input.close(); throw new IOException("Cannot decode OGG Vorbis.", exception); }
        }
        return new PcmWavStream(Files.newInputStream(path));
    }

    @Override public AudioFormat getFormat() { return format; }

    @Override public synchronized ByteBuffer read(int size) throws IOException {
        if (closed) return ByteBuffer.allocateDirect(0);
        ByteBuffer buffer = current.read(size);
        if (buffer.hasRemaining()) return buffer;
        current.close();
        current = open();
        if (!current.getFormat().matches(format)) throw new IOException("Track format changed while playing.");
        buffer = current.read(size);
        if (!buffer.hasRemaining()) throw new IOException("Track contains no audio.");
        return buffer;
    }

    @Override public synchronized void close() throws IOException {
        if (!closed) { closed = true; current.close(); }
    }

}

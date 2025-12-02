package com.vessel.Kernel;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StreamingOutputStream extends OutputStream {

    @FunctionalInterface
    public interface Listener {
        void onText(String chunk);
    }

    private final Listener listener;
    private final StringBuilder buffer = new StringBuilder();

    public StreamingOutputStream(Listener listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        buffer.append((char) b);
        if (b == '\n') {
            flushBuffer();
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        String s = new String(b, off, len, StandardCharsets.UTF_8);
        buffer.append(s);
        int idx;
        while ((idx = buffer.indexOf("\n")) >= 0) {
            String line = buffer.substring(0, idx + 1);
            buffer.delete(0, idx + 1);
            listener.onText(line);
        }
    }

    @Override
    public synchronized void flush() {
        flushBuffer();
    }

    private void flushBuffer() {
        if (buffer.length() > 0) {
            listener.onText(buffer.toString());
            buffer.setLength(0);
        }
    }
}

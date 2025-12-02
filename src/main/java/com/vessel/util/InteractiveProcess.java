package com.vessel.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Wraps a child JVM process used to run interactive notebook code.
 * Stdout/stderr are streamed to the provided Consumers.
 * Lines for System.in are sent via sendLine(...).
 */
public class InteractiveProcess {

    private final Process process;
    private final BufferedWriter stdinWriter;

    public InteractiveProcess(Path classesDir,
                              String mainClassName,
                              Consumer<String> stdoutConsumer,
                              Consumer<String> stderrConsumer) throws IOException {

        // java executable
        String javaBin = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "java";

        // classpath: compiled user classes + current app classpath
        String classpath = classesDir.toString()
                + File.pathSeparator
                + System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "com.vessel.runner.NotebookRunner",
                mainClassName
        );

        this.process = pb.start();
        this.stdinWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)
        );

        // stdout reader
        Thread outThread = new Thread(
                () -> readStream(process.getInputStream(), stdoutConsumer),
                "Interactive-stdout"
        );
        outThread.setDaemon(true);
        outThread.start();

        // stderr reader
        Thread errThread = new Thread(
                () -> readStream(process.getErrorStream(), stderrConsumer),
                "Interactive-stderr"
        );
        errThread.setDaemon(true);
        errThread.start();
    }

    private void readStream(InputStream in, Consumer<String> consumer) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                consumer.accept(line + "\n");
            }
        } catch (IOException ignored) {
        }
    }

    /** Send one line to the child process's System.in. */
    public void sendLine(String line) throws IOException {
        stdinWriter.write(line);
        stdinWriter.newLine();
        stdinWriter.flush();
    }

    /** Wait for the child process to exit. */
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    /** Forcefully destroy the process (e.g. on cancel). */
    public void destroy() {
        process.destroyForcibly();
    }
}

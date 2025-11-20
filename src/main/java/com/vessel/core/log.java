package com.vessel.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public final class log {

    // cache so we don't recreate handlers
    private static final ConcurrentHashMap<String, log> cache = new ConcurrentHashMap<>();

    private final Logger logger;

    private log(String name) {
        this.logger = createLogger(name);
    }

    // ------------- PUBLIC API -------------

    public static log get(String name) {
        return cache.computeIfAbsent(name, log::new);
    }

    public void debug(String msg) { logger.fine("Debug: "+msg); }

    public void info(String msg) { logger.info(msg); }

    public void warn(String msg) { logger.warning(msg); }

    public void severe(String msg) { logger.severe(msg); }

    public void error(String msg, Throwable e) { logger.log(Level.SEVERE, msg, e); }

    public void warning(String msg) { logger.warning(msg); }

    // ------------- INTERNAL SETUP -------------

    private static Logger createLogger(String name) {

        Path dir = Path.of("logs", name);
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd")
        );


        // <name>-2025_11_10-12_44_12.log
        Path file = dir.resolve(name + "-" + timestamp + ".log");

        try {
            Files.createDirectories(dir);


            String fmt = "[%1$tF %1$tT] [%2$-7s] (%3$s) -> %4$s%n";

            Formatter customFormatter = new Formatter() {
                @Override
                public String format(LogRecord r) {
                    return String.format(
                            fmt,
                            r.getMillis(),
                            r.getLevel().getName(),
                            r.getSourceClassName() + "." + r.getSourceMethodName(),
                            r.getMessage()
                    );
                }
            };

            // File handler
            FileHandler fileHandler = new FileHandler(file.toString(), true);
            fileHandler.setFormatter(customFormatter);
            fileHandler.setLevel(Level.ALL);

            // Console handler for stdout (white text, not red)
            StreamHandler consoleHandler = new StreamHandler(System.out, customFormatter) {
                @Override
                public synchronized void publish(LogRecord record) {
                    super.publish(record);
                    flush(); // Make sure it prints immediately
                }
            };
            consoleHandler.setLevel(Level.ALL);

            // Configure logger
            Logger logger = Logger.getLogger(name);
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHandler);
            //logger.addHandler(consoleHandler);
            logger.setLevel(Level.ALL);


            System.out.println("log → " + file.toAbsolutePath());
            return logger;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create logger: " + name, e);
        }
    }
}

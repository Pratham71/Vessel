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

    public void config(String msg) { logger.config(msg); }

    public void severe(String msg) { logger.severe(msg); }

    public void error(String msg, Throwable e) { logger.log(Level.SEVERE, msg, e); }

    // ------------- INTERNAL SETUP -------------

    private static Logger createLogger(String name) {

        Path dir = Path.of("logs", name);
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm")
        );

        // <name>-2025_11_10-12_44_12.log
        Path file = dir.resolve(name + "-" + timestamp + ".log");

        try {
            Files.createDirectories(dir);

            FileHandler handler = new FileHandler(file.toString(), true);

            String fmt = "[%1$tF %1$tT] [%2$-7s] (%3$s) -> %4$s%n";

            handler.setFormatter(new Formatter() {
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
            });

            Logger logger = Logger.getLogger(name);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
            logger.setLevel(Level.ALL);
            handler.setLevel(Level.ALL);


            System.out.println("log → " + file.toAbsolutePath());
            return logger;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create logger: " + name, e);
        }
    }
}
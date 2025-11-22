package com.vessel.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility to detect the system's current light/dark theme.
 * Works on Windows, macOS, and Linux (GNOME).
 */

public class SystemThemeDetector {

    public enum Theme {
        LIGHT, DARK
    }

    public static Theme getSystemTheme() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                return detectWindowsTheme();
            } else if (os.contains("mac")) {
                return detectMacTheme();
            } else if (os.contains("nux") || os.contains("nix")) {
                return detectLinuxTheme();
            }
        } catch (Exception ignored) {
        }
        return Theme.LIGHT;
    }

    // ---------- Windows ----------
    private static Theme detectWindowsTheme() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        String output = readOutput(process);
        if (output.contains("0x0")) return Theme.DARK;
        if (output.contains("0x1")) return Theme.LIGHT;
        return Theme.LIGHT;
    }

    // ---------- macOS ----------
    private static Theme detectMacTheme() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        String output = readOutput(process).trim();
        if (output.isEmpty()) return Theme.LIGHT;
        if (output.equalsIgnoreCase("dark")) return Theme.DARK;
        return Theme.LIGHT;
    }

    // ---------- Linux (GNOME) ----------
    private static Theme detectLinuxTheme() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "color-scheme"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        String output = readOutput(process).toLowerCase();
        if (output.contains("dark")) return Theme.DARK;
        if (output.contains("light")) return Theme.LIGHT;
        return Theme.LIGHT;
    }

    // ---------- Helper ----------
    private static String readOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        )) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
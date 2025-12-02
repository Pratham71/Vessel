package com.vessel.util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compiles a notebook cell into a small Java program with a main() method,
 * so it can be run in a separate JVM with real System.in / System.out.
 */
public class InteractiveCompiler {

    public static class CompileResult {
        public final boolean success;
        public final Path classOutputDir;
        public final String mainClassName;
        public final String compilerOutput;

        public CompileResult(boolean success,
                             Path classOutputDir,
                             String mainClassName,
                             String compilerOutput) {
            this.success = success;
            this.classOutputDir = classOutputDir;
            this.mainClassName = mainClassName;
            this.compilerOutput = compilerOutput;
        }
    }

    public static CompileResult compileInteractive(String userCode) throws IOException {
        // 1) class name & package
        String className = "UserProgram";
        String fullClassName = "usercode." + className;

        // 2) Remove any import lines inside the cell body
        String cleanedCode = stripLeadingImports(userCode);

        // 3) Build full source for UserProgram.java
        String source =
                "package usercode;\n" +
                        "import java.util.*;\n" +
                        "public class " + className + " {\n" +
                        "  public static void main(String[] args) throws Exception {\n" +
                        cleanedCode + "\n" +
                        "  }\n" +
                        "}\n";

        // 4) Create temp dirs
        Path tempDir = Files.createTempDirectory("vessel-interactive-");
        Path srcDir = tempDir.resolve("src");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(srcDir);
        Files.createDirectories(classesDir);

        // 5) Write source file
        Path sourceFile = srcDir.resolve(className + ".java");
        Files.writeString(sourceFile, source);

        // 6) Get system JavaCompiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileResult(
                    false,
                    classesDir,
                    fullClassName,
                    "No system JavaCompiler; make sure you are running with a JDK, not a JRE."
            );
        }

        // 7) Capture compiler output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        int exitCode = compiler.run(
                null,                   // stdin
                ps,                     // stdout
                ps,                     // stderr
                "-classpath", System.getProperty("java.class.path"),
                "-d", classesDir.toString(),
                sourceFile.toString()
        );

        String compilerOut = baos.toString();

        return new CompileResult(exitCode == 0, classesDir, fullClassName, compilerOut);
    }

    /**
     * Remove import-lines from the cell body, so they don't appear inside main().
     * They are replaced by a single top-level "import java.util.*;" in the wrapper.
     */
    private static String stripLeadingImports(String code) {
        if (code == null || code.isBlank()) return "";
        StringBuilder body = new StringBuilder();
        String[] lines = code.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("import ")) {
                // ignore import lines from the cell
                continue;
            }
            body.append(line).append("\n");
        }
        return body.toString();
    }
}

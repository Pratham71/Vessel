/*
TODO: Backend Execution Engine

1) Persistent JShell kernel
   - JShell should be created once per notebook, not per cell
   - Maintain state of variables/classes across cells

2) runCell(NotebookCell cell)
   - Increment cell.executionCount
   - Detect if code contains `class`, `interface`, `enum`, or `record`
   - If YES:
       > Write code to TemporaryFile.java
       > Run `javac` to compile
       > Add output directory to JShell classpath
   - If NO:
       > Evaluate using JShell.eval(...)
   - Capture:
       > stdout (success output)
       > stderr / diagnostics (errors)
       > execution time (System.nanoTime())

3) Save / Load Notebook to JSON
   - saveNotebook(Notebook notebook)
     → Convert notebook object into JSON
     → Write to /notebooks/<name>.json

   - loadNotebook(String name)
     → Read JSON and reconstruct Notebook + Cells

4) Logging + Safety
   - Add Logger (java.util.logging or slf4j)
   - log compile errors, JShell errors, runtime exceptions
   - Timeout protection (cancel execution if infinite loop)
   - Catch OutOfMemoryError on huge allocations

Nice-to-have:
- Support stdin push (future feature)
- Colored output for UI (stdout=green, stderr=red)
*/



package com.vessel.Kernel;

import com.vessel.core.log;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

public class NotebookEngine {
    private final JShell jshell; // persistent kernel
    private final ByteArrayOutputStream outputbuffer;
    private final PrintStream outputstream;
    private final log engine = log.get("engine");

    // Some basic defaults for jsh.
    private static final List<String> IMPORT_SNIPPETS = List.of(
            "import java.io.*;",
            "import java.util.*;",
            "import java.lang.Math.*;"
    );


    private static final List<String> METHOD_SNIPPETS = List.of(
            "static void print(boolean b) { System.out.print(b); }",
            "static void print(char c) { System.out.print(c); }",
            "static void print(int i) { System.out.print(i); }",
            "static void print(long l) { System.out.print(l); }",
            "static void print(float f) { System.out.print(f); }",
            "static void print(double d) { System.out.print(d); }",
            "static void print(char s[]) { System.out.print(s); }",
            "static void print(int a[]) { System.out.print(a); }",
            "static void print(String s) { System.out.print(s); }",
            "static void print(Object obj) { System.out.print(obj); }",
            "static void println() { System.out.println(); }",
            "static void println(boolean b) { System.out.println(b); }",
            "static void println(char c) { System.out.println(c); }",
            "static void println(int i) { System.out.println(i); }",
            "static void println(long l) { System.out.println(l); }",
            "static void println(float f) { System.out.println(f); }",
            "static void println(double d) { System.out.println(d); }",
            "static void println(char s[]) { System.out.println(s); }",
            "static void println(String s) { System.out.println(s); }",
            "static void println(Object obj) { System.out.println(obj); }",
            "static void printf(java.util.Locale l, String format, Object... args) { System.out.printf(l, format, args); }",
            "static void printf(String format, Object... args) { System.out.printf(format, args); }"
    );


    private static final List<String> EXTRA_SNIPPETS = List.of(
            "static String now() { return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern(\"HH:mm:ss\")); }",
            "static String date() { return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(\"dd-MM-yyyy\")); }",
            "static String day() { return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern(\"EEEE\")); }",
            "static String dateTime() { return \"%s | %s | %s\".formatted(day(), date(), now()); }",
            "static int rand(int min, int max) { return java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1); }",
            "static long TimeThis(Runnable r) { long t = System.nanoTime(); r.run(); return (System.nanoTime() - t) / 1_000_000; }"
    );


    private static final List<String> INIT_SNIPPETS = List.of(
            // expands METHOD_SNIPPETS, IMPORT_SNIPPETS and EXTRA_SNIPPETS into one list
            METHOD_SNIPPETS,
            IMPORT_SNIPPETS,
            EXTRA_SNIPPETS
    ).stream().flatMap(List::stream).toList();


    private void SetInitSnippets(JShell jshell, List<String> INIT_SNIPPETS) {
        for (String snippet : INIT_SNIPPETS) {
            jshell.eval(snippet);

        }
        engine.info("All Init-Snippets were loaded into jshell.");
    }

    public NotebookEngine(){

        // Init. outputbuffer and outputstream
        outputbuffer = new ByteArrayOutputStream();
        outputstream = new PrintStream(outputbuffer);


        // Auto Importing Commonly used Pkgs

        jshell = JShell.builder()
                .out(outputstream) // Can be replaced by UI output capture.
                .err(outputstream)
                .build();

        // Loading imports + helpers
        SetInitSnippets(jshell,  INIT_SNIPPETS);
        engine.info("Notebook Engine Initialized");
    }

    // Clears Kernel, Useful for 'Restart Kernel' button in front end.
    public void resetKernel(){
        jshell.snippets().forEach(jshell::drop);
        SetInitSnippets(jshell,  INIT_SNIPPETS);
        outputbuffer.reset();
        engine.severe("kernel reset");
    }

    // let front end interrupt Kernel
    public void interrupt() {
        jshell.stop();
        engine.severe("kernel interrupt");
    }

    // Store Variables Between Cells and exposes listing
    public List<String> vars() {
        return jshell.variables()
                .map(v -> v.name() + " : " + v.typeName())
                .toList();
    }

    // Capture Imports: Let user add imports through cells and persist.
    public List<String> imports() {
        return jshell.imports()
                .map(i -> i.fullname())
                .toList();
    }

    // Execute java Code using JShell
    public String execute(String code) {
        engine.debug("Execute was invoked");
        try {

            outputbuffer.reset(); // clear stdout buffer each run

            StringBuilder sb = new StringBuilder();

            List<SnippetEvent> events = jshell.eval(code);

            long start = System.nanoTime();

            for(SnippetEvent event : events) {

                // Capture any Syntax or Compilation Error
                jshell.diagnostics(event.snippet()).forEach(diag -> {
                    sb.append("ERROR: ").append(diag.getMessage(null));
                    engine.severe("Jshell.diagnostic caught an error: " + diag.getMessage(null));
                });

                // Any runtime exception.
                if(event.exception() != null) {
                    sb.append("Exception: ").append(event.exception().getMessage()).append('\n');
                    engine.error("Runtime Error was caught: ", event.exception());

                }

                // Output
                if( (event.value() != null)) {
                    sb.append("OUTPUT: ").append(event.value().toString());
                    engine.debug("Execution was successful: Output( " + event.value() + " )");
                }
            }

            // capture println / stdout / stderr
            String printed = outputbuffer.toString();
            if (!printed.isEmpty()){
                sb.append(printed);
                engine.debug("Stdout / Stderr was caught: " + printed);
            }

            long time = (System.nanoTime() - start) / 1_000_000;
            sb.append("\nExecution time: ").append(time).append(" ms\n");
            engine.debug("Execution time: " + time + " ms");

            return sb.toString();
        } catch (Exception e){

            engine.error("Error executing jshell: ", e);
            return "Execution Failed: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        NotebookEngine engine = new NotebookEngine();
        System.out.println(engine.execute("for (int i = 0; i < 10; i++) { print(i); }"));
    }

}

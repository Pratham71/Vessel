/**
 * TODO - Implement NotebookEnigne to frontend via Notebook Class
 * TODO - Link to Notebook.Json
 */



package com.vessel.Kernel;
import com.vessel.core.log;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.vessel.model.NotebookCell;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

public class NotebookEngine {
    // === Persistent Jshell ===
    private final JShell jshell;


    // Buffers
    private ByteArrayOutputStream jshellBuffer;
    private PrintStream jshellPrintStream;

    // === Thread-Safety ===
    private final ReentrantLock executionLock = new ReentrantLock(); // Locks the execution thread.
    private volatile boolean isExecuting = false;

    // === Timeouts ===
    private static final long EXECUTION_TIMEOUT_MS = 5_000; // 5s
    private final ExecutorService executorService; // A Java thread-pool interface used to run tasks asynchronously.

    // === Loggers ===
    private final log engine = log.get("engine"); // looging

    // === Security ===
    private static final List<String> DANGEROUS_PATTERNS = List.of(
            "System.exit",
            "Runtime.getRuntime().exec",
            "ProcessBuilder",
            "Runtime.getRuntime().halt",
            "sun.misc.Unsafe"
    );

    // === Stats ===
    private int totalExecutions = 0;
    private long totalExecutionTime = 0;
    private final LinkedList<ExecutionResult> history = new LinkedList<>();
    private static final int MAX_HISTORY = 50;


    // === Init Scripts ===
    private static final List<String> IMPORT_SNIPPETS = List.of(

            // basic
            "import java.io.*;",
            "import java.util.*;",

            // math
            "import java.lang.Math.*;",
            "import java.math.*;",
            "import java.lang.*;"

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

    private void loadInitSnippets(JShell jshell, List<String> INIT_SNIPPETS) {
        executionLock.lock();
        try {
            for (String snippet : INIT_SNIPPETS) {
                jshell.eval(snippet);

            }
            engine.info(" All init snippets loaded into JShell.");
        } finally {
            executionLock.unlock();
        }
    }

    // === Constructor ===

    public NotebookEngine() {

        // Thread Safe executor for timeout handling
        executorService = Executors.newCachedThreadPool(
                r -> {
                    Thread t = new Thread(r);

                    // Make the thread a daemon so it won't block JVM shutdown
                    t.setDaemon(true);

                    // Give each thread a readable name based on the current cell
                    t.setName("Jshell-Worker");

                    engine.severe(" Jshell thread started: " + "`" + t.getName() + "`");
                    return t;
                }
        );

        jshellBuffer = new ByteArrayOutputStream();
         jshellPrintStream = new PrintStream(jshellBuffer, true); // autoflush

        // Init. JShell with output streams
        this.jshell = JShell.builder()
                .out(jshellPrintStream)
                .err(jshellPrintStream)
                .build();

        // Load Init Snippets
        loadInitSnippets(jshell, INIT_SNIPPETS);
        engine.info(" NotebookEngine initialized with persistent JShell kernel");
    }



    // Thread safe execution.
    public Void execute(NotebookCell cell) {
        String code = cell.getContent();

//     Vallidation
        if (code == null || code.trim().isBlank()) {
            cell.setExecutionResult(new ExecutionResult("", "Empty Code Cell", 0, false));
        }

        // checking for any dangeorus pattern which may cause the program to crashout.
        for (String pattern : DANGEROUS_PATTERNS) {
            if (code.contains(pattern)) {
                engine.warn(" Blocked dangerous pattern: " + pattern);

                cell.setExecutionResult(new ExecutionResult("", "Blocked dangerous operation: " + pattern, -1, false));
            }
        }

        // Try to get the execution lock (non-blocking)
        if (!executionLock.tryLock()) {
            engine.warning(" Execution blocked: Another cell is running.");
            cell.setExecutionResult(new ExecutionResult("", "Execution blocked: Another cell is running.\nPlease wait or interrupt it.", -1, false));
        }

        try {
            isExecuting = true;
            engine.info(" Executing code");

            // Submit execution with timeout
            Future<ExecutionResult> future = executorService.submit(() -> executeInternal(code));

            try {

                // Wait for the task to finish, but only up to EXECUTION_TIMEOUT_MS
                ExecutionResult result = future.get(EXECUTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                // Updating Stats
                totalExecutions++;
                totalExecutionTime += result.executionTimeMs();
                //addToHistory(code, result);

                engine.info(" Code Execution complete");
                cell.setExecutionResult(result);

            } catch (TimeoutException e) {

                engine.debug(" Execution timed out.");

                // Cancel the running task
                future.cancel(true);

                // Reset the JShell session
                jshell.stop();

                // Return a timeout result
                cell.setExecutionResult(new ExecutionResult("", "TIMEOUT: Execution Exceeded " + (EXECUTION_TIMEOUT_MS / 1000) +
                        " Possible infinite loop or recursion."
                        , EXECUTION_TIMEOUT_MS, false));

            } catch (InterruptedException ie) {

                // Cancel the running task
                future.cancel(true);

                // Restore the interrupt status
                Thread.currentThread().interrupt();

                engine.error(" Execution interrupted", ie);

                // Return an interrupted result
                cell.setExecutionResult(new ExecutionResult("", "Execution interrupted by user", -1, false));

            } catch (ExecutionException e) {
                engine.error(" Execution failed with exception: ", e);

                // Get the underlying cause of the exception
                Throwable cause = e.getCause();
                if (cause == null) cause = e;

                // Return a fatal error result
                cell.setExecutionResult(new ExecutionResult("", "FATAL ERROR: " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), -1, false));
            }

        } finally {
            isExecuting = false;
            executionLock.unlock();
            engine.debug(" Execution lock Released.");
        }
        return null;
    }

    // Internal Execution (Runs in executor thread)
    private ExecutionResult executeInternal(String code) {
        // Start timer
        long startTime = System.nanoTime();

        // Clear the persistent buffer before running new code
        jshellBuffer.reset();

        // Output builder and success flag
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        final boolean[] success = {true};

        try {
            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUserPercentage = (usedMemory * 100.0 / maxMemory);

            if (memoryUserPercentage > 65) {
                engine.warning(" Memory usage high");
                errors.append(" Warning: Memory Usage at ")
                        .append(String.format("%.1f", memoryUserPercentage))
                        .append("%\n\n");
            }
            //Split code into individual statements using the semicolon as a delimiter
            String[] statements=code.split(";\\s*");

            // list will accumulate all events from all stmts
            List<SnippetEvent> events = new ArrayList<>();
            boolean currentSuccess=true;

            for (String statement:statements){
                String trimmedStatement = statement.trim();

                // Skip empty snippets (resulting from splitting, comments, or blank lines)
                if (trimmedStatement.isBlank()) continue;

                // 2. Re-add a semicolon for JShell to recognize it as a complete statement/expression
                String snippetCode = trimmedStatement.endsWith(";") ? trimmedStatement : trimmedStatement + ";";

                // 3. Execute JShell snippet for the statement sequentially.
                //outputStream.flush();
            List<SnippetEvent> statementEvents = jshell.eval(snippetCode);
                events.addAll(statementEvents);
                // Check for errors on this statement
                for (SnippetEvent event : statementEvents) {
                    if (event.status() == jdk.jshell.Snippet.Status.REJECTED || event.exception() != null) {
                        currentSuccess = false;
                    }
                }
            }
            success[0]=currentSuccess;
            System.out.println("events.size() = " + events.size());

            if (events.isEmpty()) {
                errors.append(" No output (empty snippet)");
            }

            // Process each event
            for (SnippetEvent event : events) {
                // Snippet status handling
                switch (event.status()) {
                    case REJECTED:
                        errors.append(" REJECTED: Code could not be Compiled\n");
                        success[0] = false;
                        break;
                    case OVERWRITTEN:
                        errors.append(" Previous definition overwritten\n");
                        break;
                    case VALID:
                        break;
                    case RECOVERABLE_DEFINED:
                        errors.append(" Defined with recoverable errors\n");
                        break;
                    case RECOVERABLE_NOT_DEFINED:
                        errors.append(" Defined with recoverable errors\n");
                        break;
                }

                // Compilation diagnostics
                jshell.diagnostics(event.snippet()).forEach(diag -> {
                    errors.append("Compilation Error: ")
                            .append(diag.getMessage(null)).append("\n");
                    engine.severe(" Compilation error: " + diag.getMessage(null));
                    success[0] = false;
                });

                // Runtime exception
                if (event.exception() != null) {
                    Exception exception = event.exception();
                    errors.append(" Runtime Exception: ")
                            .append(exception.getClass().getSimpleName())
                            .append(": ").append(exception.getMessage())
                            .append("\n");

                    // First stack frame
                    StackTraceElement[] stackTrace = exception.getStackTrace();
                    if (stackTrace.length > 0) {
                        errors.append("   at ").append(stackTrace[0]).append("\n");
                    }

                    engine.error("Runtime exception caught: ", exception);
                    success[0] = false;
                }

//                // Expression result
//                if (event.value() != null && !event.value().isEmpty()) {
//                    output.append("Expressions value: ")
//                            .append(event.value()).append("\n");
//                    engine.debug("Expressions value: " + event.value());
//                }
            }

            // Capture STDOUT
            // Force flushing just to be safe. (even though auto-flush is set true on init.)
            jshellPrintStream.flush();
            String printed = jshellBuffer.toString();

            if (!printed.isEmpty()) {
                output.append(printed);
                if (!printed.endsWith("\n")) output.append("\n");
                engine.debug("Stdout captured: " + printed.length() + " chars");
            }

        } catch (OutOfMemoryError e) {
            // Out-of-memory handler
            engine.severe("OutOfMemoryError: " + e.getMessage());
            output.append("FATAL: Out of memory!\n")
                    .append("The kernel will be reset.\n");
            success[0] = false;

            // Reset kernel async
            CompletableFuture.runAsync(this::resetKernel);

        } catch (StackOverflowError e) {
            // Stack overflow handler
            engine.severe("StackOverflowError: " + e.getMessage());
            errors.append("FATAL: Stack overflow!\n")
                    .append("Likely infinite recursion.\n");
            success[0] = false;

        } catch (Exception e) {
            // General error handler
            engine.error("Unexpected error", e);
            errors.append(" UNEXPECTED ERROR: ")
                    .append(e.getClass().getSimpleName()).append(": ")
                    .append(e.getMessage()).append("\n");
            success[0] = false;

        }

        // Compute execution time
        long executionTime = (System.nanoTime() - startTime) / 1_000_000;

        // Append time
        output.append("\nExecution time: ")
                .append(executionTime).append(" ms\n");

        // Return result
        return new ExecutionResult(output.toString(), errors.toString(), executionTime, success[0]);
    }

    // Clears Kernel, Useful for 'Restart Kernel' button in front end.
    public void resetKernel() {
        // Lock to prevent concurrent resets/executions
        executionLock.lock();

        try {
            engine.info("Resetting kernel...");

            // Drop all user-defined snippets
            jshell.snippets().forEach(snippet -> jshell.drop(snippet));

            // Reload initial snippets
            loadInitSnippets(jshell, INIT_SNIPPETS);

            // Clear stats and history
            totalExecutions = 0;
            totalExecutionTime = 0;
            history.clear();

            engine.info("Kernel Reset Complete.");
        } finally {
            // Release lock
            executionLock.unlock();
        }
    }


    // let front end interrupt Kernel
    // Interrupt the currently running execution
    public void interrupt() {

        // Check if a cell is currently executing
        if (isExecuting) {

            // Stop the JShell session
            jshell.stop();

            // Log interruption
            engine.warning("Kernel Interrupted by User.");

        } else {

            // No active execution
            engine.info("No execution in progress to interrupt.");
        }
    }


    public List<String> getVariables() {
        executionLock.lock();
        try {
            return jshell.variables()
                    .map(v -> v.name() + " : " + v.typeName() + " = " +
                            (v.typeName().equals("String") ? ("\"" + jshell.varValue(v).describeConstable().orElse("null") + "\"") :
                                    jshell.varValue(v).describeConstable().orElse("null"))
                    ).toList();
        } finally {
            executionLock.unlock();
        }
    }


    // Getters.
    public List<String> getImports() {
        executionLock.lock();
        try {
            return jshell.imports()
                    .map(i -> i.fullname())
                    .toList();
        } finally {
            executionLock.unlock();
        }
    }

    public List<String> getMethods() {
        executionLock.lock();
        try {
            return jshell.methods()
                    .map(m -> m.signature() + " " + m.name() + "(" + m.parameterTypes() + ")")
                    .toList();
        } finally {
            executionLock.unlock();
        }
    }

    public Map<String, Object> getStatistics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", totalExecutions);
        stats.put("averageExecutionTimeMs", totalExecutions > 0 ? totalExecutionTime / totalExecutions : 0);
        stats.put("totalExecutionTime", totalExecutionTime);
        stats.put("variableCount", getVariables().size());
        stats.put("methodCount", getMethods().size());
        stats.put("importsCount", getImports().size());
        stats.put("classCount", jshell.types().count());
        stats.put("isExecuting", isExecuting);
        stats.put("memoryTotalMB", usedMemory / 1024 / 1024);
        stats.put("memoryUsedMB", usedMemory / 1024 / 1024);
        stats.put("memoryMaxMB", maxMemory / 1024 / 1024);
        stats.put("memoryUsagePercent", String.format("%.1f", (usedMemory * 100.0 / maxMemory)));

        return stats;
    }

//    private void addToHistory(String code, ExecutionResult result) {
//        history.add(new ExecutionResult(
//                code,
//                result.output(),
//                result.executionTimeMs(),
//                result.success()
//        ));
//
//        engine.info(" Adding " + code + " to history");
//
//        if (history.size() > MAX_HISTORY) {
//            history.removeFirst();
//        }
//    }
//
//    public List<ExecutionResult> getHistory() {
//        return new ArrayList<ExecutionResult>(history);
//    }
//
//    public void clearHistory() {
//        history.clear();
//        engine.info(" Clearing history...");
//    }

    // Cleanup and close jshell safely
    public void shutdown() {
        engine.info(" Shutting down NotebookEngine...");

        // Shutdown executor service
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                engine.warning(" Executor service did not terminate cleanly.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            engine.error(" Shutting down interrupted", ie);
        }

        // close jshell
        if (jshell != null) {
            jshell.close();
            engine.info(" Shutting down JShell...");
        }

        //clearHistory();

        engine.info(" NotebookEngine shutdown complete.");
    }

    // === Utilities ===
    public boolean isExecuting() {
        return isExecuting;
    }
}
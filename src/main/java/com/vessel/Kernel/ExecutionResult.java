package com.vessel.Kernel;

/**
 * Represents the result of executing a code cell in the notebook.
 *
 * A record is a special Java class (Java 14+) that automatically creates:
 * - Private final fields for each parameter
 * - A constructor with all fields
 * - Getters for each field (output(), error(), executionTimeMs(), success())
 * - equals(), hashCode(), and toString() methods
 *
 * Records are immutable and perfect for simple data containers.
 *
 * @param output The standard output/printed content from execution
 * @param error Error messages if execution failed (empty string if successful)
 * @param executionTimeMs Time taken to execute in milliseconds
 * @param success True if execution completed without errors, false otherwise
 */
public record ExecutionResult(String output, String error, long executionTimeMs, boolean success) {}
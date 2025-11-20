package com.vessel.Kernel;

/**
 * Stores the result of a single code cell execution.
 *
 * A record is a special Java class (Java 14+) that automatically creates:
 * - Private final fields for each parameter
 * - A constructor with all fields
 * - Getters for each field (output(), error(), executionTimeMs(), success())
 * - equals(), hashCode(), and toString() methods
 *
 * Records are immutable and perfect for simple data containers.
 *
 * @param output The printed output from the code
 * @param error Any error messages
 * @param executionTimeMs How long it took to run (milliseconds)
 * @param success Whether it executed without errors
 */
public record ExecutionRecord(String output, String error, long executionTimeMs, boolean success) {}
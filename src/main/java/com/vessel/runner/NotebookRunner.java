// src/main/java/com/vessel/runner/NotebookRunner.java
package com.vessel.runner;

public class NotebookRunner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No class name provided");
            return;
        }
        String userClassName = args[0];

        Class<?> userClass = Class.forName(userClassName);
        var mainMethod = userClass.getMethod("main", String[].class);
        String[] mainArgs = new String[0];
        mainMethod.invoke(null, (Object) mainArgs);
    }
}

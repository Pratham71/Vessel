package com.vessel.model;

// USE THIS NOW FOR CELL TYPES!!!
// Import wherever you need to use multiple cell types
public enum CellType {
    CODE("Java Code"),
    MARKDOWN("Markdown"),
    TEXT("Plain Text");

    private final String displayName;

    CellType(String name) {
        this.displayName = name;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
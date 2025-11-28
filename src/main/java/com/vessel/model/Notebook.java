/*
TODO: Notebook Model (Notebook)

Required:
- Maintain notebook name (used for saving/loading)
- Maintain List<NotebookCell>
- addCell(cell)
- removeCell(cellId)
- getCell(cellId)
- getCells()

Optional:
- Track creation date / last modified date
*/
package com.vessel.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Notebook {

    // Notebook name (used for saving/loading as JSON)
    private final String name;

    // All cells in this notebook
    private final List<NotebookCell> cells = new ArrayList<>();

    // Optional: metadata for real-world use
    private final Instant createdAt;
    private Instant lastModifiedAt;

    public Notebook(String name) {
        this.name = name;
        this.createdAt = Instant.now();
        this.lastModifiedAt = this.createdAt;
    }

    // Add a new cell to the notebook
    public void addCell(NotebookCell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("cell cannot be null");
        }
        cells.add(cell);
        touch();
    }

    // Remove a cell by ID
    public boolean removeCell(String cellId) {
        if (cellId == null) {
            return false;
        }
        boolean removed = cells.removeIf(cell -> cellId.equals(cell.getId()));
        if (removed) {
            touch();
        }
        return removed;
    }

    // Get a specific cell by ID
    public NotebookCell getCell(String cellId) {
        if (cellId == null) {
            return null;
        }
        for (NotebookCell cell : cells) {
            if (cellId.equals(cell.getId())) {
                return cell;
            }
        }
        return null;
    }

    // Return an unmodifiable view of all cells
    public List<NotebookCell> getCells() {
        return Collections.unmodifiableList(cells);
    }

    // If you literally need a list of IDs instead:
    public List<String> getCellIds() {
        List<String> ids = new ArrayList<>(cells.size());
        for (NotebookCell cell : cells) {
            ids.add(cell.getId());
        }
        return Collections.unmodifiableList(ids);
    }

    // Notebook name (used for JSON save filename)
    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    private void touch() {
        lastModifiedAt = Instant.now();
    }
}
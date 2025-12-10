package com.vessel.model;

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

import java.util.ArrayList;
import java.util.List;

import com.vessel.Kernel.NotebookEngine;
import com.vessel.Kernel.ExecutionResult;

public class Notebook {

    private String name;
    private List<NotebookCell> cells = new ArrayList<>();
    private transient NotebookEngine engine;

    private boolean used = false; //check whether user has unsaved changes
    private String filePath = null; // stores the full file path on disk where this notebook is saved.


    public void markUsed() {
        this.used = true;
    }

    public void markSaved() {
        this.used = false;
    }

    // check if notebook has unsaved changes
    public boolean isUsed() {
        return used;
    }

    public Notebook(String name) {
        this.name = name;
        initEngineIfNull();
    }
    // Add a new cell to the notebook
    public void addCell(NotebookCell cell) {
        cells.add(cell);
        markUsed();
    }

    // Remove a cell by ID
    public void removeCell(String cellId) {
        cells.removeIf(cell -> cell.getId().equals(cellId));
        markUsed();
    }

    // Get a specific cell by ID
    public NotebookCell getCell(String cellId) {
        return cells.stream()
                .filter(cell -> cell.getId().equals(cellId))
                .findFirst()
                .orElse(null);
    }

    // updates the notebook name without creating a new object
    public void setName(String name) {
        this.name = name;
        markUsed();
    }


    // Return all cells for rendering
    public List<NotebookCell> getCells() {
        return cells;
    }

    // Notebook name (used for JSON save filename)
    public String getName() {
        return name;
    }

    // Engine realated code:
    public NotebookEngine getEngine() { return engine; }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void shutdownEngine(){
        if (this.engine == null) {
            return;
        }
        if(this.engine.isExecuting()){
            this.engine.interrupt();
        }
        this.engine.shutdown();
        this.engine = null;
    }
    public void initEngineIfNull() {
        if (this.engine == null) {
            this.engine = new NotebookEngine();
        }
    }
}

/*
TODO: Cell Model (NotebookCell)

- Store unique ID (UUID)
- Store cell type (CODE / MARKDOWN)
- Store content (string)
- Maintain execution count (In[n])
- Maintain outputs: List<Output> (stdout/stderr)
- Add timestamps: createdAt, lastModifiedAt

Nice-to-have:
- Add title field (optional)
- Auto-update lastModifiedAt on content change
*/

package com.vessel.model;

import com.vessel.Kernel.ExecutionResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public class NotebookCell {

//    public static class Output {
//        public enum Type { STDOUT, STDERR }
//        public final Type type;
//        public final String text;
//
//        public Output(Type type, String text) {
//            this.type = type;
//            this.text = text;
//        }
//    }

    private transient ExecutionResult executionResult;
    private final String id = UUID.randomUUID().toString();
    private CellType cellType;
    private String content;
    private int executionCount = 0;
    private List<ExecutionResult> outputs = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    public String getId() { return id; }
    public CellType getType() { return cellType; }
    public void setType(CellType type) { this.cellType = type; }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.lastModifiedAt = LocalDateTime.now();
    }

    public List<ExecutionResult> getOutputs() { return outputs; }
    // public void addOutput( NotebookCell.Output.Type type, String text){ outputs.add(new NotebookCell.Output(type, text)); }
    // Removes old output before running again for the same cell.
    // public void clearOutputs(){ outputs.clear(); }

    // temp debug method cuz im too dumb to use logs :(
    public void dumpContent(){
        System.out.println("Notebook Cell Type: " + cellType);
        System.out.println("Notebook Cell Content: " + content);
        System.out.println("Notebook Cell Created At: " + createdAt);
        System.out.println("Notebook Cell Last Modified At: " + lastModifiedAt);
        System.out.println("Notebook Cell Execution Count: " + executionCount);
    }

    public void setExecutionResult(ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }
    public ExecutionResult getExecutionResult() { return executionResult; }
    public int getExecutionCount() { return executionCount; }
    public void incrementExecutionCount() { executionCount++; }
}
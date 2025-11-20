// NEED TO FIX THIS!
package com.vessel;

import com.vessel.Kernel.NotebookEngine;
import com.vessel.model.NotebookCell;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
// it is for single block of code cell running and saving and writing lines code
public class CodeCell {

    @FXML
    private TextArea codeArea;

    @FXML
    private Button runButton;

    @FXML
    private Button saveButton;

    private NotebookEngine engine;
    private NotebookCell notebookCell;

    @FXML
    public void initialize() {
        runButton.setOnAction(e -> runCode());
        saveButton.setOnAction(e -> saveCell());
    }

    // Just updates model with latest text for rn
    // Later Notebook class will save it to json <3
    private void saveCell() {
        notebookCell.setContent(codeArea.getText());
        System.out.println("cell saved: "  + notebookCell.getContent());
    }


    private void runCode() {
        String code = codeArea.getText();
        notebookCell.setContent(code);
        notebookCell.clearOutputs();

        String output = engine.execute(code).output();
        String error = engine.execute(code).error();

        notebookCell.incrementExecutionCount();
        notebookCell.addOutput(NotebookCell.Output.Type.STDOUT, output);

        System.out.println("output saved: "  + notebookCell.getContent());
    }

}

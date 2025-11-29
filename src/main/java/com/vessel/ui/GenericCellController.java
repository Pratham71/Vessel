package com.vessel.ui;

import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;

public class GenericCellController {
    @FXML protected VBox root; // This is the root of the cell
    @FXML protected ChoiceBox<CellType> cellLanguage;
    @FXML protected CodeArea codeArea;
    @FXML private Label promptLabel;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    protected VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    protected NotebookCell cellModel;

    // Called before the specific cell type is initialized
    protected void initialize() {
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);

        // --- CELL MODEL LISTENERS ---
        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) cellModel.setContent(newText);
        });

        // Listener for setting cell model's "type" on type change (in the dropbox)
        cellLanguage.setOnAction(e -> {
            if (cellModel != null) cellModel.setType(cellLanguage.getValue());
        });

        // --- INITIAL PROMPT ---
        promptLabel.setMouseTransparent(true);  // let clicks go to the CodeArea

        // show prompt only when empty
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            boolean empty = newText == null || newText.isEmpty();
            promptLabel.setVisible(empty);
        });

        // --- BUTTON LISTENERS --
        deleteBtn.setOnAction(e -> deleteCell());

        clearBtn.setOnAction(e -> codeArea.clear());
    }

    public void setNotebookCell(NotebookCell cell) {
        this.cellModel = cell;

        // Only set default text if the cell is empty (not re-loading output)
        if (cell.getContent() == null || cell.getContent().isBlank()) {
            // Ask subclass for its default text (can be empty)
            String initText = getInitialContent();

            if (initText != null) {
                codeArea.replaceText(initText);
                cell.setContent(initText);
            }
        } else {
            // Fill UI from whatever the model contains (e.g. on loading)
            codeArea.replaceText(cell.getContent());
        }

        cellLanguage.setValue(cell.getType());
    }

    public void setParentContainer(VBox parent) {
        this.parentContainer = parent;
    }

    public void setRoot(VBox root) {
        this.root = root;
    }

    public void setCellType(CellType type) {
        cellLanguage.setValue(type);
    }

    // hook for subclasses (specifically CodeCellController) to use
    protected String getInitialContent() {
        return null; // default: no template
    }
    protected void deleteCell() {
        if (parentContainer != null && root != null) {
            parentContainer.getChildren().remove(root);
        }
    }
}
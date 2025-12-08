package com.vessel.ui;

import com.vessel.Kernel.NotebookEngine;
import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;


public class GenericCellController {
    // === INHERITED BY SUBCLASSES ===
    protected NotebookController notebookController;

    @FXML protected Button deleteBtn;
    @FXML protected Button clearBtn;

    @FXML protected Pane root; // This is the root of the cell
    @FXML protected ChoiceBox<CellType> cellLanguage;
    @FXML protected CodeArea codeArea;
    @FXML protected Label promptLabel;

    protected VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    protected NotebookCell cellModel;
    protected NotebookEngine engine;

    // Called before the specific cell type is initialized
    protected void initialize() {
        if (cellLanguage != null) {
            cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
            cellLanguage.setValue(CellType.CODE);

            cellLanguage.setOnAction(e -> {
                if (cellModel == null) return;

                CellType newType = (CellType) cellLanguage.getValue();
                cellModel.setType(newType);

                // Ask the notebook to switch this cell's UI to the new type
                if (notebookController != null && root != null) {
                    notebookController.switchCellType(this, newType);
                }
            });
        }

        // --- CELL MODEL LISTENERS ---
        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) cellModel.setContent(newText);
        });

        // Listener for setting cell model's "type" on type change (in the dropbox)

        // --- INITIAL PROMPT ---
        promptLabel.setMouseTransparent(true);  // let clicks go to the CodeArea

        // show prompt only when empty
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            boolean empty = newText == null || newText.isEmpty();
            promptLabel.setVisible(empty);
        });

        // --- BUTTON LISTENERS --
        deleteBtn.setOnAction(e -> confirmDelete());
        clearBtn.setOnAction(e -> confirmClear());
    }

    public void setNotebookController(NotebookController controller) {
        this.notebookController = controller;
    }

    public void setNotebookCell(NotebookCell cell) {
        this.cellModel = cell;

        if (cell.getContent() != null && !cell.getContent().isBlank()) {
            // Fill UI from whatever the model contains (e.g. on loading)
            codeArea.replaceText(cell.getContent());
        }

        if (cellLanguage != null && cell.getType() != null) {
            cellLanguage.setValue(cell.getType());
        }
    }

    public NotebookCell getNotebookCell() {
        return cellModel;
    }

    public void setParentContainer(VBox parent) {
        this.parentContainer = parent;
    }

    public void setRoot(Pane root) {
        this.root = root;
    }

    public Pane getRoot() {
        return root;
    }

    public void setCellType(CellType type) {
        if (cellLanguage != null) {
            cellLanguage.setValue(type);
        }

        if (cellModel != null) {
            cellModel.setType(type);
        }
    }

    // Its easier to have all cell types secretly hold an engine,
    // rather than have it instantiate and un-instantiate everytime you switch types
    // Only difference is non code cells cant RUN the engine
    public void setEngine(NotebookEngine engine) {
        this.engine = engine;
    }

    protected void deleteCell() {
        if (parentContainer != null && root != null) {
            parentContainer.getChildren().remove(root);
        }

        if (cellModel != null) {
            // also remove from notebook model
            notebookController.getNotebook().removeCell(cellModel.getId());
        }
    }
    private void confirmDelete() {
        try {
            Alert alert = generateAlert();

            alert.setTitle("Delete Cell");
            alert.setHeaderText("Are you sure you want to delete this cell?");

            ButtonType yes = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    deleteCell();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void confirmClear() {
        try {
            Alert alert = generateAlert();

            alert.setTitle("Clear Cell");
            alert.setHeaderText("Clear all text from this cell?");

            ButtonType yes = new ButtonType("Clear", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    codeArea.clear();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Alert generateAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            alert.initOwner(root.getScene().getWindow());
        } else {
            System.err.println("Warning: root or window is null, Alert owner not set.");
        }

        // Remove default Modena stylesheet
        alert.getDialogPane().getStylesheets().clear();

        boolean isDarkMode = SystemThemeDetector.getSystemTheme() == SystemThemeDetector.Theme.DARK;
        String theme = isDarkMode ? "/dark.css" : "/light.css";
        var cssResource = getClass().getResource(theme);

        if (cssResource == null) {
            System.err.println("ERROR: Stylesheet not found: " + theme);
        } else {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        }
        return alert;
    }

    public int getCaretPosition() {
        return codeArea.getCaretPosition();
    }

    public void restoreCaret(int caretPos, javafx.scene.control.IndexRange sel) {
        codeArea.moveTo(caretPos);
        if (sel != null && (sel.getStart() != sel.getEnd())) {
            codeArea.selectRange(sel.getStart(), sel.getEnd());
        }
    }

    public javafx.scene.control.IndexRange getSelection() {
        return codeArea.getSelection();
    }
}
package com.vessel.ui;

import com.vessel.Kernel.NotebookEngine;
import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.HBox;


public class GenericCellController {
    @FXML private Label promptLabel;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;

    private NotebookController notebookController;

    // === INHERITED BY SUBCLASSES ===

    @FXML protected VBox root; // This is the root of the cell
    @FXML protected ChoiceBox<CellType> cellLanguage;
    @FXML protected CodeArea codeArea;

    protected VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    protected NotebookCell cellModel;
    protected NotebookEngine engine;

    // Called before the specific cell type is initialized
    protected void initialize() {
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);

        // --- CELL MODEL LISTENERS ---
        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) {
                cellModel.setContent(newText);

                // mark notebook as having unsaved changes
                if (notebookController != null) {
                    notebookController.getCurrentNotebook().markUsed();
                }
            }
        });

        // Listener for setting cell model's "type" on type change (in the dropbox)
        cellLanguage.setOnAction(e -> {
            if (cellModel != null) {
                cellModel.setType(cellLanguage.getValue());
                markNotebookUsed(); // 🔥 mark notebook as modified
            }
        });


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

        cellLanguage.setValue(cell.getType());
    }

    public NotebookCell getNotebookCell() {
        return cellModel;
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
            notebookController.getCurrentNotebook().markUsed();

        }
    }

    // whenever a cell's content changes, mark notebook as used
    protected void markNotebookUsed() {
        if (notebookController != null) {
            notebookController.getCurrentNotebook().markUsed();
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
                    markNotebookUsed();
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
}
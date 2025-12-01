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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.HBox;


public class GenericCellController {
    @FXML protected VBox root; // This is the root of the cell
    @FXML protected ChoiceBox<CellType> cellLanguage;
    @FXML protected CodeArea codeArea;
    @FXML private Label promptLabel;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML protected HBox controlsContainer; // Added fx:id to FXML
    @FXML private Label executionCountLabelFXML; // Placeholder Label from FXML
    @FXML private Button runBtn; // The button that toggles between Run/Cancel

    // Field to hold the thread/task of the current execution
    private Thread executionThread = null;


    protected VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    protected NotebookCell cellModel;
    private int executionCount = 0;

    // Called before the specific cell type is initialized
    protected void initialize() {
        executionCountLabelFXML.setText("[-]");
        executionCountLabelFXML.getStyleClass().add("execution-count-label");
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
        deleteBtn.setOnAction(e -> confirmDelete());
        clearBtn.setOnAction(e -> confirmClear());
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
    private void confirmDelete() {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
                alert.initOwner(root.getScene().getWindow());
            } else {
                System.err.println("Warning: root or window is null, Alert owner not set.");
            }

            // Remove default Modena stylesheet
            alert.getDialogPane().getStylesheets().clear();

            boolean isDarkMode = SystemThemeDetector.getSystemTheme() == SystemThemeDetector.Theme.DARK;

            // Remove leading slash to avoid resource loading issues
            String theme = isDarkMode ? "resources/dark.css" : "resources/light.css";
            var cssResource = getClass().getResource(theme);

            if (cssResource == null) {
                System.err.println("ERROR: Stylesheet not found: " + theme);
            } else {
                alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
            }

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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
                alert.initOwner(root.getScene().getWindow());
            } else {
                System.err.println("Warning: root or window is null, Alert owner not set.");
            }

            // Remove default Modena stylesheet
            alert.getDialogPane().getStylesheets().clear();

            boolean isDarkMode = SystemThemeDetector.getSystemTheme() == SystemThemeDetector.Theme.DARK;
            String theme = isDarkMode ? "style/dark-theme.css" : "style/light-theme.css";
            var cssResource = getClass().getResource(theme);

            if (cssResource == null) {
                System.err.println("ERROR: Stylesheet not found: " + theme);
            } else {
                alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
            }

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
    public void incrementAndDisplayExecutionCount() {
        executionCount++;
        executionCountLabelFXML.setText("[" + executionCount + "]");
    }


}
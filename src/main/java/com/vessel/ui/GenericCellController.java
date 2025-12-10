package com.vessel.ui;

import com.vessel.Kernel.NotebookEngine;
import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.application.Platform;
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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @FXML protected Button moveUpBtn;
    @FXML protected Button moveDownBtn;

    public void updateEngine(NotebookEngine newEngine) {
        this.engine = newEngine;
    }
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

        moveUpBtn.setOnAction(e -> moveCellUp());
        moveDownBtn.setOnAction(e -> moveCellDown());

        deleteBtn.setOnAction(e -> confirmDelete());
        clearBtn.setOnAction(e -> confirmClear());

        // === AUTO-INDENT ON ENTER ===
        codeArea.setOnKeyReleased(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {

                // Use Platform.runLater() for consistency, although sometimes not strictly
                // needed here
                Platform.runLater(() -> {
                    int caretPos = codeArea.getCaretPosition();

                    // The ENTER key has already been processed by the system,
                    // inserting the newline character (\n).

                    // We need to look at the line *above* the new caret position (which is now line
                    // 3)
                    // to find the indentation of line 1.

                    int currentParagraphIndex = codeArea.getCurrentParagraph();

                    // Safety check: ensure we are not on the first line after a fresh startup
                    if (currentParagraphIndex < 1) {
                        return;
                    }

                    // Get the text of the line *before* the current position (the line we just
                    // left)
                    String previousLine = codeArea.getParagraph(currentParagraphIndex - 1).getText();

                    // --- Calculate indentation ---
                    int indentEnd = 0;
                    for (char c : previousLine.toCharArray()) {
                        if (c == ' ' || c == '\t') {
                            indentEnd++;
                        } else {
                            break;
                        }
                    }
                    // Indentation from the line we just left
                    String indent = previousLine.substring(0, indentEnd);

                    String trimmedLine = previousLine.trim();
                    boolean shouldAddExtraIndent = trimmedLine.endsWith("{");

                    if (shouldAddExtraIndent) {
                        indent += "    "; // Add extra indent for blocks
                    }

                    // The caret is currently at the start of the new line (Line 3).
                    // We insert the indentation string right there.
                    codeArea.insertText(caretPos, indent);

                    // Move the caret to the end of the newly inserted indentation
                    codeArea.moveTo(caretPos + indent.length());
                });
            }
        });

        // === AUTO-CLOSING BRACKETS AND PARENTHESES ===
        codeArea.setOnKeyTyped(event -> {
            String typed = event.getCharacter();
            int caretPos = codeArea.getCaretPosition();
            String closing = null;
            switch (typed) {
                case "(":
                    closing = ")";
                    break;
                case "{":
                    closing = "}";
                    break;
                case "[":
                    closing = "]";
                    break;
                case "\"":
                    String textBefore = codeArea.getText(0, caretPos);
                    long quoteCount = textBefore.chars().filter(ch -> ch == '"').count();
                    if (quoteCount % 2 == 1)
                        closing = "\"";
                    break;
                case "'":
                    String textBefore2 = codeArea.getText(0, caretPos);
                    long singleQuoteCount = textBefore2.chars().filter(ch -> ch == '\'').count();
                    if (singleQuoteCount % 2 == 1)
                        closing = "'";
                    break;
            }
            if (closing != null) {
                final String closingChar = closing;
                Platform.runLater(() -> {
                    int currentPos = codeArea.getCaretPosition();
                    codeArea.insertText(currentPos, closingChar);
                    codeArea.moveTo(currentPos);
                });
            }
        });

        // === DISABLE INTERNAL SCROLLING & MAKE CELL GROW ===
        // === FIX: Use the stable totalHeightEstimateProperty() listener ===
        codeArea.totalHeightEstimateProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {

                double contentHeight = newVal.doubleValue();

                // CRITICAL FIX: REDUCE THIS BUFFER.
                // 10 to 20 pixels is usually enough for visual comfort.
                double safetyMargin = 20; // Reduced from 70/100 to 20

                double calculatedHeight = contentHeight + safetyMargin;

                double newHeight = Math.max(100, calculatedHeight);

                // Apply the new, accurate height to lock the size
                codeArea.setPrefHeight(newHeight);
                codeArea.setMinHeight(newHeight);
                codeArea.setMaxHeight(newHeight);
            });
        });

        // === FIX SCROLLING ISSUE ===
        // Consume scroll events and pass to parent ScrollPane
        codeArea.addEventFilter(javafx.scene.input.ScrollEvent.ANY, event -> {
            // 1. Check for Vertical Scroll (to ensure horizontal still works)
            if (event.getDeltaY() != 0) {
                // 2. Consume the event so the CodeArea doesn't handle it
                event.consume();

                // 3. Re-fire the event on the CodeArea's parent (VBox root of the cell)
                // Let the JavaFX system bubble it up naturally to the main ScrollPane
                javafx.scene.Node parent = codeArea.getParent();
                if (parent != null) {
                    javafx.event.Event.fireEvent(parent, event.copyFor(parent, parent));
                }
            }
        });
    }

    private void moveCellUp() {
        int index = parentContainer.getChildren().indexOf(root);
        if (index <= 0) return; // already at top

        // swap in model
        Collections.swap(notebookController.getCurrentNotebook().getCells(), index, index - 1);
        // swap in ui
        parentContainer.getChildren().remove(root);
        parentContainer.getChildren().add(index - 1, root);
    }

    private void moveCellDown() {
        int index = parentContainer.getChildren().indexOf(root);
        if (index >= parentContainer.getChildren().size() - 1) return; // already at bottom

        // swap in model
        Collections.swap(notebookController.getCurrentNotebook().getCells(), index, index + 1);
        // swap in ui
        parentContainer.getChildren().remove(root);
        parentContainer.getChildren().add(index + 1, root);
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
            notebookController.getCurrentNotebook().removeCell(cellModel.getId());
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

    protected AtomicBoolean clearConfirmed = new AtomicBoolean();
    protected void confirmClear() {
        try {
            Alert alert = generateAlert();
            clearConfirmed.set(false);

            alert.setTitle("Clear Cell");
            alert.setHeaderText("Clear all text from this cell?");

            ButtonType yes = new ButtonType("Clear", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.showAndWait().ifPresent(response -> {
                if (response == yes) {
                    codeArea.clear();
                    clearConfirmed.set(true);
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
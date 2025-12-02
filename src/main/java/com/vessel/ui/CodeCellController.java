/*#TODO: Program cell growth logic*/

package com.vessel.ui;

import com.vessel.Kernel.ExecutionResult;
import com.vessel.Kernel.NotebookEngine;
import com.vessel.model.NotebookCell;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.regex.*;

import static com.vessel.util.SyntaxService.computeHighlighting;

public class CodeCellController extends GenericCellController {

    @FXML private VBox outputBox; // New outputbox -> JShell output goes in here
    @FXML private Button runBtn; // The button that toggles between Run/Cancel
    @FXML private VBox root; // This is the root of the cell

    private VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    private NotebookCell cellModel;
    private NotebookController notebookController;

    private NotebookEngine engine;

    /**
     * This is called by the NotebookController after loading the cell.
     */

    public void setParentContainer(VBox parent) {
        this.parentContainer = parent; // #TODO: move to superclass
    }

    // Field to hold the thread/task of the current execution
    private Thread executionThread = null;
    public void setNotebookController(NotebookController controller) {
        this.notebookController = controller;
    }

    // Field to hold the FontIcon for dynamic icon swapping
    private FontIcon runIcon;
    private FontIcon stopIcon;

    @Override
    public void setNotebookCell(NotebookCell cell) {
//        if (cell.getContent() != null && !cell.getContent().isBlank()) {
//            // Fill UI from whatever the model contains (e.g. on loading)
//            codeArea.replaceText(cell.getContent());
//        } // #TODO: Update in superclass
        super.setNotebookCell(cell);
        if (cell.getExecutionResult() != null && !cell.getContent().isBlank()) {
            displayOutput();
        }
    }

    public NotebookCell getNotebookCell() {
        return cellModel;
    }

    @FXML
    @Override
    protected void initialize() {
        // Initialize GenericCellController superclass first
        super.initialize();
        // Stop Icon: Define the icon for 'Cancel' (e.g., a square stop icon)
        // You'll need FontIcon imported: org.kordamp.ikonli.javafx.FontIcon
        runIcon = (FontIcon) runBtn.getGraphic();
        stopIcon = new FontIcon("fas-stop"); // Or use another relevant stop icon literal
        stopIcon.setIconSize(16);
        stopIcon.getStyleClass().add("font-icon"); // Apply the same style class as the run button

        runBtn.setOnAction(e -> toggleExecution());

        // Listener for syntax highlighting (Using richtext's richChanges() listener instead cuz more performant for syntax highlighting)
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))  // filter to only fire when text actually changes - ignores caret movement and stuff
                .subscribe(change -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        codeArea.setWrapText(false); // realized IDEs kinda have infinite horizontal space for long lines of code

        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("lineno");

            StackPane spacer = new StackPane(lineNo);
            spacer.getStyleClass().add("line-gutter");
            spacer.setAlignment(Pos.CENTER_RIGHT);

            return spacer;
        });

    }


    private void displayOutput(RotateTransition spin) {
        spin.stop();
        displayOutput();
    }

    // Overload for loading old outputs on loading existing file
    private void displayOutput() {
        outputBox.getChildren().clear();

        // explicit check for when loading a file
        if (!outputBox.isVisible()) {
            outputBox.setVisible(true);
        }

        // THIS IS WHERE YOUR JSHELL OUTPUT SHOULD GO!!!!
        // Currently just prints whatever is in the box back as output
        ExecutionResult shellResult = cellModel.getExecutionResult();

        if (!shellResult.success()) {
            Label err = new Label("Error:\n" + shellResult.error());
            err.setStyle("-fx-text-fill: #ff5555;");
            outputBox.getChildren().add(err);
        }
        else if (shellResult.output().trim().isEmpty()) {
            Label noOutputLabel = new Label("(No output to print)");
            noOutputLabel.setStyle("-fx-text-fill: #888a99; -fx-font-size: 15px; -fx-font-family: 'Fira Mono', 'Consolas', monospace;");
            outputBox.getChildren().add(noOutputLabel);
            fadeIn(noOutputLabel);
        } else {
            TextArea resultArea = new TextArea(shellResult.output().trim());
            resultArea.getStyleClass().add("read-only-output");
            resultArea.setEditable(false);
            resultArea.setWrapText(true);
            resultArea.setFocusTraversable(false);
            resultArea.setMaxWidth(1000);

            // MIGHT NEED SLIGHT FIXING LATER: Auto-resizes resultArea by line count
            int lineCount = resultArea.getText().split("\n", -1).length;
            resultArea.setPrefRowCount(Math.max(1, lineCount));
            resultArea.setWrapText(true);
            Platform.runLater(() -> adjustOutputAreaHeight(resultArea));
            resultArea.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> adjustOutputAreaHeight(resultArea)));
            outputBox.getChildren().add(resultArea);
            fadeIn(resultArea);
        }
        outputBox.setPrefHeight(-1); // reset container sizing
    }

    private void deleteCell() {
        if (parentContainer != null && root != null) {
            parentContainer.getChildren().remove(root);
        }
        // TODO: Move to superclass
        if (cellModel != null) {
            // also remove from notebook model
            notebookController.getNotebook().removeCell(cellModel.getId());
        }
    }


    private void adjustOutputAreaHeight(TextArea area) {
        Text helper = new Text();
        helper.setFont(area.getFont());
        helper.setWrappingWidth(area.getWidth() - 10); // -10 fudge for padding/border
        String text = area.getText();
        if (text == null || text.isEmpty()) text = " ";

        // Line height for dynamic fudge:
        helper.setText("Ay");
        double lineHeight = helper.getLayoutBounds().getHeight();

        // Full wrapped content height:
        helper.setText(text);
        double textHeight = helper.getLayoutBounds().getHeight();
        area.setPrefHeight(Math.max(lineHeight * 2, textHeight + lineHeight * 1.25));
    }

    // fancy fade animation cuz why not
    private void fadeIn(Region node) {
        node.setOpacity(0);
        node.applyCss();
        node.layout();
        Timeline fade = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(node.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(400), new KeyValue(node.opacityProperty(), 1))
        );
        fade.play();
    }

    private void executeCode() {
        outputBox.setVisible(true);
        outputBox.getChildren().clear();

        // --- Increment execution count at start ---
        incrementAndDisplayExecutionCount();
        cellModel.incrementExecutionCount();
        // 1. Change UI state to CANCEL
        setRunButtonState(true);

        // Spinner
        HBox spinnerBox = new HBox(8);
        FontIcon spinnerIcon = new FontIcon("fas-spinner");
        spinnerIcon.getStyleClass().add("output-spinner");
        RotateTransition spin = new RotateTransition(Duration.seconds(1), spinnerIcon);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.play();
        Label loadingText = new Label("Executing...");
        loadingText.setStyle("-fx-text-fill: #d4d4d4; -fx-font-size: 14px;");
        spinnerBox.getChildren().addAll(spinnerIcon, loadingText);
        outputBox.getChildren().add(spinnerBox);
        fadeIn(spinnerBox);



        // Execution thread
        executionThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Simulate code execution
                // --------------------------------------------------

                if (!Thread.currentThread().isInterrupted()) {
                    Platform.runLater(() -> {
                        displayOutput(spin);
                        setRunButtonState(false);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    outputBox.getChildren().clear();
                    Label cancelledLabel = new Label("[Execution Cancelled]");
                    cancelledLabel.getStyleClass().add("execution-cancelled");
                    outputBox.getChildren().add(cancelledLabel);
                    setRunButtonState(false);
                });
            }
        });
        executionThread.start();
    }
    private void toggleExecution() {
        if (executionThread != null && executionThread.isAlive()) {
            // If a thread is running, CANCEL it
            executionThread.interrupt();
            executionThread = null;
        } else {
            // If no thread is running, START execution
            executeCode();
        }
    }
    public void setEngine(NotebookEngine engine) {
        this.engine = engine;
    }

}

    /**
     * Updates the run button's icon and tooltip based on the running state.
     * @param isRunning True if execution is starting, False if it has finished or been cancelled.
     */
    private void setRunButtonState(boolean isRunning) {
        if (isRunning) {
            runBtn.setGraphic(stopIcon);
            runBtn.setTooltip(new Tooltip("Cancel Execution"));
        } else {
            runBtn.setGraphic(runIcon);
            runBtn.setTooltip(new Tooltip("Run this cell"));
            executionThread = null; // Ensure thread reference is cleared
        }
    }
}
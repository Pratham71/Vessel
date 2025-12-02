/*#TODO: Program cell growth logic*/

package com.vessel.ui;

import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.concurrent.Task;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.fxmisc.richtext.CodeArea;
import java.util.regex.*;

import static com.vessel.util.SyntaxService.computeHighlighting;

public class CodeCellController {
    @FXML private ChoiceBox<CellType> cellLanguage;
    @FXML private Button runBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private CodeArea codeArea;
    @FXML private VBox outputBox; // New outputbox -> JShell output goes in here
    @FXML private VBox root; // This is the root of the cell

    private VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    private NotebookCell cellModel;
    private NotebookController notebookController;


    /**
     * This is called by the NotebookController after loading the cell.
     */

    public void setParentContainer(VBox parent) {
        this.parentContainer = parent;
    }

    public void setRoot(VBox root) {
        this.root = root;
    }

    public void setNotebookController(NotebookController controller) {
        this.notebookController = controller;
    }


    // links this ui cell with its notebookcell model and loads initial content
    public void setNotebookCell(NotebookCell cell) {
        this.cellModel = cell;

        // Only set default text if the cell is empty (not re-loading output)
        if (cell.getContent() == null || cell.getContent().isBlank()) {
            String initText = "// Enter Java code here\nSystem.out.println(\"Hello, world!\");";

            // Set both UI and model content
            codeArea.replaceText(initText);
            cell.setContent(initText);
        } else {
            // Fill UI from whatever the model contains (e.g. on loading)
            codeArea.replaceText(cell.getContent());
        }
        cellLanguage.setValue(cell.getType());
    }

    public NotebookCell getNotebookCell() {
        return cellModel;
    }

    @FXML
    private void initialize() {
        // prevents NullPointer crashes
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);

        // EL PROBLEMO - this new RichTextFX CodeArea doesnt have prompt text support
        // cellLanguage.setOnAction(e -> codeArea.setPromptText(getPromptForType(cellLanguage.getValue())));
        // codeArea.setPromptText(getPromptForType(cellLanguage.getValue()));

        // Listener for syntax highlighting (Using richtext's richChanges() listener instead cuz more performant for syntax highlighting)
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))  // filter to only fire when text actually changes - ignores caret movement and stuff
                .subscribe(change -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) cellModel.setContent(newText);
        });

        // Listener for setting cell model's "type" on type change (in the dropbox)
        cellLanguage.setOnAction(e -> {
            if (cellModel != null) cellModel.setType(cellLanguage.getValue());
        });

        codeArea.setWrapText(false); // realized IDEs kinda have infinite horizontal space for long lines of code


        // --- BUTTON HANDLERS ---

        runBtn.setOnAction(e -> runCell());

        deleteBtn.setOnAction(e -> deleteCell());

        clearBtn.setOnAction(e -> codeArea.clear());
    }


    private void runCell() {
        outputBox.setVisible(true);
        outputBox.getChildren().clear();

        // --- Increment execution count ---
        cellModel.incrementExecutionCount();

        // --- Add spinner ---
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
        outputBox.applyCss();
        outputBox.layout();
        fadeIn(spinnerBox);

        // --- Simulate background execution ---
        Task<Void> fakeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1000); // Simulate work, replace with JShell code later
                return null;
            }
        };

        fakeTask.setOnSucceeded(ev -> {
            displayOutput(spin);
        });
        new Thread(fakeTask).start();
        cellModel.dumpContent(); // temp debug print
    }

    private void displayOutput(RotateTransition spin) {
        spin.stop();
        outputBox.getChildren().clear();

        // THIS IS WHERE YOUR JSHELL OUTPUT SHOULD GO!!!!
        // Currently just prints whatever is in the box back as output
        String shellOutput = codeArea.getText().isEmpty() ? "" : "Run clicked for " + cellLanguage.getValue() + ":\n" + codeArea.getText();

        if (shellOutput.trim().isEmpty()) {
            Label noOutputLabel = new Label("(No output to print)");
            noOutputLabel.setStyle("-fx-text-fill: #888a99; -fx-font-size: 15px; -fx-font-family: 'Fira Mono', 'Consolas', monospace;");
            outputBox.getChildren().add(noOutputLabel);
            fadeIn(noOutputLabel);
        } else {
            TextArea resultArea = new TextArea(shellOutput.trim());
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

    public void setCellType(CellType type) {
        cellLanguage.setValue(type);
    }

    private String getPromptForType(String type) {
        return switch (type) {
            case "Java Code" -> "Enter Java code here...";
            case "Markdown" -> "Enter Markdown content...";
            case "Plain Text" -> "Enter plain text...";
            default -> "";
        };
    }
}

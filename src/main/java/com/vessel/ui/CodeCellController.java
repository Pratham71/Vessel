/*#TODO: Program cell growth logic*/

package com.vessel.ui;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Pos;
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

import static com.vessel.util.SyntaxService.computeHighlighting;

public class CodeCellController extends GenericCellController {
    @FXML private Button runBtn;
    @FXML private VBox outputBox; // New outputbox -> JShell output goes in here

    @FXML
    @Override
    protected void initialize() {
        // Initialize GenericCellController superclass first
        super.initialize();

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

        // --- BUTTON HANDLERS ---

        runBtn.setOnAction(e -> runCell());
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

    private String getPromptForType(String type) {
        return switch (type) {
            case "Java Code" -> "Enter Java code here...";
            case "Markdown" -> "Enter Markdown content...";
            case "Plain Text" -> "Enter plain text...";
            default -> "";
        };
    }
}
/*#TODO: Migrate syntax highlighting to helper class/file (looks v messy atm)*/

package com.vessel.frontendhelpers;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class CodeCellController {
    @FXML private ChoiceBox<CellType> cellLanguage;
    @FXML private Button runBtn;
    @FXML private Button deleteBtn;
    @FXML private Button clearBtn;
    @FXML private CodeArea codeArea;
    @FXML private VBox outputBox; // New outputbox -> JShell output goes in here
    @FXML private VBox root; // This is the root of the cell

    private VBox parentContainer; // The notebook VBox (set by UIController on creation)
    private NotebookCell cellModel;

    // Execution state for this cell
    private volatile Task<Void> currentTask = null;
    private volatile boolean isRunning = false;

    // spinner references (kept here so we can stop/remove them on cancel)
    private HBox spinnerBox = null;
    private RotateTransition spinnerRotate = null;

    /**
     * This is called by the UIController after loading the cell.
     */
    public void setParentContainer(VBox parent) {
        this.parentContainer = parent;
    }

    public void setRoot(VBox root) {
        this.root = root;
    }

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

    // Syntax highlighting patterns
    private static final String[] JAVA_KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default",
            "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "void", "volatile", "while"
    };
    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", JAVA_KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String STRING_PATTERN = "\"([^\\\"\\\\]|\\\\.)*\"";
    private static final String CHAR_PATTERN   = "'(?:[^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "//[^\\n]*|/\\*.*?\\*/";
    private static final String NUMBER_PATTERN = "\\b[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?[fFdD]?\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<CHAR>" + CHAR_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")",
            Pattern.DOTALL
    );

    @FXML
    private void initialize() {
        // prevents NullPointer crashes
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);

        // Listener for syntax highlighting
        codeArea.textProperty().addListener((obs, oldText, newText) ->
                codeArea.setStyleSpans(0, computeHighlighting(newText))
        );

        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) cellModel.setContent(newText);
        });

        // Listener for setting cell model's "type" on type change (in the dropbox)
        cellLanguage.setOnAction(e -> {
            if (cellModel != null) cellModel.setType(cellLanguage.getValue());
        });

        codeArea.setWrapText(false); // realized IDEs kinda have infinite horizontal space for long lines of code

        // -------------------- Button handlers --------------------
        runBtn.setOnAction(e -> runCellSimulation());

        // DELETE BUTTON - now shows warning before deleting
        deleteBtn.setOnAction(e -> {
            if (parentContainer != null && root != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Warning");
                confirm.setHeaderText(null);
                confirm.setContentText("Are you sure you want to delete this cell?");
                ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
                confirm.getButtonTypes().setAll(yes, no);

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == yes) {
                    parentContainer.getChildren().remove(root);
                }
            }
        });

        // CLEAR BUTTON - shows confirmation before clearing
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Warning");
            confirm.setHeaderText(null);
            confirm.setContentText("Are you sure you want clear this cell?");
            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(yes, no);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == yes) {
                clearContentWithoutConfirm();
            }
        });
    }

    private void runCellSimulation() {
        // If already running, treat this click as "pause"
        if (isRunning) {
            if (currentTask != null) currentTask.cancel(true);
            return;
        }

        outputBox.setVisible(true);
        outputBox.getChildren().clear();

        if (cellModel != null) cellModel.incrementExecutionCount();

        spinnerBox = new HBox(8);
        FontIcon spinnerIcon = new FontIcon("fas-spinner");
        spinnerIcon.getStyleClass().add("output-spinner");
        spinnerRotate = new RotateTransition(Duration.seconds(1), spinnerIcon);
        spinnerRotate.setByAngle(360);
        spinnerRotate.setCycleCount(RotateTransition.INDEFINITE);
        spinnerRotate.play();
        Label loadingText = new Label("Executing...");
        loadingText.setStyle("-fx-text-fill: #d4d4d4; -fx-font-size: 14px;");
        spinnerBox.getChildren().addAll(spinnerIcon, loadingText);
        outputBox.getChildren().add(spinnerBox);
        fadeIn(spinnerBox);

        isRunning = true;
        if (runBtn.getGraphic() instanceof FontIcon runIcon) runIcon.setIconLiteral("fas-stop");
        else runBtn.setGraphic(new FontIcon("fas-stop"));
        if (runBtn.getTooltip() == null) runBtn.setTooltip(new Tooltip("stop"));
        else runBtn.getTooltip().setText("stop");

        Task<Void> fakeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int steps = 20;
                for (int i = 0; i < steps; i++) {
                    if (isCancelled()) break;
                    Thread.sleep(50);
                }
                return null;
            }
        };

        currentTask = fakeTask;

        fakeTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null) spinnerRotate.stop();
            outputBox.getChildren().clear();

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
                int lineCount = resultArea.getText().split("\n", -1).length;
                resultArea.setPrefRowCount(Math.max(1, lineCount));
                Platform.runLater(() -> adjustOutputAreaHeight(resultArea));
                resultArea.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> adjustOutputAreaHeight(resultArea)));
                outputBox.getChildren().add(resultArea);
                fadeIn(resultArea);
            }
            finishExecutionCleanup();
        }));

        fakeTask.setOnCancelled(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null) spinnerRotate.stop();
            outputBox.getChildren().clear();
            Label cancelledLabel = new Label("(Execution cancelled)");
            cancelledLabel.setStyle("-fx-text-fill: #ffb86b; -fx-font-size: 14px;");
            outputBox.getChildren().add(cancelledLabel);
            fadeIn(cancelledLabel);
            finishExecutionCleanup();
        }));

        fakeTask.setOnFailed(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null) spinnerRotate.stop();
            outputBox.getChildren().clear();
            Label failLabel = new Label("(Execution failed)");
            failLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
            outputBox.getChildren().add(failLabel);
            fadeIn(failLabel);
            finishExecutionCleanup();
        }));

        new Thread(fakeTask).start();
        if (cellModel != null) cellModel.dumpContent();
    }

    /**
     * Called internally after execution completes/cancels/fails to restore UI state.
     */
    private void finishExecutionCleanup() {
        isRunning = false;
        currentTask = null;

        if (spinnerRotate != null) spinnerRotate.stop();
        if (spinnerBox != null && outputBox.getChildren().contains(spinnerBox)) outputBox.getChildren().remove(spinnerBox);

        Platform.runLater(() -> {
            if (runBtn != null) {
                if (runBtn.getGraphic() instanceof FontIcon runIcon) runIcon.setIconLiteral("fas-play");
                else runBtn.setGraphic(new FontIcon("fas-play"));
                if (runBtn.getTooltip() == null) runBtn.setTooltip(new Tooltip("Run this cell"));
                else runBtn.getTooltip().setText("Run this cell");
            }
        });
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text.isEmpty()) {
            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            spansBuilder.add(Collections.singleton("plain"), 0);
            return spansBuilder.create();
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN")    != null ? "paren"    :
                                    matcher.group("BRACE")    != null ? "brace"    :
                                            matcher.group("BRACKET")  != null ? "bracket"  :
                                                    matcher.group("SEMICOLON")!= null ? "semicolon":
                                                            matcher.group("STRING")   != null ? "string"   :
                                                                    matcher.group("CHAR")     != null ? "char"     :
                                                                            matcher.group("COMMENT")  != null ? "comment"  :
                                                                                    matcher.group("NUMBER")   != null ? "number"   :
                                                                                            "plain";
            if (matcher.start() > lastKwEnd) spansBuilder.add(Collections.singleton("plain"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        if (lastKwEnd < text.length()) spansBuilder.add(Collections.singleton("plain"), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private void adjustOutputAreaHeight(TextArea area) {
        Text helper = new Text();
        helper.setFont(area.getFont());
        helper.setWrappingWidth(area.getWidth() - 10);
        String text = area.getText();
        if (text == null || text.isEmpty()) text = " ";
        helper.setText("Ay");
        double lineHeight = helper.getLayoutBounds().getHeight();
        helper.setText(text);
        double textHeight = helper.getLayoutBounds().getHeight();
        area.setPrefHeight(Math.max(lineHeight * 2, textHeight + lineHeight * 1.25));
    }

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

    // Helper getters used by UIController if needed
    public boolean isRunning() { return isRunning; }
    public Button getRunButton() { return runBtn; }
    public void clearOutput() {
        if (outputBox != null) {
            outputBox.getChildren().clear();
            outputBox.setVisible(false);
        }
    }
    public void clearContentWithoutConfirm() {
        codeArea.clear();
        if (cellModel != null) cellModel.setContent("");
        clearOutput();
    }
}

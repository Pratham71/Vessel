/*#TODO: Migrate syntax highlighting to helper class/file (looks v messy atm)*/

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
import java.util.Collection;
import java.util.Collections;

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

    private VBox parentContainer; // The notebook VBox (set by NotebookController on creation)
    private NotebookCell cellModel;

    /**
     * This is called by the NotebookController after loading the cell.
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


    private static final String[] JAVA_KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
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

//        EL PROBLEMO - this new RichTextFX CodeArea doesnt have prompt text support
//        cellLanguage.setOnAction(e -> codeArea.setPromptText(getPromptForType(cellLanguage.getValue())));
//        codeArea.setPromptText(getPromptForType(cellLanguage.getValue()));

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

//        codeArea.getParagraphs().addListener((ListChangeListener<? super Object>) change -> {
//            Platform.runLater(() -> {
//                var flowNode = codeArea.lookup(".virtual-flow");
//                if (flowNode instanceof Region flow) {
//                    double fudge = 8;
//                    codeArea.setPrefHeight(flow.getHeight() + fudge);
//                }
//            });
//        });
// --- Comment out the initial grow on startup (in Platform.runLater):
// Platform.runLater(() -> {
//     var flowNode = codeArea.lookup(".virtual-flow");
//     if (flowNode instanceof Region flow) {
//         double fudge = 8;
//         codeArea.setPrefHeight(flow.getHeight() + fudge);
//     }
// });

        codeArea.setWrapText(false); // realized IDEs kinda have infinite horizontal space for long lines of code


        // Button handlers

        runBtn.setOnAction(e -> {
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
                spin.stop();
                outputBox.getChildren().clear();

//              THIS IS WHERE YOUR JSHELL OUTPUT SHOULD GO!!!!
//              Currently just prints whatever is in the box as output
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
                    // Auto-resize resultArea by line count
                    int lineCount = resultArea.getText().split("\n", -1).length;
                    resultArea.setPrefRowCount(Math.max(1, lineCount));
                    resultArea.setWrapText(true);
                    Platform.runLater(() -> adjustOutputAreaHeight(resultArea));
                    resultArea.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> adjustOutputAreaHeight(resultArea)));
                    outputBox.getChildren().add(resultArea);
                    fadeIn(resultArea);
                }
                outputBox.setPrefHeight(-1); // reset container sizing
            });
            new Thread(fakeTask).start();
            cellModel.dumpContent(); // temp debug print
        });

            deleteBtn.setOnAction(e -> {
                if (parentContainer != null && root != null) {
                    parentContainer.getChildren().remove(root);
                }
            });
            clearBtn.setOnAction(e -> codeArea.clear());
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
                                                                                            "plain"; // For any matched but uncategorized case (very rare)
            // Add unstyled/plain segment before this match
            if (matcher.start() > lastKwEnd) {
                spansBuilder.add(Collections.singleton("plain"), matcher.start() - lastKwEnd);
            }
            // Add the highlighted segment
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        // Add any remaining plain text at the end
        if (lastKwEnd < text.length()) {
            spansBuilder.add(Collections.singleton("plain"), text.length() - lastKwEnd);
        }
        return spansBuilder.create();
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

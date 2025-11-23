/* 
* TODO: Migrate syntax highlighting to helper class/file (looks v messy atm)
 * CodeCellController.java
 * This controller manages the individual code cells in the notebook.
 * It handles user interactions, code execution simulation, syntax highlighting,
 * and UI updates for each cell.
 */

package com.vessel.frontendhelpers; // Package declaration for frontend helper classes

// Import necessary model classes
import com.vessel.model.CellType; // Enum for cell types (CODE, MARKDOWN, etc.)
import com.vessel.model.NotebookCell; // Model representing a single notebook cell

// Import JavaFX collections for managing lists in UI components
import javafx.collections.FXCollections;

// Import JavaFX FXML annotation for linking with .fxml files
import javafx.fxml.FXML;

// Import JavaFX application thread utilities
import javafx.application.Platform;

// Import JavaFX UI controls
import javafx.scene.control.*;

// Import JavaFX layout containers
import javafx.scene.layout.*;

// Import JavaFX text nodes
import javafx.scene.text.Text;

// Import JavaFX concurrency utilities for background tasks
import javafx.concurrent.Task;

// Import JavaFX animation classes for UI effects
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

// Import Ikonli for font icons (e.g., FontAwesome)
import org.kordamp.ikonli.javafx.FontIcon;

// Import RichTextFX CodeArea for syntax highlighting editor
import org.fxmisc.richtext.CodeArea;

// Import Java regex classes for parsing code for highlighting
import java.util.regex.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

// Import RichTextFX model classes for styling text
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public class CodeCellController {

    // --- FXML Injected UI Components ---

    @FXML
    private ChoiceBox<CellType> cellLanguage;
    @FXML
    private Button runBtn; // Button to execute the code in the cell
    @FXML
    private Button deleteBtn; // Button to delete this cell from the notebook
    @FXML
    private Button clearBtn; // Button to clear the content of this cell
    @FXML
    private CodeArea codeArea; // The main text editor area with syntax highlighting support
    @FXML
    private VBox outputBox; // Container to display the output of the code execution
    @FXML
    private VBox root; // The root container of this cell's UI
    @FXML
    private Label executionCountLabel; // Label showing execution count like "In [5]:"
    @FXML
    private Region activeIndicator; // Vertical bar indicating active/focused cell
    @FXML
    private Region leftBlueBar; // Left vertical blue bar that expands with cell

    // Reference to the outer HBox container
    private HBox cellRoot;

    private CellType cellType;

    // --- Internal State Variables ---

    private VBox parentContainer; // Reference to the parent container (Notebook's main VBox)
    private NotebookCell cellModel; // The data model backing this UI cell

    // Execution state tracking
    private volatile Task<Void> currentTask = null; // The currently running background task (if any)
    private volatile boolean isRunning = false; // Flag to indicate if code is currently executing

    // UI references for the loading spinner
    private HBox spinnerBox = null; // Container for the spinner icon and text
    private RotateTransition spinnerRotate = null; // Animation for the spinner rotation

    // Execution time tracking
    private long executionStartTime = 0; // Timestamp when execution started
    private Label executionTimeLabel = null; // Label showing execution time

    /**
     * Sets the parent container for this cell.
     * Used to allow the cell to remove itself from the notebook.
     * 
     * @param parent The VBox containing all cells.
     */
    public void setParentContainer(VBox parent) {
        this.parentContainer = parent; // Store the parent reference
    }

    /**
     * Sets the cell root (outer HBox container).
     * Used when removing the cell from the parent.
     * 
     * @param cellRoot The root HBox of this cell.
     */
    public void setCellRoot(HBox cellRoot) {
        this.cellRoot = cellRoot; // Store the outer HBox reference
    }

    /**
     * Links this controller to a data model (NotebookCell).
     * Populates the UI with data from the model.
     * 
     * @param cell The NotebookCell model.
     */
    public void setNotebookCell(NotebookCell cell) {
        this.cellModel = cell; // Store the model reference

        // Check if the cell content is empty or null
        if (cell.getContent() == null || cell.getContent().isBlank()) {
            // Default text to show for a new Java cell
            String initText = "// Enter Java code here\nSystem.out.println(\"Hello, world!\");";

            // Update the UI CodeArea with default text
            codeArea.replaceText(initText);
            // Update the model with default text
            cell.setContent(initText);
        } else {
            // If model has content (e.g., loading from file), populate the UI with it
            codeArea.replaceText(cell.getContent());
        }
        // Set the dropdown value to match the cell type from the model
        cellLanguage.setValue(cell.getType());
    }

    // --- Syntax Highlighting Configuration ---

    // Array of Java keywords to highlight
    private static final String[] JAVA_KEYWORDS = new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default",
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
    private static final String CHAR_PATTERN = "'(?:[^'\\\\]|\\\\.)*'";
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
            Pattern.DOTALL);

    /**
     * JavaFX Initialization method.
     * Called automatically after the FXML file is loaded.
     */
    @FXML
    private void initialize() {
        // Initialize the language dropdown with values from the CellType enum
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values()));
        // Set default value to CODE
        cellLanguage.setValue(CellType.CODE);

        // EL PROBLEMO - this new RichTextFX CodeArea doesnt have prompt text support
        // cellLanguage.setOnAction(e ->
        // codeArea.setPromptText(getPromptForType(cellLanguage.getValue())));
        // codeArea.setPromptText(getPromptForType(cellLanguage.getValue()));

        // Listener for syntax highlighting
        codeArea.textProperty()
                .addListener((obs, oldText, newText) -> codeArea.setStyleSpans(0, computeHighlighting(newText)));

        // Listener for updating cell model's content field
        codeArea.textProperty().addListener((obs, old, newText) -> {
            if (cellModel != null) // Check if model exists
                cellModel.setContent(newText); // Update model content
        });

        // Add listener to sync language dropdown changes back to the model
        cellLanguage.setOnAction(e -> {
            if (cellModel != null) // Check if model exists
                cellModel.setType(cellLanguage.getValue()); // Update model type
        });
        // codeArea.getParagraphs().addListener((ListChangeListener<? super Object>)
        // change -> {
        // Platform.runLater(() -> {
        // var flowNode = codeArea.lookup(".virtual-flow");
        // if (flowNode instanceof Region flow) {
        // double fudge = 8;
        // codeArea.setPrefHeight(flow.getHeight() + fudge);
        // }
        // });
        // });
        // --- Comment out the initial grow on startup (in Platform.runLater):
        // Platform.runLater(() -> {
        // var flowNode = codeArea.lookup(".virtual-flow");
        // if (flowNode instanceof Region flow) {
        // double fudge = 8;
        // codeArea.setPrefHeight(flow.getHeight() + fudge);
        // }
        // Disable text wrapping for code editor behavior (horizontal scroll preferred)
        codeArea.setWrapText(false);

        // Dynamic height adjustment - expand cell as content grows
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                // Calculate height based on number of lines
                int lineCount = newText.split("\n", -1).length;
                double lineHeight = 20; // Approximate line height in pixels
                double padding = 20; // Top and bottom padding
                double minHeight = 120; // Minimum height
                double calculatedHeight = Math.max(minHeight, (lineCount * lineHeight) + padding);
                codeArea.setPrefHeight(calculatedHeight);
            });
        });

        // Auto-indentation on Enter key
        codeArea.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                int caretPos = codeArea.getCaretPosition();
                String text = codeArea.getText();

                // Find the start of the current line
                int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;

                // Count leading spaces/tabs on current line
                final int[] indentCount = { 0 };
                for (int i = lineStart; i < caretPos && i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (c == ' ' || c == '\t') {
                        indentCount[0]++;
                    } else {
                        break;
                    }
                }

                // Check if previous character is an opening bracket
                final boolean afterOpenBracket;
                if (caretPos > 0) {
                    char prevChar = text.charAt(caretPos - 1);
                    afterOpenBracket = (prevChar == '{' || prevChar == '(' || prevChar == '[');
                } else {
                    afterOpenBracket = false;
                }

                // Insert newline with indentation
                Platform.runLater(() -> {
                    String indentStr = " ".repeat(indentCount[0]);
                    if (afterOpenBracket) {
                        indentStr += "    "; // Add 4 spaces for new block
                    }
                    codeArea.insertText(caretPos, "\n" + indentStr);
                });

                event.consume(); // Prevent default Enter behavior
            }
        });

        // Add focus listener to show active indicator and border
        codeArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                // Show blue indicator when cell is active
                activeIndicator.setStyle(
                        "-fx-min-width: 0px; -fx-pref-width: 0px; -fx-min-height: 0px; -fx-background-color: #ffb86b;");
                // Show left blue bar
                leftBlueBar.setStyle("-fx-min-width: 4px; -fx-pref-width: 4px; -fx-background-color: #ffb86b;");
                // Add active border to cell
                if (cellRoot != null) {
                    cellRoot.getStyleClass().remove("code-cell-inactive");
                    cellRoot.getStyleClass().add("code-cell-active");
                }
            } else {
                // Hide indicator when cell loses focus
                activeIndicator.setStyle(
                        "-fx-min-width: 4px; -fx-pref-width: 4px; -fx-min-height: 20px; -fx-background-color: transparent;");
                // Hide left blue bar
                leftBlueBar.setStyle("-fx-min-width: 4px; -fx-pref-width: 4px; -fx-background-color: transparent;");
                // Remove active border from cell
                if (cellRoot != null) {
                    cellRoot.getStyleClass().remove("code-cell-active");
                    cellRoot.getStyleClass().add("code-cell-inactive");
                }
            }
        });

        // Update execution count label initially
        updateExecutionCountLabel();

        // --- Button Event Handlers ---

        // Run Button: Triggers execution simulation
        runBtn.setOnAction(e -> {
            // If already running, treat this click as "Stop/Cancel"
            if (isRunning) {
                if (currentTask != null) {
                    currentTask.cancel(true);
                }
                return;
            }

            outputBox.setVisible(true);
            outputBox.getChildren().clear();

            // --- Increment execution count ---
            if (cellModel != null)
                cellModel.incrementExecutionCount();
            updateExecutionCountLabel();

            // Start execution timer
            executionStartTime = System.currentTimeMillis();

            // --- Add spinner ---
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
            outputBox.applyCss();
            outputBox.layout();
            fadeIn(spinnerBox);

            // Update button to show "Stop" icon
            isRunning = true;
            if (runBtn.getGraphic() instanceof FontIcon runIcon)
                runIcon.setIconLiteral("fas-stop");
            else
                runBtn.setGraphic(new FontIcon("fas-stop"));
            if (runBtn.getTooltip() == null)
                runBtn.setTooltip(new Tooltip("Stop"));
            else
                runBtn.getTooltip().setText("Stop");

            // --- Simulate background execution ---
            Task<Void> fakeTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    int steps = 20;
                    for (int i = 0; i < steps; i++) {
                        if (isCancelled())
                            break;
                        Thread.sleep(50); // Simulate work, replace with JShell code later
                    }
                    return null;
                }
            };

            currentTask = fakeTask;

            fakeTask.setOnSucceeded(ev -> Platform.runLater(() -> {
                if (spinnerRotate != null)
                    spinnerRotate.stop();
                outputBox.getChildren().clear();

                // THIS IS WHERE YOUR JSHELL OUTPUT SHOULD GO!!!!
                // Currently just prints whatever is in the box as output
                String shellOutput = codeArea.getText().isEmpty() ? ""
                        : "Run clicked for " + cellLanguage.getValue() + ":\n" + codeArea.getText();

                if (shellOutput.trim().isEmpty()) {
                    Label noOutputLabel = new Label("(No output to print)");
                    noOutputLabel.setStyle(
                            "-fx-text-fill: #888a99; -fx-font-size: 15px; -fx-font-family: 'Fira Mono', 'Consolas', monospace;");
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
                    resultArea.widthProperty()
                            .addListener((obs, o, n) -> Platform.runLater(() -> adjustOutputAreaHeight(resultArea)));
                    outputBox.getChildren().add(resultArea);
                    fadeIn(resultArea);
                }

                // Add execution time label
                addExecutionTimeLabel();

                outputBox.setPrefHeight(-1); // reset container sizing
                finishExecutionCleanup();
            }));

            // On Cancellation
            fakeTask.setOnCancelled(ev -> Platform.runLater(() -> {
                if (spinnerRotate != null)
                    spinnerRotate.stop(); // Stop spinner
                outputBox.getChildren().clear(); // Clear spinner
                // Show cancellation message
                Label cancelledLabel = new Label("(Execution cancelled)");
                cancelledLabel.setStyle("-fx-text-fill: #ffb86b; -fx-font-size: 14px;");
                outputBox.getChildren().add(cancelledLabel);
                fadeIn(cancelledLabel);
                addExecutionTimeLabel(); // Show time even when cancelled
                finishExecutionCleanup(); // Reset UI state
            }));

            // On Failure
            fakeTask.setOnFailed(ev -> Platform.runLater(() -> {
                if (spinnerRotate != null)
                    spinnerRotate.stop();
                outputBox.getChildren().clear();
                Label failLabel = new Label("(Execution failed)");
                failLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
                outputBox.getChildren().add(failLabel);
                fadeIn(failLabel);
                finishExecutionCleanup();
            }));

            new Thread(fakeTask).start();
            if (cellModel != null)
                cellModel.dumpContent(); // temp debug print
        });

        // Delete Button: Shows confirmation dialog before removing the cell
        deleteBtn.setOnAction(e -> {
            // Ensure we have references to parent and cellRoot
            if (parentContainer != null && cellRoot != null) {
                // Create confirmation alert
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                // Set owner to main window for centering
                confirm.initOwner(root.getScene().getWindow());
                confirm.setTitle("Warning"); // Set title
                confirm.setHeaderText(null); // No header
                confirm.setContentText("Are you sure you want to delete this cell?"); // Warning message

                // Define buttons
                ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
                confirm.getButtonTypes().setAll(yes, no); // Set buttons on alert

                // Show alert and wait for user response
                Optional<ButtonType> result = confirm.showAndWait();
                // If user clicked Yes
                if (result.isPresent() && result.get() == yes) {
                    // Remove this cell's cellRoot (outer HBox) from the parent container
                    parentContainer.getChildren().remove(cellRoot);
                }
            }
        });

        // Clear Button: Shows confirmation dialog before clearing content
        clearBtn.setOnAction(e -> {
            // Create confirmation alert
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            // Set owner to main window for centering
            confirm.initOwner(root.getScene().getWindow());
            confirm.setTitle("Warning"); // Set title
            confirm.setHeaderText(null); // No header
            confirm.setContentText("Are you sure you want clear this cell?"); // Warning message

            // Define buttons
            ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType no = new ButtonType("No", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(yes, no); // Set buttons on alert

            // Show alert and wait for user response
            Optional<ButtonType> result = confirm.showAndWait();
            // If user clicked Yes
            if (result.isPresent() && result.get() == yes) {
                // Perform clear action
                clearContentWithoutConfirm();
            }
        });
    }

    /**
     * Simulates the execution of code in the cell.
     * Handles UI state changes (loading spinner, button toggle) and background
     * task.
     */
    private void runCellSimulation() {
        // Check if code is already running
        if (isRunning) {
            // If running, this click acts as a "Stop" command
            if (currentTask != null)
                currentTask.cancel(true); // Cancel the background task
            return; // Exit method
        }

        // Prepare output area for new execution
        outputBox.setVisible(true); // Make output box visible
        outputBox.getChildren().clear(); // Clear previous output

        // Increment execution count in the model
        if (cellModel != null)
            cellModel.incrementExecutionCount();

        // --- Create and Display Loading Spinner ---
        spinnerBox = new HBox(8); // Horizontal box for spinner and text
        FontIcon spinnerIcon = new FontIcon("fas-spinner"); // Spinner icon
        spinnerIcon.getStyleClass().add("output-spinner"); // Add CSS class

        // Create rotation animation for the spinner
        spinnerRotate = new RotateTransition(Duration.seconds(1), spinnerIcon);
        spinnerRotate.setByAngle(360); // Rotate 360 degrees
        spinnerRotate.setCycleCount(RotateTransition.INDEFINITE); // Loop forever
        spinnerRotate.play(); // Start animation

        // Create "Executing..." label
        Label loadingText = new Label("Executing...");
        loadingText.setStyle("-fx-text-fill: #d4d4d4; -fx-font-size: 14px;"); // Inline style for label

        // Add icon and text to the box
        spinnerBox.getChildren().addAll(spinnerIcon, loadingText);
        // Add box to the output area
        outputBox.getChildren().add(spinnerBox);
        // Fade in the spinner
        fadeIn(spinnerBox);

        // --- Update State and UI ---
        isRunning = true; // Set running flag

        // Change Run button icon to Stop icon
        if (runBtn.getGraphic() instanceof FontIcon runIcon)
            runIcon.setIconLiteral("fas-stop");
        else
            runBtn.setGraphic(new FontIcon("fas-stop"));

        // Update tooltip text
        if (runBtn.getTooltip() == null)
            runBtn.setTooltip(new Tooltip("stop"));
        else
            runBtn.getTooltip().setText("stop");

        // --- Define Background Task ---
        Task<Void> fakeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Simulate work with a loop
                int steps = 20;
                for (int i = 0; i < steps; i++) {
                    if (isCancelled()) // Check for cancellation
                        break;
                    Thread.sleep(50); // Sleep for 50ms (simulating processing)
                }
                return null;
            }
        };

        currentTask = fakeTask; // Store reference to current task

        // --- Task Completion Handlers ---

        // On Success
        fakeTask.setOnSucceeded(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null)
                spinnerRotate.stop(); // Stop spinner animation
            outputBox.getChildren().clear(); // Clear spinner

            // Prepare output text
            String shellOutput = codeArea.getText().isEmpty() ? ""
                    : "Run clicked for " + cellLanguage.getValue() + ":\n" + codeArea.getText();

            if (shellOutput.trim().isEmpty()) {
                // Handle empty output
                Label noOutputLabel = new Label("(No output to print)");
                noOutputLabel.setStyle(
                        "-fx-text-fill: #888a99; -fx-font-size: 15px; -fx-font-family: 'Fira Mono', 'Consolas', monospace;");
                outputBox.getChildren().add(noOutputLabel);
                fadeIn(noOutputLabel);
            } else {
                // Display output in a read-only TextArea
                TextArea resultArea = new TextArea(shellOutput.trim());
                resultArea.getStyleClass().add("read-only-output"); // Add CSS class
                resultArea.setEditable(false); // Make read-only
                resultArea.setWrapText(true); // Enable text wrapping
                resultArea.setFocusTraversable(false); // Prevent focus stealing
                resultArea.setMaxWidth(1000); // Set max width

                // Calculate and set height based on content
                int lineCount = resultArea.getText().split("\n", -1).length;
                resultArea.setPrefRowCount(Math.max(1, lineCount));
                Platform.runLater(() -> adjustOutputAreaHeight(resultArea));
                // Adjust height on width change (resize)
                resultArea.widthProperty()
                        .addListener((obs, o, n) -> Platform.runLater(() -> adjustOutputAreaHeight(resultArea)));

                outputBox.getChildren().add(resultArea); // Add result area to output box
                fadeIn(resultArea); // Fade it in
            }
            finishExecutionCleanup(); // Reset UI state
        }));

        // On Cancellation
        fakeTask.setOnCancelled(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null)
                spinnerRotate.stop(); // Stop spinner
            outputBox.getChildren().clear(); // Clear spinner
            // Show cancellation message
            Label cancelledLabel = new Label("(Execution cancelled)");
            cancelledLabel.setStyle("-fx-text-fill: #ffb86b; -fx-font-size: 14px;");
            outputBox.getChildren().add(cancelledLabel);
            fadeIn(cancelledLabel);
            finishExecutionCleanup(); // Reset UI state
        }));

        // On Failure
        fakeTask.setOnFailed(ev -> Platform.runLater(() -> {
            if (spinnerRotate != null)
                spinnerRotate.stop(); // Stop spinner
            outputBox.getChildren().clear(); // Clear spinner
            // Show failure message
            Label failLabel = new Label("(Execution failed)");
            failLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");
            outputBox.getChildren().add(failLabel);
            fadeIn(failLabel);
            finishExecutionCleanup(); // Reset UI state
        }));

        // Start the background thread
        new Thread(fakeTask).start();

        // Debug: Dump model content to console
        if (cellModel != null)
            cellModel.dumpContent();
    }

    /**
     * Called internally after execution completes/cancels/fails to restore UI
     * state.
     */
    private void finishExecutionCleanup() {
        isRunning = false;
        currentTask = null;

        if (spinnerRotate != null)
            spinnerRotate.stop();
        if (spinnerBox != null && outputBox.getChildren().contains(spinnerBox))
            outputBox.getChildren().remove(spinnerBox);

        Platform.runLater(() -> {
            if (runBtn != null) {
                if (runBtn.getGraphic() instanceof FontIcon runIcon)
                    runIcon.setIconLiteral("fas-play");
                else
                    runBtn.setGraphic(new FontIcon("fas-play"));
                if (runBtn.getTooltip() == null)
                    runBtn.setTooltip(new Tooltip("Run this cell"));
                else
                    runBtn.getTooltip().setText("Run this cell");
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
            String styleClass = matcher.group("KEYWORD") != null ? "keyword"
                    : matcher.group("PAREN") != null ? "paren"
                            : matcher.group("BRACE") != null ? "brace"
                                    : matcher.group("BRACKET") != null ? "bracket"
                                            : matcher.group("SEMICOLON") != null ? "semicolon"
                                                    : matcher.group("STRING") != null ? "string"
                                                            : matcher.group("CHAR") != null ? "char"
                                                                    : matcher.group("COMMENT") != null ? "comment"
                                                                            : matcher.group("NUMBER") != null ? "number"
                                                                                    : "plain";
            if (matcher.start() > lastKwEnd)
                spansBuilder.add(Collections.singleton("plain"), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end(); // Update last match end
        }

        // Add plain text style for any remaining text after the last match
        if (lastKwEnd < text.length())
            spansBuilder.add(Collections.singleton("plain"), text.length() - lastKwEnd);

        return spansBuilder.create();
    }

    private void adjustOutputAreaHeight(TextArea area) {
        Text helper = new Text();
        helper.setFont(area.getFont());
        helper.setWrappingWidth(area.getWidth() - 10);
        String text = area.getText();
        if (text == null || text.isEmpty())
            text = " ";
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
                new KeyFrame(Duration.millis(400), new KeyValue(node.opacityProperty(), 1)));
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
    public boolean isRunning() {
        return isRunning;
    }

    public Button getRunButton() {
        return runBtn;
    }

    public void clearOutput() {
        if (outputBox != null) {
            outputBox.getChildren().clear();
            outputBox.setVisible(false);
        }
    }

    public void clearContentWithoutConfirm() {
        codeArea.clear();
        if (cellModel != null)
            cellModel.setContent("");
        clearOutput();
    }

    /**
     * Updates the execution count label to show "In [n]:" format
     */
    private void updateExecutionCountLabel() {
        if (executionCountLabel != null && cellModel != null) {
            int count = cellModel.getExecutionCount();
            if (count > 0) {
                executionCountLabel.setText("In [" + count + "]:");
            } else {
                executionCountLabel.setText("");
            }
        }
    }

    /**
     * Adds execution time label at the bottom of the output box
     */
    private void addExecutionTimeLabel() {
        if (executionStartTime > 0) {
            long executionTime = System.currentTimeMillis() - executionStartTime;
            double seconds = executionTime / 1000.0;

            executionTimeLabel = new Label(String.format("(Executed in %.2fs)", seconds));
            executionTimeLabel.setStyle(
                    "-fx-text-fill: #888888; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 8 0 0 0;");
            outputBox.getChildren().add(executionTimeLabel);
        }
    }
}

package com.vessel.ui;
// importing all required javafx classes
import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import javafx.collections.FXCollections;
import javafx.fxml.FXML; // methods linked with FXML basically all those we wrote in main.fxml file those fx:id, is pulled here with this
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene; // UI scene
import javafx.scene.control.*; // buttons, labels, textarea, ChoiceBox
import javafx.scene.layout.*; // VBox, HBox, Priority, Insets
import javafx.stage.FileChooser; // For opening/saving project files
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon; // adding ikonli icons to button

import java.io.*; // reading and writing project files

public class NotebookController {
// these are those fxml elements labelled via fx:id in main.fxml file
    @FXML private VBox codeCellContainer; // that blocks containers made where user actually writes
    @FXML private ChoiceBox<CellType> cellLanguage; // dropdown with 3 lang choices
    @FXML private Label javaVersionLabel; // displays java version of the user in the toolbar

//    private boolean darkMode = false; // default theme is light mode
    private SystemThemeDetector.Theme theme = SystemThemeDetector.getSystemTheme();
    private Scene scene; // reference to the scene in Main.java so we can modify scene, here also

    // Pass scene reference from Main.java
    public void setScene(Scene scene) { // detects and adds system theme stylesheet
        this.scene = scene;
        // Set initial theme
        scene.getStylesheets().add(getClass().getResource((theme == SystemThemeDetector.Theme.LIGHT ? "/light.css" : "/dark.css")).toExternalForm());
    }

    @FXML
    private void initialize() { // called automatically after FXML loads, sets default lang to Java Code, and shows java version in toolbar
        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);
        javaVersionLabel.setText("Java: " + System.getProperty("java.version"));

        // Create default code cell on startup
        createCodeCell(CellType.CODE);
    }

    // -------------------- Cell Creation --------------------

    @FXML
    private void addCell() {
        createCodeCell(cellLanguage.getValue());
    }

    @FXML
    private void addMarkdownCell() {
        createCodeCell(CellType.MARKDOWN);
    }

    // it creates a new cell container with proper formatting and light border
     private void createCodeCell(CellType initialType) {
         NotebookCell cellModel = new NotebookCell();
         cellModel.setType(initialType);

         try {
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/CodeCell.fxml"));
             VBox cell = loader.load();
             CodeCellController cellController = loader.getController();
             cellController.setNotebookCell(cellModel); // Pass cellModel object to the controller
             cellController.setParentContainer(codeCellContainer); // so Delete button can remove this cell
             cellController.setRoot(cell); // pass root for removal
             cellController.setCellType(initialType); //Init language
             codeCellContainer.getChildren().add(cell);
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

    // creates button icon
    private Button makeIconButton(String iconLiteral, String tooltipText) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().add("font-icon");
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setTooltip(new Tooltip(tooltipText));
        return btn;
    }
//    // gives place holder text - ive updated it to use new enum for the sake of later use
//    private String getPromptForType(CellType type) {
//        return switch (type) {
//            case CODE -> "Enter Java code here...";
//            case MARKDOWN -> "Enter Markdown content...";
//            case TEXT -> "Enter plain text...";
//        };
//    }

    // -------------------- Toolbar Actions --------------------
    // NOTE: NEED TO ADD LOGIC FOR EACH BUTTON!
    @FXML private void cutCell() { System.out.println("Cut cell"); }
    @FXML private void copyCell() { System.out.println("Copy cell"); }
    @FXML private void pasteCell() { System.out.println("Paste cell"); }
    @FXML private void moveUpCell() { System.out.println("Move cell up"); }
    @FXML private void moveDownCell() { System.out.println("Move cell down"); }
    @FXML private void runCell() { System.out.println("Run all cells"); }
    @FXML private void pauseCell() { System.out.println("Pause all cells"); }
    @FXML private void refreshCell() { System.out.println("Refresh all cells"); }

    // -------------------- File Actions --------------------
    // Saving project to system
    @FXML
    private void saveProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vessel Project", "*.vessel"));
        File file = fileChooser.showSaveDialog(codeCellContainer.getScene().getWindow());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (var node : codeCellContainer.getChildren()) {
                    if (node instanceof VBox cellBox) {
                        HBox header = (HBox) cellBox.getChildren().get(0);
                        ChoiceBox<String> lang = (ChoiceBox<String>) header.getChildren().get(0);
                        TextArea codeArea = (TextArea) cellBox.getChildren().get(1);

                        writer.write(lang.getValue());
                        writer.newLine();
                        writer.write(codeArea.getText().replace("\r", "\\r").replace("\n", "\\n"));
                        writer.newLine();
                        writer.write("---CELL-END---");
                        writer.newLine();
                    }
                }
                System.out.println("Project saved to " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // opens already existing project
    @FXML
    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Vessel Project", "*.vessel"));
        File file = fileChooser.showOpenDialog(codeCellContainer.getScene().getWindow());

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                codeCellContainer.getChildren().clear();
                String line;
                String type = null;
                StringBuilder code = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.equals("---CELL-END---")) {
                        createCodeCellFromFile(type, code.toString());
                        code.setLength(0);
                        type = null;
                    } else if (type == null) {
                        type = line;
                    } else {
                        code.append(line.replace("\\n", "\n").replace("\\r", "\r")).append("\n");
                    }
                }
                System.out.println("Project loaded from " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // NOT SURE ABOUT THIS PART
    private void createCodeCellFromFile(String type, String content) {
        VBox cellBox = new VBox(5);
        cellBox.setPadding(new Insets(8));
        cellBox.setStyle("-fx-border-color: #aaa; -fx-border-width: 1; -fx-background-color: -fx-base;");

        HBox header = new HBox(8);
        ChoiceBox<String> cellLanguage = new ChoiceBox<>();
        cellLanguage.getItems().addAll("Java Code", "Markdown", "Plain Text");
        cellLanguage.setValue(type);

        Button runBtn = makeIconButton("fas-play", "Run this cell");
        Button deleteBtn = makeIconButton("fas-trash", "Delete this cell");
        Button clearBtn = makeIconButton("fas-sync-alt", "Clear this cell");

        TextArea codeArea = new TextArea();
        codeArea.setText(content);
        codeArea.setPrefRowCount(5);
        // NEED TO FIX THIS!
        VBox.setVgrow(codeArea, Priority.ALWAYS); // expands vertically as there are more number of lines in that small placeholder

        header.getChildren().addAll(cellLanguage, runBtn, deleteBtn, clearBtn);
        cellBox.getChildren().addAll(header, codeArea);
        codeCellContainer.getChildren().add(cellBox);
    }

    // -------------------- Menu Actions --------------------
    // NOTE: NEED TO ADD LOGIC FOR EACH BUTTON!
    @FXML private void exportPDF() { System.out.println("Export PDF"); }
    @FXML private void undoAction() { System.out.println("Undo"); }
    @FXML private void redoAction() { System.out.println("Redo"); }
    @FXML private void toggleToolbar() { System.out.println("Toggle Toolbar"); }
    @FXML private void zoomIn() { System.out.println("Zoom In"); }
    @FXML private void zoomOut() { System.out.println("Zoom Out"); }
    @FXML private void showAbout() { System.out.println("Show About"); }
    @FXML private void showDocs() { System.out.println("Show Documentation"); }

    // -------------------- Theme Toggle --------------------
    // simple method to toggle theme
    @FXML
    private void toggleTheme() {
        if (scene == null) return;

        scene.getStylesheets().clear();
        if (theme == SystemThemeDetector.Theme.DARK) {
            scene.getStylesheets().add(getClass().getResource("/light.css").toExternalForm());
            theme = SystemThemeDetector.Theme.LIGHT;
        } else {
            scene.getStylesheets().add(getClass().getResource("/dark.css").toExternalForm());
            theme = SystemThemeDetector.Theme.DARK;
        }
    }
}

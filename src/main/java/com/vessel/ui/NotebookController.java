package com.vessel.ui;
// importing all required javafx classes
import com.vessel.model.CellType;
import com.vessel.model.Notebook;
import com.vessel.model.NotebookCell;
import com.vessel.persistence.NotebookPersistence;
import javafx.collections.FXCollections;
import javafx.fxml.FXML; // methods linked with FXML basically all those we wrote in Notebook.fxml file those fx:id, is pulled here with this
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
    @FXML private Menu insertMenu;
//    private boolean darkMode = false; // default theme is light mode
    private SystemThemeDetector.Theme theme = SystemThemeDetector.getSystemTheme();
    private Scene scene; // reference to the scene in Main.java so we can modify scene, here also
    private final NotebookPersistence persistence = new NotebookPersistence();
    private Notebook currentNotebook;


    // Pass scene reference from Main.java
    public void setScene(Scene scene) { // detects and adds system theme stylesheet
        this.scene = scene;
        // Set initial theme
        scene.getStylesheets().add(getClass().getResource((theme == SystemThemeDetector.Theme.LIGHT ? "/light.css" : "/dark.css")).toExternalForm());
    }

    @FXML
    private void initialize() {// called automatically after FXML loads, sets default lang to Java Code, and shows java version in toolbar

        // Notebook init.
        currentNotebook = new Notebook("untitled"); //  hardcoded right now

        cellLanguage.setItems(FXCollections.observableArrayList(CellType.values())); // Fill the choice dropbox thing
        cellLanguage.setValue(CellType.CODE);
        javaVersionLabel.setText("Java: " + System.getProperty("java.version"));

        // Dynamically populating insert menu
        for (CellType type : CellType.values()) {
            MenuItem item = new MenuItem("Add " + type.toString()); // Will show something like "Add xyz"
            item.setOnAction(e -> addCell(type));
            insertMenu.getItems().add(item);
        }

        // Create default code cell on startup
        addCell(CellType.CODE);
    }

    // -------------------- Cell Creation --------------------

    // it creates a new cell container with proper formatting and light border
     private void addCell(CellType initialType) {
         NotebookCell cellModel = new NotebookCell();
         cellModel.setType(initialType);
         currentNotebook.addCell(cellModel);   // <-- IMPORTANT (this was missing)
         codeCellContainer.getChildren().add(createCellUI(initialType, cellModel));
     }

     // Parameterless overloading (used by .fxml files)
    @FXML
    private void addCell() {
        addCell(cellLanguage.getValue());
    }

    // Factory method that dumps out the VBox (div) housing the code cell
    private VBox createCellUI(CellType type, NotebookCell cellModel) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CodeCell.fxml"));
            VBox cell = loader.load();
            CodeCellController cellController = loader.getController();
            if (cellController instanceof CodeCellController ) {
                cellController.setEngine(currentNotebook.getEngine());
            }
            cellController.setNotebookCell(cellModel); // Pass cellModel object to the controller
            cellController.setParentContainer(codeCellContainer); // so Delete button can remove this cell
            cellController.setRoot(cell); // pass root for removal
            cellController.setCellType(type); //Init language
            cell.setUserData(cellController);
            return cell;
        }catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void syncModelFromUI() {
        currentNotebook.getCells().clear();
        for (var node : codeCellContainer.getChildren()) {
            if (node instanceof VBox cellBox) {
                // retrieve the controller for this cell
                CodeCellController controller = (CodeCellController) cellBox.getUserData();
                NotebookCell cell = controller.getNotebookCell();
                currentNotebook.addCell(cell);
            }
        }
    }


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
        TextInputDialog dialog = new TextInputDialog(currentNotebook.getName());
        dialog.setTitle("Save Notebook");
        dialog.setHeaderText("Enter notebook name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            currentNotebook.setName(name);   // rename notebook without resetting its data
            syncModelFromUI();        // sync ui state into data model before saving
            boolean ok = persistence.save(currentNotebook);
            System.out.println(ok ? "Saved!" : "Save failed");
        });
    }

    // opens already existing project
    // #TODO: Need to be replaced by json logic
    @FXML
    private void openProject() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Open Notebook");
        dialog.setHeaderText("Enter notebook name to load:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            Notebook loaded = persistence.load(name);

            if (loaded != null) {
                if (currentNotebook != null) {
                    currentNotebook.shutdownEngine();
                }
                currentNotebook = loaded;
                currentNotebook.initEngineIfNull();
                renderNotebook();
                System.out.println("Notebook loaded successfully.");
            } else {
                System.out.println("Could not load notebook: " + name);
            }
        });
    }

    // clears ui and rebuilds all cells from the loaded notebook model
    private void renderNotebook() {
        codeCellContainer.getChildren().clear();
        for (NotebookCell cell : currentNotebook.getCells()) {
            codeCellContainer.getChildren().add(createCellUI(cell.getType(), cell));
        }
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

    // -------------------- Helpers --------------------
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

    public Notebook getCurrentNotebook() {
        return currentNotebook;
    }
}

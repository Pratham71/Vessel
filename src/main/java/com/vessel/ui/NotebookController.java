package com.vessel.ui;
// importing all required javafx classes

/*
* ESSENTIAL/SEMI-ESSENTIAL STUFF
* #TODO: Add move cell logic to cells
* #TODO: Add logic to all global buttons (eg run all button)
* #TODO: Add a menu tab for shell controls, to start, shutdown and restart shell
* #TODO: Add logic to all menu buttons - ESPECIALLY undo/redo, zoom in/out etc
*       - if any of them are too difficult to implement then remove from menubar
* #TODO: Add indicators for shell status
* #TODO: Make cell's horizontal scrollbar visible (currently invisible)
* #TODO: Style scrollbars to be a dark color instead of white - looks VERY off (for dark.css/darkmode only)
* #TODO: Add keyboard shortcut functionality - undo/redo, cntrl + S to save etc
*
* #TODO: autosave - if user tries to close an already SAVED file with new changes, it should auto-save before closing
*       (does not apply to unsaved "untitled" files)
*           -you can also autosave every x minutes
* #TODO: untitled files with new content should prompt a dialogue if user tries closing app without saving it
*       - make sure dialogue/alert is positioned on top of parent window
* #TODO: Add logic (if present in branch) for auto indents, auto closing brackets etc.
*
* #TODO: Add a "New notebook" button/option to the File tab in the menubar that opens a new note book
* #TODO: Add zoom functionality
* #TODO: Add a editable label/button (turns to textarea on click) that shows notebook name
* #TODO: Remove obsolete, useless, or overly complicated buttons
*
* MIGRATING STUFF FROM PRI'S "optimized/Frontend-uidesign" BRANCH
* #TODO: Add/improve on automatic cell expansion logic - more lines = box grows
*       (both input and output - output breaks sometimes with large no. of lines, with current logic)
* #TODO: Add logic for bubbling up scroll from codeArea to parent notebook
*       (basically when u hover mouse on codecell currently, notebook doesnt scroll - tries to scroll cell instead)
*           - cell growth logic must work for this!
*
* OPTIONAL STUFF (if we have time - prioritize last)
* #TODO: (Optional) Settings menu -> autosave time and more
* #TODO: (Optional) Landing page on launching app -> shows previous notebooks
*/

import com.vessel.model.CellType;
import com.vessel.model.Notebook;
import com.vessel.model.NotebookCell;
import com.vessel.persistence.NotebookPersistence;
import javafx.application.Platform;
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
import javafx.concurrent.Task;
import javafx.stage.Window;
import com.vessel.Kernel.NotebookEngine;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



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
    private final NotebookEngine engine = new NotebookEngine(); // global notebook engine instance used to run all jshell executions across all cells
    private Future<?> runAllFuture;     // future object returned by the "run all" task so we can pause/cancel it later
    private final ExecutorService toolbarExecutor = Executors.newSingleThreadExecutor(); // single-thread executor used only for global toolbar actions like run all / pause all
    private NotebookCell clipboardCell = null; // stores copied or cut cell temporarily


    private void removeCellFromUI(GenericCellController controller) {// removes a cell's vbox from the ui
        codeCellContainer.getChildren().remove(controller.getRoot());
    }

    // returns the cell controller that is currently selected by the user
    // used by global actions like cut, copy, paste, move up/down
    private GenericCellController getSelectedCell() {
        for (var node : codeCellContainer.getChildren()) {
            GenericCellController c = (GenericCellController) node.getUserData();
        }
        return null;
    }

    // removes selection highlight from all cells in the notebook
    // ensures only one cell can be selected at a time
    private void clearAllSelections() {
        for (var node : codeCellContainer.getChildren()) {
            GenericCellController c = (GenericCellController) node.getUserData();
        }
    }

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
            final String fxml = (type == CellType.CODE) ? "/CodeCell.fxml" : "/TextCell.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            VBox cell = loader.load();

            GenericCellController controller = loader.getController();
            if (controller instanceof CodeCellController ) {
                controller.setEngine(currentNotebook.getEngine());
            }
            controller.setNotebookCell(cellModel); // Pass cellModel object to the controller
            controller.setNotebookController(this);
            controller.setParentContainer(codeCellContainer); // so Delete button can remove this cell
            controller.setRoot(cell); // pass root for removal
            controller.setCellType(type); //Init language
            cell.setUserData(controller); // clicking the cell should mark it as the globally selected cell
            cell.setOnMouseClicked(e -> { // clears selection for all other cells and highlights only this one
                clearAllSelections();
            });
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
                var controller = (GenericCellController) cellBox.getUserData();
                NotebookCell cell = controller.getNotebookCell();
                currentNotebook.addCell(cell);
            }
        }
    }

    public Notebook getNotebook() {
        return currentNotebook;
    }


    // -------------------- Toolbar Actions --------------------
    // NOTE: NEED TO ADD LOGIC FOR EACH BUTTON!
    // holds a single copied / cut cell model

    // removes the currently selected cell from both ui + notebook model
    // and stores it in a temporary clipboard for pasting later
    @FXML private void cutCell() {
        GenericCellController selected = getSelectedCell();
        if (selected == null) return;
        clipboardCell = selected.getNotebookCell(); // store model
        removeCellFromUI(selected);
        currentNotebook.removeCell(clipboardCell.getId());
    }

    // copies the selected cell model into a temporary clipboard
    // does not remove it from the notebook
    @FXML private void copyCell() {
        GenericCellController selected = getSelectedCell();
        if (selected == null) return;
        // deep copy to avoid modifying original
        NotebookCell clone = new NotebookCell();
        clone.setType(selected.getNotebookCell().getType());
        clone.setContent(selected.getNotebookCell().getContent());
        clipboardCell = clone;
    }

    // inserts a new cell below the currently selected one using clipboard contents
    @FXML private void pasteCell() {
        if (clipboardCell == null) return;
        GenericCellController selected = getSelectedCell();
        int index = (selected == null)
                ? currentNotebook.getCells().size()
                : codeCellContainer.getChildren().indexOf(selected.getRoot()) + 1;
        NotebookCell clone = new NotebookCell();
        clone.setType(clipboardCell.getType());
        clone.setContent(clipboardCell.getContent());
        currentNotebook.addCellAt(index, clone);
        VBox ui = createCellUI(clone.getType(), clone);
        codeCellContainer.getChildren().add(index, ui);
    }
    @FXML private void moveUpCell() {
        GenericCellController selected = getSelectedCell();
        if (selected == null) return;
        VBox ui = selected.getRoot();
        int index = codeCellContainer.getChildren().indexOf(ui);
        if (index <= 0) return; // already at top
        // swap in model
        Collections.swap(currentNotebook.getCells(), index, index - 1);
        // swap in ui
        codeCellContainer.getChildren().remove(ui);
        codeCellContainer.getChildren().add(index - 1, ui);
    }
    @FXML private void moveDownCell() {
        GenericCellController selected = getSelectedCell();
        if (selected == null) return;
        VBox ui = selected.getRoot();
        int index = codeCellContainer.getChildren().indexOf(ui);
        if (index >= codeCellContainer.getChildren().size() - 1) return; // already at bottom
        // swap in model
        Collections.swap(currentNotebook.getCells(), index, index + 1);
        // swap in ui
        codeCellContainer.getChildren().remove(ui);
        codeCellContainer.getChildren().add(index + 1, ui); }

    @FXML private void runCell() {
        // prevent starting another run-all if one is already in progress
        if (runAllFuture != null && !runAllFuture.isDone()) {
            System.out.println("run-all already in progress, ignoring click");
            return;
        }
        runAllFuture = toolbarExecutor.submit(() -> {
            System.out.println("=== running all cells ===");
            for (var node : codeCellContainer.getChildren()) {
                if (!(node instanceof VBox cellBox)) continue;
                var controller = (GenericCellController) cellBox.getUserData();
                // only code cells have executable java code; skip markdown/text cells
                if (!(controller instanceof CodeCellController codeController)) {
                    continue;
                }
                NotebookCell cell = codeController.getNotebookCell();
                Platform.runLater(codeController::showExecutingState);
                engine.execute(cell);
                // grab the result that engine stored in the cell and push it to the ui
                var result = cell.getExecutionResult();
                Platform.runLater(() -> codeController.updateOutput(result));

                // if user presses pause
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("run-all interrupted, stopping loop");
                    return;
                }
            }
            System.out.println("=== run-all finished ===");
        });
    }

    // pauses any ongoing "run all" execution by cancelling the future
    // also interrupts the jshell kernel so the currently running cell stops immediately
    @FXML private void pauseCell() {
        System.out.println("Pausing kernel...");
        if (runAllFuture != null) {
            runAllFuture.cancel(true);
        }
        engine.interrupt(); // stops JShell execution
        System.out.println("Execution interrupted."); }

    // clears only the output areas of every cell but keeps the input code intact
    @FXML private void refreshCell() {
        System.out.println("Resetting kernel...");
        engine.resetKernel();
        for (var node : codeCellContainer.getChildren()) {
            if (node instanceof VBox cellBox) {
                CodeCellController controller = (CodeCellController) cellBox.getUserData();
                controller.clearOutputOnly();
            }
        }
    }

    // -------------------- File Actions --------------------
    // Saving project to system
    @FXML
    private void saveProject() {
        // sync UI → model
        syncModelFromUI();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Notebook");
        // open in /notebooks by default
        fileChooser.setInitialDirectory(new File("notebooks"));
        fileChooser.setInitialFileName(currentNotebook.getName() + ".json");
        // allow only json files
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Vessel Notebook (*.json)", "*.json"));
        File file = fileChooser.showSaveDialog(codeCellContainer.getScene().getWindow());
        if (file == null) return; // user canceled

        // wrap save logic in Task
        Task<Boolean> saveTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return persistence.saveToPath(currentNotebook, file.getAbsolutePath());
            }
        };

        saveTask.setOnSucceeded(e -> System.out.println("save done!"));
        saveTask.setOnFailed(e -> System.out.println("save failed."));

        new Thread(saveTask).start();
    }

    // opens already existing project
    // #TODO: Need to be replaced by json logic
    @FXML
    private void openProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Notebook");
        fileChooser.setInitialDirectory(new File("notebooks"));
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Vessel Notebook (*.json)", "*.json"));
        File file = fileChooser.showOpenDialog(codeCellContainer.getScene().getWindow());
        if (file == null) return;
        Notebook loaded = persistence.loadFromPath(file.getAbsolutePath());
        if (loaded != null) {
            if (currentNotebook != null) {
                currentNotebook.shutdownEngine();
            }
            currentNotebook = loaded;
            currentNotebook.initEngineIfNull();
            renderNotebook();
            System.out.println("loaded ok");
        } else {
            System.out.println("load failed");
        }
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
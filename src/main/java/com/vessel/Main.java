package com.vessel; // com.vessel is folder(package) inside which we have all the files
// some javafx classes are imported
// Main.java
import com.vessel.ui.NotebookController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private NotebookController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Notebook.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 650);

        controller = loader.getController();
        controller.setScene(scene);

        stage.setScene(scene);
        stage.setTitle("Vessel Notebook");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Called when app closes
        if (controller != null && controller.getCurrentNotebook() != null) {
            controller.getCurrentNotebook().shutdownEngine();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


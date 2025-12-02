package com.vessel.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vessel.model.Notebook;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.time.LocalDateTime;

// gson-based json persistence layer for saving and loading notebook objects
public class NotebookPersistence {

    private static final String ROOT = "notebooks/";
    private final Gson gson;

    public NotebookPersistence() {
        // configure gson with support for localdatetime serialization and pretty printing
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                                LocalDateTime.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();

        ensureRoot();
    }

    // makes sure the notebooks/ directory exists before saving any files
    private void ensureRoot() {
        File dir = new File(ROOT);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // removes illegal characters from filename and replaces spaces with underscores
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_ ]", "")
                .replace(" ", "_");
    }

    // converts the entire notebook object into json and writes it to disk
    // returns true if saving worked, false if any io error happened
    public boolean save(Notebook notebook) {
        String cleanName = sanitize(notebook.getName());
        File file = new File(ROOT + cleanName + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            // write notebook object as json into the file
            gson.toJson(notebook, writer);
            return true;
        } catch (IOException e) {
            System.err.println("[NotebookPersistence] Save failed: " + e.getMessage());
            return false;
        }
    }

    // loads a notebook json file from disk and converts it back into a notebook object
    // returns null if file not found or loading failed
    public Notebook load(String name) {
        String cleanName = sanitize(name);
        File file = new File(ROOT + cleanName + ".json");

        if (!file.exists()) {
            System.err.println("[NotebookPersistence] File not found: " + file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, Notebook.class);
        } catch (IOException e) {
            System.err.println("[NotebookPersistence] Load failed: " + e.getMessage());
            return null;
        }
    }

}

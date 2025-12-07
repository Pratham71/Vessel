package com.vessel.ui;

import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import com.vessel.util.SyntaxService;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Collection;

public class TextCellController extends GenericCellController {

    @FXML private ToggleButton previewToggle;
    @FXML private Button moveUpBtn;
    @FXML private Button moveDownBtn;
    @FXML private StackPane editorStack;

    // WebView for markdown preview (created lazily)
    private WebView markdownPreview;

    @FXML
    @Override
    protected void initialize() {
        // Generic wiring: cellLanguage, delete/clear, prompt, content binding
        super.initialize();

        // For now, move up/down buttons are just stubs
        moveUpBtn.setOnAction(e -> moveCell(-1));
        moveDownBtn.setOnAction(e -> moveCell(1));

        // Preview toggle for markdown
        previewToggle.setOnAction(e -> {
            boolean selected = previewToggle.isSelected();

            if (selected) {
                showPreview();
            } else {
                showEditorOnly();
            }

            if (cellModel != null) {
                cellModel.setMarkdownPreviewOn(selected);
            }
        });
    }

    @Override
    public void setNotebookCell(NotebookCell cell) {
        super.setNotebookCell(cell);
        if (cell.getType() != null) {
            setCellType(cell.getType());
        }

        if (cell.getType() == CellType.MARKDOWN && cell.isMarkdownPreviewOn()) {
            // ensure toggle shows ON and preview is visible
            previewToggle.setSelected(true);   // will also show selected CSS
            showPreview();
        } else {
            previewToggle.setSelected(false);
            showEditorOnly();
        }
    }

    @Override
    public void setCellType(CellType type) {
        super.setCellType(type);
        if (type == CellType.MARKDOWN) {
            promptLabel.setText("Enter markdown here");
            setPreviewToggleVisible(true);
            // to apply highlighting immediately on switching
            codeArea.setStyleSpans(0, SyntaxService.computeMarkdownHighlighting(codeArea.getText()));
            enableMarkdownHighlighting();
        } else { // TEXT/plain
            promptLabel.setText("Enter text here");
            setPreviewToggleVisible(false);
            disableMarkdownHighlighting();        // one-shot StyleSpans - applies plain style
            showEditorOnly();
        }
    }
    /* --------- Syntax Highlighting ---------- */

    private void disableMarkdownHighlighting() {
        // Just apply one big "plain" span so everything uses .code-area .plain
        String text = codeArea.getText();
        var builder = new org.fxmisc.richtext.model.StyleSpansBuilder<Collection<String>>();
        builder.add(java.util.Collections.singleton("plain"), text.length());
        codeArea.setStyleSpans(0, builder.create());
    }

    private void enableMarkdownHighlighting() {
        // Attach a listener using the markdown syntax service (next section)
        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(ch -> codeArea.setStyleSpans(
                        0, SyntaxService.computeMarkdownHighlighting(codeArea.getText())
                ));
    }

    /* --------- Preview handling ---------- */

    private void setPreviewToggleVisible(boolean visible) {
        previewToggle.setVisible(visible);
        previewToggle.setManaged(visible);

        if (!visible && previewToggle.isSelected()) {
            previewToggle.setSelected(false);
        }
    }

    private void showPreview() {
        ensurePreviewCreated();

        // get current theme from NotebookController
        SystemThemeDetector.Theme theme =
                (notebookController != null) ? notebookController.getTheme()
                        : SystemThemeDetector.getSystemTheme();

        String md = codeArea.getText();
        String html = SyntaxService.renderMarkdownToHtml(md, theme);

        markdownPreview.getEngine().loadContent(html);

        codeArea.setVisible(false);
        codeArea.setManaged(false);

        markdownPreview.setVisible(true);
        markdownPreview.setManaged(true);
    }

    public void refreshPreview() {
        // only if preview is currently visible
        if (markdownPreview == null || !markdownPreview.isVisible()) {
            return;
        }

        SystemThemeDetector.Theme theme =
                (notebookController != null) ? notebookController.getTheme()
                        : SystemThemeDetector.getSystemTheme();

        String md = codeArea.getText();
        String html = SyntaxService.renderMarkdownToHtml(md, theme);
        markdownPreview.getEngine().loadContent(html);
    }

    private void ensurePreviewCreated() {
        if (markdownPreview != null) return;

        markdownPreview = new WebView();
        markdownPreview.setContextMenuEnabled(false);

        // window starts small then height will be updated from JS bridge
        markdownPreview.setMinHeight(0);
        markdownPreview.setPrefHeight(0);
        markdownPreview.setMaxHeight(Region.USE_PREF_SIZE);

        WebEngine engine = markdownPreview.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                Object winObj = engine.executeScript("window");
                if (winObj instanceof JSObject win) {
                    win.setMember("java", new PreviewBridge());
                    engine.executeScript("updateHeight()");
                }
            }
        });

        editorStack.getChildren().add(markdownPreview);
        markdownPreview.setVisible(false);
        markdownPreview.setManaged(false);
    }

    public class PreviewBridge {
        public void resize(double height) {
            Platform.runLater(() -> {
                double h = Math.max(32, height + 1); // small safety margin
                markdownPreview.setPrefHeight(h);
                markdownPreview.setMinHeight(h);
                markdownPreview.setMaxHeight(h);
            });
        }
    }

    private void showEditorOnly() {
        codeArea.setVisible(true);
        codeArea.setManaged(true);

        if (markdownPreview != null) {
            markdownPreview.setVisible(false);
            markdownPreview.setManaged(false);
        }
    }

    private void moveCell(int delta) {
        // Hook into NotebookController later (e.g. notebookController.moveCell(cellModel, delta))
    }
}

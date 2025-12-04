package com.vessel.ui;

import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import com.vessel.util.SyntaxService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

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
            if (previewToggle.isSelected()) {
                showPreview();
            } else {
                showEditorOnly();
            }
        });
    }

    @Override
    public void setNotebookCell(NotebookCell cell) {
        super.setNotebookCell(cell);
        if (cell.getType() != null) {
            setCellType(cell.getType());
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
        if (markdownPreview == null) {
            markdownPreview = new WebView();
            markdownPreview.setContextMenuEnabled(false);
        }

        String content = codeArea.getText();
        String html =
                "<html><head><meta charset='UTF-8'></head><body><pre>"
                        + escapeHtml(content)
                        + "</pre></body></html>";

        markdownPreview.getEngine().loadContent(html);

        if (!editorStack.getChildren().contains(markdownPreview)) {
            editorStack.getChildren().add(markdownPreview);
        }

        codeArea.setVisible(false);
        codeArea.setManaged(false);
        markdownPreview.setVisible(true);
        markdownPreview.setManaged(true);
    }

    private void showEditorOnly() {
        codeArea.setVisible(true);
        codeArea.setManaged(true);

        if (markdownPreview != null) {
            markdownPreview.setVisible(false);
            markdownPreview.setManaged(false);
        }
    }

    private String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void moveCell(int delta) {
        // Hook into NotebookController later (e.g. notebookController.moveCell(cellModel, delta))
    }
}

package com.vessel.ui;

import com.vessel.model.CellType;
import com.vessel.model.NotebookCell;
import com.vessel.util.SyntaxService;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.kordamp.ikonli.javafx.FontIcon;


public class TextCellController extends GenericCellController {

    @FXML private ToggleButton previewToggle;
    @FXML private Button moveUpBtn;
    @FXML private Button moveDownBtn;
    @FXML private StackPane editorStack;

    @FXML private StackPane previewLoadingOverlay;
    @FXML FontIcon previewSpinnerIcon;

    private WebView markdownPreview;
    private RotateTransition previewSpin;

    @FXML
    @Override
    protected void initialize() {
        super.initialize();

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
                .subscribe(ch -> applyHighlighting());

        previewSpin = new RotateTransition(Duration.seconds(1), previewSpinnerIcon);
        previewSpin.setByAngle(360);
        previewSpin.setCycleCount(RotateTransition.INDEFINITE);

        // TODO: wire stubs to main notebook cell
        moveUpBtn.setOnAction(e -> moveCell(-1));
        moveDownBtn.setOnAction(e -> moveCell(1));

        previewToggle.setOnAction(e -> {
            boolean selected = previewToggle.isSelected();
            if(codeArea.getContent() == null || codeArea.getText().isBlank()){return;}
            if (selected) {
//                codeArea.setManaged(false);
//                if (markdownPreview != null) {
//                    markdownPreview.setManaged(true);
//                }
                showPreview();
            } else {
//                codeArea.setManaged(true);
//                if (markdownPreview != null) {
//                    markdownPreview.setManaged(false);
//                }
                hidePreview();
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
            previewToggle.setSelected(true);
            showPreview();
        } else {
            previewToggle.setSelected(false);
            hidePreview();
        }
    }

    @Override
    public void setCellType(CellType type) {
        super.setCellType(type);
        if (type == CellType.MARKDOWN) {
            promptLabel.setText("Enter markdown here");
            setPreviewToggleVisible(true);

            codeArea.setStyleSpans(0, SyntaxService.computeMarkdownHighlighting(codeArea.getText()));
        } else {
            promptLabel.setText("Enter text here");
            setPreviewToggleVisible(false);
            hidePreview();
        }

        applyHighlighting();
    }
    /* --------- Syntax Highlighting ---------- */

    private void applyHighlighting() {
        String text = codeArea.getText();

        if (cellModel != null && cellModel.getType() == CellType.MARKDOWN) {
            codeArea.setStyleSpans(0,
                    SyntaxService.computeMarkdownHighlighting(text));
        } else {
            var builder =
                    new org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>>();
            builder.add(java.util.Collections.singleton("plain"), text.length());
            codeArea.setStyleSpans(0, builder.create());
        }
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
        codeArea.setEditable(false);
        codeArea.deselect();

        previewLoadingOverlay.setVisible(true);
        previewLoadingOverlay.setManaged(true);
        previewSpin.play();

        SystemThemeDetector.Theme theme =
                (notebookController != null) ? notebookController.getTheme()
                        : SystemThemeDetector.getSystemTheme();
        String md = codeArea.getText();
        String html = SyntaxService.renderMarkdownToHtml(md, theme);

        // ensurePreviewCreated() is (I *think*) computationally heavy so I added a loadscreen logic here
        Platform.runLater(() -> {
            ensurePreviewCreated();

            markdownPreview.getEngine().loadContent(html);
        });
    }

    private void hidePreview() {
        previewSpin.stop();
        previewLoadingOverlay.setVisible(false);
        previewLoadingOverlay.setManaged(false);
        codeArea.setEditable(true);

        if (markdownPreview != null && markdownPreview.isVisible()) {
            fadeOutPreview();
        } else {
            // fallback to direct switch if stuff breaks/loading is interrupted
            codeArea.setVisible(true);
            if (markdownPreview != null) {
                markdownPreview.setVisible(false);
            }
        }
    }

    private void fadeInPreview() {
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), codeArea);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        javafx.animation.FadeTransition fadeIn =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), markdownPreview);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeOut.setOnFinished(e -> {
            codeArea.setVisible(false);
            codeArea.setManaged(false);
            markdownPreview.setVisible(true);
            markdownPreview.setManaged(true);

            fadeIn.play();
        });

        fadeOut.play();
    }

    private void fadeOutPreview() {
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(Duration.millis(250), markdownPreview);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        javafx.animation.FadeTransition fadeIn =
                new javafx.animation.FadeTransition(Duration.millis(250), codeArea);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        fadeOut.setOnFinished(e -> {
            markdownPreview.setVisible(false);
            markdownPreview.setManaged(false);
            codeArea.setVisible(true);
            codeArea.setManaged(true);

            fadeIn.play();
        });

        fadeOut.play();
    }

    public void refreshPreview() {
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

        markdownPreview.setMinHeight(0);
        markdownPreview.setPrefHeight(Region.USE_COMPUTED_SIZE);
        markdownPreview.setMaxHeight(Region.USE_COMPUTED_SIZE);

        markdownPreview.setVisible(false);

        WebEngine engine = markdownPreview.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SCHEDULED) {
                // reset before each load
                Platform.runLater(() -> {
                    markdownPreview.setMinHeight(0);
                    markdownPreview.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    markdownPreview.setMaxHeight(Region.USE_COMPUTED_SIZE);
                });
            }

            if (state == Worker.State.SUCCEEDED) {
                Object winObj = engine.executeScript("window");
                if (winObj instanceof JSObject win) {
                    win.setMember("java", new PreviewBridge());
                }

                // ask JS to compute height; overlay is still visible now
                Platform.runLater(() -> {
                    try {
                        engine.executeScript("updateHeight()");
                    } catch (Exception ignored) {
                    }
                });
            }
        });

        editorStack.getChildren().add(markdownPreview);
    }


    // IMPORTANT: DO NOT DELETE
    // IDE may tell u the method is unused, but its called from the html preview's <script> body
    public class PreviewBridge {
        public void resize(double height) {
            Platform.runLater(() -> {
                // avoids pure 0 height
                double safe = Math.max(1, height);

                markdownPreview.setMinHeight(safe);
                markdownPreview.setPrefHeight(Region.USE_COMPUTED_SIZE);
                markdownPreview.setMaxHeight(Region.USE_COMPUTED_SIZE);

                if (previewLoadingOverlay.isVisible()) {
                    previewSpin.stop();
                    previewLoadingOverlay.setVisible(false);
                    previewLoadingOverlay.setManaged(false);
                    fadeInPreview();
                }
            });
        }
    }

    private void moveCell(int delta) {
        // TODO: hook into NotebookController later (e.g. notebookController.moveCell(cellModel, delta))
    }
}

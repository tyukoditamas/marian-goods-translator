package org.app.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.app.service.ExcelTranslationService;

import java.io.File;

public class MainController {
    @FXML
    private Button browseButton;
    @FXML
    private TextArea logArea;
    @FXML
    private ProgressIndicator spinner;

    @FXML
    private void onBrowseExcel() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
        );
        File excelFile = chooser.showOpenDialog(browseButton.getScene().getWindow());
        if (excelFile == null) {
            log("Excel file selection cancelled.");
            return;
        }
        log("Processing Excel file: " + excelFile);

        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                ExcelTranslationService svc = new ExcelTranslationService(line ->
                        Platform.runLater(() -> log(line))
                );
                return svc.processExcel(excelFile);
            }
        };

        spinner.visibleProperty().bind(task.runningProperty());
        browseButton.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(e -> {
            File out = task.getValue();
            log("Translated Excel saved: " + out.getAbsolutePath());
        });
        task.setOnFailed(e -> {
            log("Translation failed: " + task.getException().getMessage());
        });

        new Thread(task, "excel-translator").start();
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }
}

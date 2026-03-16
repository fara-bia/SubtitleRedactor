package subtitleRedactor;

import java.io.File;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Window {

    private static final String INVALID_FILE_NAME_CHARS = "[\\\\/:*?\"<>|]";
    private static final String MODE_SELECTED_STYLE =
            "-fx-background-color: #0f766e; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String MODE_UNSELECTED_STYLE =
            "-fx-background-color: #d1fae5; -fx-text-fill: #134e4a;";

    private File selectedAssFile;
    private File selectedDirectory;
    private Thread workerThread;
    private FixerAI.RunMode selectedRunMode = FixerAI.RunMode.COOL;
    private FixerAI.GpuMode selectedGpuMode = FixerAI.GpuMode.DONT_USE_GPU;

    public Window(Stage stage) {

        stage.setOnCloseRequest(event -> {
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
            }

            FixerAI.destroyActiveProcesses();
        });

        VBox root = new VBox(15);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(20));

        root.setBackground(
                new Background(new BackgroundFill(Color.AQUAMARINE, null, null)));

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);

        Label languageLabel = new Label("English");
        topBar.getChildren().add(languageLabel);

        Button selectButton = new Button("Select .ASS file");
        selectButton.setPrefWidth(220);
        selectButton.setPrefHeight(60);

        Label fileLabel = new Label("No file selected");

        selectButton.setOnAction(e -> {

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open ASS Subtitle");

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("ASS subtitles", "*.ass"));

            File file = chooser.showOpenDialog(stage);

            if (file != null) {
                selectedAssFile = file;
                fileLabel.setText(file.getAbsolutePath());
            }

        });

        Label redactedLabel = new Label("Redacted File Name");

        TextField redactedField = new TextField();
        redactedField.setPrefWidth(250);

        Button directoryButton = new Button("Select Save Directory");

        Label directoryLabel = new Label("No directory selected");

        directoryButton.setOnAction(e -> {

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Output Folder");

            File dir = chooser.showDialog(stage);

            if (dir != null) {
                selectedDirectory = dir;
                directoryLabel.setText(dir.getAbsolutePath());
            }

        });

        Label progressLabel = new Label("Progress: 0%");
        Label statusLabel = new Label("Ready");
        Label runModeLabel = new Label("Run Type");
        Label gpuModeLabel = new Label("GPU (if you have CUDA)");

        Button fastModeButton = new Button("Fast  High Cpu Usage");
        Button coolModeButton = new Button("Cool  Balanced Cpu Usage");
        fastModeButton.setPrefWidth(220);
        coolModeButton.setPrefWidth(220);
        updateModeButtonStyles(fastModeButton, coolModeButton);

        fastModeButton.setOnAction(e -> {
            selectedRunMode = FixerAI.RunMode.FAST;
            updateModeButtonStyles(fastModeButton, coolModeButton);
        });

        coolModeButton.setOnAction(e -> {
            selectedRunMode = FixerAI.RunMode.COOL;
            updateModeButtonStyles(fastModeButton, coolModeButton);
        });

        HBox runModeBox = new HBox(10, fastModeButton, coolModeButton);

        Button useGpuButton = new Button("Use GPU");
        Button dontUseGpuButton = new Button("Do Not Use GPU");
        useGpuButton.setPrefWidth(220);
        dontUseGpuButton.setPrefWidth(220);
        updateGpuButtonStyles(useGpuButton, dontUseGpuButton);

        useGpuButton.setOnAction(e -> {
            selectedGpuMode = FixerAI.GpuMode.USE_GPU;
            updateGpuButtonStyles(useGpuButton, dontUseGpuButton);
        });

        dontUseGpuButton.setOnAction(e -> {
            selectedGpuMode = FixerAI.GpuMode.DONT_USE_GPU;
            updateGpuButtonStyles(useGpuButton, dontUseGpuButton);
        });

        HBox gpuModeBox = new HBox(10, useGpuButton, dontUseGpuButton);

        Button startButton = new Button("Start Redaction");

        startButton.setOnAction(e -> {

            if (selectedAssFile == null || selectedDirectory == null) {
                showError("Please select both an input subtitle and an output folder.");
                return;
            }

            String outputName = redactedField.getText().trim();

            if (outputName.isEmpty()) {
                showError("Please enter a file name for the redacted subtitle.");
                return;
            }

            if (outputName.matches(".*" + INVALID_FILE_NAME_CHARS + ".*")) {
                showError("The output file name contains invalid characters.");
                return;
            }

            String outputPath =
                    new File(selectedDirectory, outputName + ".ass").getAbsolutePath();

            startButton.setDisable(true);
            progressLabel.setText("Progress: 0%");
            statusLabel.setText("Processing...");

            workerThread = new Thread(() -> {

                try {
                    LinePicker picker = new LinePicker();

                    picker.processFile(
                            selectedAssFile.getAbsolutePath(),
                            outputPath,
                            (refinedLines, totalLines, percent) -> Platform.runLater(() ->
                                    progressLabel.setText(
                                            String.format("Progress: %d%% (%d/%d)", (int) percent, refinedLines, totalLines)
                                    )
                            ),
                            selectedRunMode,
                            selectedGpuMode
                    );

                    Platform.runLater(() -> {
                        progressLabel.setText("Progress: 100%");
                        statusLabel.setText("Completed: " + outputPath);
                        startButton.setDisable(false);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();

                    Platform.runLater(() -> {
                        statusLabel.setText("Failed");
                        startButton.setDisable(false);
                        showError("Redaction failed: " + ex.getMessage());
                    });
                }
            });

            workerThread.setName("subtitle-redactor-worker");
            workerThread.setDaemon(true);
            workerThread.start();
        });

        root.getChildren().addAll(
                topBar,
                selectButton,
                fileLabel,
                redactedLabel,
                redactedField,
                directoryButton,
                directoryLabel,
                runModeLabel,
                runModeBox,
                gpuModeLabel,
                gpuModeBox,
                startButton,
                progressLabel,
                statusLabel);

        Scene scene = new Scene(root, 600, 700);

        stage.setTitle("ASS SUBTITLE REDACTOR");
        stage.setScene(scene);
        stage.show();
    }

    private void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Subtitle Redactor");
        alert.setHeaderText("Operation could not be completed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateModeButtonStyles(Button fastModeButton, Button coolModeButton) {
        fastModeButton.setStyle(selectedRunMode == FixerAI.RunMode.FAST ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);
        coolModeButton.setStyle(selectedRunMode == FixerAI.RunMode.COOL ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);
    }

    private void updateGpuButtonStyles(Button useGpuButton, Button dontUseGpuButton) {
        useGpuButton.setStyle(selectedGpuMode == FixerAI.GpuMode.USE_GPU ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);
        dontUseGpuButton.setStyle(selectedGpuMode == FixerAI.GpuMode.DONT_USE_GPU ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);
    }
}

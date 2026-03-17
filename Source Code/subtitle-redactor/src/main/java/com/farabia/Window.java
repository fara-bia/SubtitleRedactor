package com.farabia;

import java.io.File;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Window {

    private enum Language {
        ENGLISH,
        TURKCE
    }

    private static final String INVALID_FILE_NAME_CHARS = "[\\\\/:*?\"<>|]";
    private static final String MODE_SELECTED_STYLE =
            "-fx-background-color: #0f766e; -fx-text-fill: white; -fx-font-weight: bold;";
    private static final String MODE_UNSELECTED_STYLE =
            "-fx-background-color: #d1fae5; -fx-text-fill: #134e4a;";
    private static final String PROMPT_ENABLED_STYLE =
            "-fx-control-inner-background: white; -fx-text-fill: #0f172a;";
    private static final String PROMPT_DISABLED_STYLE =
            "-fx-control-inner-background: #94a3b8; -fx-text-fill: #334155;";
    private static final String SIGNATURE_STYLE =
            "-fx-font-size: 10px; -fx-text-fill: #0f172a;";

    private File selectedAssFile;
    private File selectedDirectory;
    private Thread workerThread;
    private FixerAI.RunMode selectedRunMode = FixerAI.RunMode.COOL;
    private FixerAI.GpuMode selectedGpuMode = FixerAI.GpuMode.DONT_USE_GPU;
    private Language selectedLanguage = Language.ENGLISH;

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
        root.setBackground(new Background(new BackgroundFill(Color.AQUAMARINE, null, null)));

        Button englishButton = new Button("ENGLISH");
        Button turkceButton = new Button("TÜRKÇE");
        englishButton.setPrefHeight(28);
        turkceButton.setPrefHeight(28);
        englishButton.setMaxWidth(Double.MAX_VALUE);
        turkceButton.setMaxWidth(Double.MAX_VALUE);

        HBox languageBar = new HBox(8, englishButton, turkceButton);
        languageBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(englishButton, Priority.ALWAYS);
        HBox.setHgrow(turkceButton, Priority.ALWAYS);

        Button selectButton = new Button();
        selectButton.setPrefWidth(220);
        selectButton.setPrefHeight(60);

        Label fileLabel = new Label();
        Label redactedLabel = new Label();
        TextField redactedField = new TextField();
        redactedField.setPrefWidth(250);

        Button directoryButton = new Button();
        Label directoryLabel = new Label();

        Label progressLabel = new Label();
        Label statusLabel = new Label();
        Label runModeLabel = new Label();
        Label gpuModeLabel = new Label();
        Label promptLabel = new Label();

        Button fastModeButton = new Button();
        Button coolModeButton = new Button();
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

        Button useGpuButton = new Button();
        Button dontUseGpuButton = new Button();
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

        TextArea promptArea = new TextArea();
        promptArea.setWrapText(true);
        promptArea.setPrefRowCount(6);
        promptArea.setStyle(PROMPT_ENABLED_STYLE);

        Button defaultPromptButton = new Button();
        defaultPromptButton.setPrefHeight(36);

        HBox promptBox = new HBox(10, promptArea, defaultPromptButton);
        promptBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(promptArea, Priority.ALWAYS);

        Button startButton = new Button();

        Label signatureLabel = new Label("by Farabia");
        signatureLabel.setStyle(SIGNATURE_STYLE);
        Region signatureSpacer = new Region();
        HBox.setHgrow(signatureSpacer, Priority.ALWAYS);
        HBox footerBox = new HBox(signatureSpacer, signatureLabel);
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        englishButton.setOnAction(e -> {
            Language previousLanguage = selectedLanguage;
            selectedLanguage = Language.ENGLISH;
            refreshLanguage(
                    previousLanguage,
                    englishButton,
                    turkceButton,
                    selectButton,
                    fileLabel,
                    redactedLabel,
                    directoryButton,
                    directoryLabel,
                    progressLabel,
                    statusLabel,
                    runModeLabel,
                    gpuModeLabel,
                    promptLabel,
                    fastModeButton,
                    coolModeButton,
                    useGpuButton,
                    dontUseGpuButton,
                    defaultPromptButton,
                    startButton,
                    promptArea);
        });

        turkceButton.setOnAction(e -> {
            Language previousLanguage = selectedLanguage;
            selectedLanguage = Language.TURKCE;
            refreshLanguage(
                    previousLanguage,
                    englishButton,
                    turkceButton,
                    selectButton,
                    fileLabel,
                    redactedLabel,
                    directoryButton,
                    directoryLabel,
                    progressLabel,
                    statusLabel,
                    runModeLabel,
                    gpuModeLabel,
                    promptLabel,
                    fastModeButton,
                    coolModeButton,
                    useGpuButton,
                    dontUseGpuButton,
                    defaultPromptButton,
                    startButton,
                    promptArea);
        });

        selectButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(isEnglish() ? "Open ASS Subtitle" : "ASS Altyazı Seç");
            if(selectedLanguage == Language.ENGLISH){
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASS Subtitles", "*.ass"));
            }
            else {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASS Dosyaları", "*.ass"));

            }

            File file = chooser.showOpenDialog(stage);

            if (file != null) {
                selectedAssFile = file;
                fileLabel.setText(file.getAbsolutePath());
            }
        });

        directoryButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(isEnglish() ? "Select Output Folder" : "Çıktı Klasörünü Seç");

            File dir = chooser.showDialog(stage);

            if (dir != null) {
                selectedDirectory = dir;
                directoryLabel.setText(dir.getAbsolutePath());
            }
        });

        defaultPromptButton.setOnAction(e -> promptArea.setText(FixerAI.getDefaultPrompt(isEnglish())));

        startButton.setOnAction(e -> {
            if (selectedAssFile == null || selectedDirectory == null) {
                showError(isEnglish()
                        ? "Please select both an input subtitle and an output folder."
                        : "Lütfen altyazı ve çıktı klasörünü seçin.");
                return;
            }

            String outputName = redactedField.getText().trim();
            if (outputName.isEmpty()) {
                showError(isEnglish()
                        ? "Please enter a file name for the redacted subtitle."
                        : "Lütfen redakte edilen altyazı için bir dosya adı girin.");
                return;
            }

            if (outputName.matches(".*" + INVALID_FILE_NAME_CHARS + ".*")) {
                showError(isEnglish()
                        ? "The output file name contains invalid characters."
                        : "Çıktı dosyası adı geçersiz karakterler içeriyor.");
                return;
            }

            String outputPath = new File(selectedDirectory, outputName + ".ass").getAbsolutePath();
            String promptText = promptArea.getText().isBlank()
                    ? FixerAI.getDefaultPrompt(isEnglish())
                    : promptArea.getText();

            startButton.setDisable(true);
            updatePromptInputState(promptArea, defaultPromptButton, true);
            progressLabel.setText(progressPrefix() + " 0%");
            statusLabel.setText(isEnglish() ? "Processing..." : "İşleniyor...");

            workerThread = new Thread(() -> {
                try {
                    LinePicker picker = new LinePicker();

                    picker.processFile(
                            selectedAssFile.getAbsolutePath(),
                            outputPath,
                            (refinedLines, totalLines, percent) -> Platform.runLater(() ->
                                    progressLabel.setText(String.format(
                                            "%s %d%% (%d/%d)",
                                            progressPrefix(),
                                            (int) percent,
                                            refinedLines,
                                            totalLines))),
                            promptText,
                            selectedRunMode,
                            selectedGpuMode);

                    Platform.runLater(() -> {
                        progressLabel.setText(progressPrefix() + " 100%");
                        statusLabel.setText((isEnglish() ? "Completed: " : "Tamamlandı: ") + outputPath);
                        startButton.setDisable(false);
                        updatePromptInputState(promptArea, defaultPromptButton, false);
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();

                    Platform.runLater(() -> {
                        statusLabel.setText(isEnglish() ? "Failed" : "Başarısız");
                        startButton.setDisable(false);
                        updatePromptInputState(promptArea, defaultPromptButton, false);
                        showError((isEnglish() ? "Redaction failed: " : "Redaksiyon başarısız: ") + ex.getMessage());
                    });
                }
            });

            workerThread.setName("subtitle-redactor-worker");
            workerThread.setDaemon(true);
            workerThread.start();
        });

        root.getChildren().addAll(
                languageBar,
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
                promptLabel,
                promptBox,
                startButton,
                progressLabel,
                statusLabel,
                footerBox);

        Scene scene = new Scene(root, 600, 700);
        promptArea.prefWidthProperty().bind(scene.widthProperty().multiply(0.75).subtract(55));
        defaultPromptButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25).subtract(55));

        refreshLanguage(
                null,
                englishButton,
                turkceButton,
                selectButton,
                fileLabel,
                redactedLabel,
                directoryButton,
                directoryLabel,
                progressLabel,
                statusLabel,
                runModeLabel,
                gpuModeLabel,
                promptLabel,
                fastModeButton,
                coolModeButton,
                useGpuButton,
                dontUseGpuButton,
                defaultPromptButton,
                startButton,
                promptArea);

        stage.setTitle("ASS SUBTITLE REDACTOR");
        stage.setScene(scene);
        stage.show();
    }

    private boolean isEnglish() {
        return selectedLanguage == Language.ENGLISH;
    }

    private String progressPrefix() {
        return isEnglish() ? "Progress:" : "İlerleme:";
    }

    private void refreshLanguage(
            Language previousLanguage,
            Button englishButton,
            Button turkceButton,
            Button selectButton,
            Label fileLabel,
            Label redactedLabel,
            Button directoryButton,
            Label directoryLabel,
            Label progressLabel,
            Label statusLabel,
            Label runModeLabel,
            Label gpuModeLabel,
            Label promptLabel,
            Button fastModeButton,
            Button coolModeButton,
            Button useGpuButton,
            Button dontUseGpuButton,
            Button defaultPromptButton,
            Button startButton,
            TextArea promptArea) {

        englishButton.setStyle(selectedLanguage == Language.ENGLISH ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);
        turkceButton.setStyle(selectedLanguage == Language.TURKCE ? MODE_SELECTED_STYLE : MODE_UNSELECTED_STYLE);

        selectButton.setText(isEnglish() ? "Select .ASS File" : ".ASS Dosyası Seç");
        redactedLabel.setText(isEnglish() ? "Redacted File Name" : "Redakte Dosya Adı");
        directoryButton.setText(isEnglish() ? "Select Save Directory" : "Kayıt Klasörünü Seç");
        runModeLabel.setText(isEnglish() ? "Run Type" : "Çalışma Türü");
        gpuModeLabel.setText(isEnglish() ? "GPU (if you have CUDA)" : "GPU (CUDA varsa)");
        promptLabel.setText(isEnglish() ? "AI Prompt:" : "AI Komutu:");
        fastModeButton.setText(isEnglish() ? "Fast  High CPU Usage" : "Hızlı  Yüksek CPU Kullanımı");
        coolModeButton.setText(isEnglish() ? "Cool  Balanced CPU Usage" : "Serin  Dengeli CPU Kullanımı");
        useGpuButton.setText(isEnglish() ? "Use GPU" : "GPU Kullan");
        dontUseGpuButton.setText(isEnglish() ? "Do Not Use GPU" : "GPU Kullanma");
        defaultPromptButton.setText(isEnglish() ? "Use Default Prompt" : "Varsayılan Komutu Kullan");
        startButton.setText(isEnglish() ? "Start Redaction" : "Redaksiyonu Başlat");

        if (selectedAssFile == null) {
            fileLabel.setText(isEnglish() ? "No file selected" : "Dosya seçilmedi");
        }

        if (selectedDirectory == null) {
            directoryLabel.setText(isEnglish() ? "No directory selected" : "Klasör seçilmedi");
        }

        if (statusLabel.getText().isBlank()
                || statusLabel.getText().equals("Ready")
                || statusLabel.getText().equals("Hazır")) {
            statusLabel.setText(isEnglish() ? "Ready" : "Hazır");
        }

        if (progressLabel.getText().isBlank()
                || progressLabel.getText().startsWith("Progress:")
                || progressLabel.getText().startsWith("İlerleme:")) {
            progressLabel.setText(progressPrefix() + " 0%");
        }

        if (previousLanguage != null
                && promptArea.getText().equals(FixerAI.getDefaultPrompt(previousLanguage == Language.ENGLISH))) {
            promptArea.setText(FixerAI.getDefaultPrompt(isEnglish()));
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Subtitle Redactor");
        alert.setHeaderText(isEnglish() ? "Operation could not be completed" : "İşlem tamamlanamadı");
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

    private void updatePromptInputState(TextArea promptArea, Button defaultPromptButton, boolean processing) {
        promptArea.setDisable(processing);
        defaultPromptButton.setDisable(processing);
        promptArea.setStyle(processing ? PROMPT_DISABLED_STYLE : PROMPT_ENABLED_STYLE);
    }
}

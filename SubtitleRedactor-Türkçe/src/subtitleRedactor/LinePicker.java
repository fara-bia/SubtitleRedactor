package subtitleRedactor;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LinePicker {

    private static final int MAX_BATCH_LINES = 10;
    private static final int MAX_BATCH_CHARACTERS = 1800;

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int refinedLines, int totalLines, double percent);
    }

    private int refinedLines = 0;
    private int totalLines = 0;

    public int getRefinedLines() {
        return refinedLines;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public double getProgressPercent() {
        if (totalLines == 0) return 0;
        return ((double) refinedLines / totalLines) * 100;
    }

    public void processFile(String inputPath, String outputPath) throws IOException {
        processFile(inputPath, outputPath, null, FixerAI.RunMode.COOL, FixerAI.GpuMode.DONT_USE_GPU);
    }

    public void processFile(String inputPath, String outputPath, ProgressListener listener) throws IOException {
        processFile(inputPath, outputPath, listener, FixerAI.RunMode.COOL, FixerAI.GpuMode.DONT_USE_GPU);
    }

    public void processFile(
            String inputPath,
            String outputPath,
            ProgressListener listener,
            FixerAI.RunMode runMode,
            FixerAI.GpuMode gpuMode) throws IOException {

        totalLines = 0;
        try (BufferedReader reader = Files.newBufferedReader(Path.of(inputPath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Dialogue:")) {
                    totalLines++;
                }
            }
        }

        refinedLines = 0;
        FixerAI ai = new FixerAI();

        try (BufferedReader reader = Files.newBufferedReader(Path.of(inputPath), StandardCharsets.UTF_8);
             FileWriter writer = new FileWriter(outputPath)) {

            String line;
            List<String> metaBatch = new ArrayList<>();
            List<String> textBatch = new ArrayList<>();
            int currentBatchCharacters = 0;

            while ((line = reader.readLine()) != null) {

                if (line.startsWith("Dialogue:")) {
                    int commaCount = 0;
                    int splitIndex = -1;

                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == ',') {
                            commaCount++;
                            if (commaCount == 9) {
                                splitIndex = i + 1;
                                break;
                            }
                        }
                    }

                    if (splitIndex != -1) {
                        String metadata = line.substring(0, splitIndex);
                        String text = line.substring(splitIndex);

                        metaBatch.add(metadata);
                        textBatch.add(text);
                        currentBatchCharacters += text.length();

                        if (textBatch.size() >= MAX_BATCH_LINES
                                || currentBatchCharacters >= MAX_BATCH_CHARACTERS) {
                            flushBatch(metaBatch, textBatch, writer, ai, listener, runMode, gpuMode);
                            currentBatchCharacters = 0;
                        }
                    } else {
                        flushBatch(metaBatch, textBatch, writer, ai, listener, runMode, gpuMode);
                        currentBatchCharacters = 0;
                        writer.writeOnFile(line + "\n");
                    }
                } else {
                    flushBatch(metaBatch, textBatch, writer, ai, listener, runMode, gpuMode);
                    currentBatchCharacters = 0;
                    writer.writeOnFile(line + "\n");
                }
            }

            flushBatch(metaBatch, textBatch, writer, ai, listener, runMode, gpuMode);
        }
    }

    private void flushBatch(
            List<String> metaBatch,
            List<String> textBatch,
            FileWriter writer,
            FixerAI ai,
            ProgressListener listener,
            FixerAI.RunMode runMode,
            FixerAI.GpuMode gpuMode) throws IOException {
        if (textBatch.isEmpty()) {
            return;
        }

        List<String> fixedTexts = ai.fixLines(textBatch, listener, refinedLines, totalLines, runMode, gpuMode);

        for (int i = 0; i < metaBatch.size(); i++) {
            String meta = metaBatch.get(i);
            String text = fixedTexts.get(i);
            writer.writeOnFile(meta + text + "\n");
        }
        
        refinedLines += textBatch.size();

        // Catch-up update at the end of the batch
        if (listener != null) {
            listener.onProgress(refinedLines, totalLines, getProgressPercent());
        }

        metaBatch.clear();
        textBatch.clear();
    }
}

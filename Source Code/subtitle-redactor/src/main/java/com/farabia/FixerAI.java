package com.farabia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixerAI {

    public static final String DEFAULT_PROMPT_TR = String.join("\n",
            "Sen hassas bir dil uzmanısın. Amacın, metnin monosamik (tekil) anlamını ve yazarın orijinal kelime dağarcığını KESİNLİKLE korumaktır. Yalnızca mekanik imla hatalarını, harf yutulmalarını ve Türkçe karakter eksikliklerini düzelt.",
            "- Asla eş anlamlı kelime atama.",
            "- Asla kelime ekleme veya çıkarma.",
            "- Tembellik yapma, tüm satırları eksiksiz işle.",
            "- SADECE 'Numara|Düzeltilmiş Metin' formatında yanıt ver.",
            "- Harf değişimlerini (w->r, z->s vb.) ve yutulan harfleri (meraba->merhaba) standart imlaya çevir.",
            "- Örnek: 'kawdesim' her zaman 'kardeşim' olarak düzeltilmelidir.");

    public static final String DEFAULT_PROMPT_EN = String.join("\n",
            "You are a meticulous language editor. Your goal is to preserve the exact meaning of the text and the author's original wording while fixing only mechanical spelling mistakes, dropped letters, and missing Turkish characters.",
            "- Never replace words with synonyms.",
            "- Never add or remove words.",
            "- Process every line completely without skipping.",
            "- Reply ONLY in the format 'Number|Corrected Text'.",
            "- Convert letter substitutions (such as w->r, z->s) and dropped letters (such as meraba->merhaba) into standard spelling.",
            "- Example: 'kawdesim' must always be corrected to 'kardeşim'.");

    public static final String DEFAULT_PROMPT = DEFAULT_PROMPT_TR;

    public enum RunMode {
        FAST(0),
        COOL(6);

        private final int threadLimit;

        RunMode(int threadLimit) {
            this.threadLimit = threadLimit;
        }

        public int getThreadLimit() {
            return threadLimit;
        }
    }

    public enum GpuMode {
        USE_GPU,
        DONT_USE_GPU
    }

    private static final boolean DEBUG_LOG_ENABLED = true;
    private static final String LINE_BREAK_TOKEN = "<<<LB>>>";
    private static final int MAX_PREDICT_TOKENS = 2048;
    private static final Object PROCESS_LOCK = new Object();
    private static final List<Process> ACTIVE_PROCESSES = new ArrayList<>();
    private static Boolean gpuBackendAvailable;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(FixerAI::destroyActiveProcesses, "llama-cleanup-shutdown-hook"));
    }

    private final File projectDirectory;
    private final File llamaCliFile;
    private final File modelFile;
    private final File logFile;

    public FixerAI() {
        projectDirectory = new File(System.getProperty("user.dir"));
        llamaCliFile = projectDirectory.toPath().resolve("ai").resolve("llama-cli.exe").toFile();
        modelFile = findModelFile(projectDirectory.toPath().resolve("ai"));

        logFile = new File(projectDirectory, "ai_debug_log.txt");
        initializeLog();
    }

    public static String getDefaultPrompt(boolean englishSelected) {
        return englishSelected ? DEFAULT_PROMPT_EN : DEFAULT_PROMPT_TR;
    }

    private void initializeLog() {
        if (!DEBUG_LOG_ENABLED) {
            return;
        }

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            } else {
                logFile.delete();
                logFile.createNewFile();
            }
            writeToLog("=== AI Redactor Log Initialized at "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " ===\n");
        } catch (IOException e) {
            System.err.println("Could not create log file: " + e.getMessage());
        }
    }

    private synchronized void writeToLog(String text) {
        if (!DEBUG_LOG_ENABLED) {
            return;
        }

        if (logFile != null && logFile.exists()) {
            try {
                Files.writeString(logFile.toPath(), text + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
            }
        }
    }

    public List<String> fixLines(
            List<String> subtitleTexts,
            LinePicker.ProgressListener listener,
            int baseRefinedCount,
            int totalLines,
            String prompt,
            RunMode runMode,
            GpuMode gpuMode) {
        List<String> results = new ArrayList<>(Collections.nCopies(subtitleTexts.size(), null));
        File tempPromptFile = null;
        Process process = null;

        try {
            if (!llamaCliFile.isFile() || modelFile == null) {
                writeToLog("ERROR: llama-cli.exe or .gguf model not found. Aborting batch.");
                return fallback(subtitleTexts);
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("<|im_start|>system\n")
                    .append(resolvePrompt(prompt))
                    .append("\n<|im_end|>\n")
                    .append("<|im_start|>user\n");

            for (int i = 0; i < subtitleTexts.size(); i++) {
                String clean = subtitleTexts.get(i).trim();
                clean = clean.replace("\\N", LINE_BREAK_TOKEN);
                promptBuilder.append(i).append("|").append(clean).append("\n");
            }

            promptBuilder.append("<|im_end|>\n<|im_start|>assistant\n");

            tempPromptFile = File.createTempFile("llama_prompt_", ".txt");
            Files.writeString(tempPromptFile.toPath(), promptBuilder.toString(), StandardCharsets.UTF_8);

            writeToLog("\n\n========== BATCH START ==========");
            writeToLog("--- PROMPT SENT TO AI (Via Temp File) ---");
            writeToLog(promptBuilder.toString());
            writeToLog("--- RAW AI OUTPUT STREAM ---");

            int threadCount = getThreadLimit(runMode);
            writeToLog("Using run mode: " + runMode.name());
            writeToLog("Using llama-cli thread limit: " + (threadCount == 0 ? "auto" : threadCount));
            writeToLog("Requested GPU mode: " + gpuMode.name());

            ProcessBuilder builder = new ProcessBuilder(
                    llamaCliFile.getAbsolutePath(),
                    "-m", modelFile.getAbsolutePath(),
                    "--simple-io",
                    "--single-turn",
                    "--no-display-prompt",
                    "--no-show-timings",
                    "--temp", "0",
                    "--seed", "0",
                    "-c", "4096",
                    "-n", String.valueOf(MAX_PREDICT_TOKENS),
                    "-f", tempPromptFile.getAbsolutePath()
            );

            if (threadCount > 0) {
                builder.command().add("-t");
                builder.command().add(String.valueOf(threadCount));
                builder.command().add("-tb");
                builder.command().add(String.valueOf(threadCount));
            }

            if (gpuMode == GpuMode.USE_GPU) {
                if (isGpuBackendAvailable()) {
                    builder.command().add("--gpu-layers");
                    builder.command().add("auto");
                    writeToLog("GPU backend detected. Enabling GPU layers.");
                } else {
                    writeToLog("GPU requested, but no GPU backend was detected. Falling back to CPU.");
                }
            }

            builder.directory(projectDirectory);
            builder.redirectErrorStream(true);

            process = builder.start();
            registerProcess(process);

            Pattern pattern = Pattern.compile("^\\s*(\\d+)\\s*[|\\-:\\.]\\s*(.*)");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        writeToLog("Worker thread interrupted. Destroying llama-cli process.");
                        destroyProcess(process);
                        break;
                    }

                    writeToLog(line);

                    Matcher m = pattern.matcher(line);

                    if (m.find()) {
                        try {
                            int id = Integer.parseInt(m.group(1));

                            if (id >= 0 && id < results.size()) {
                                String text = m.group(2).trim();
                                text = text.replace("<<< LB >>>", LINE_BREAK_TOKEN)
                                        .replace("<<< LB>>>", LINE_BREAK_TOKEN)
                                        .replace("<<<LB >>>", LINE_BREAK_TOKEN);
                                text = text.replace(LINE_BREAK_TOKEN, "\\N");

                                if (results.get(id) == null && listener != null) {
                                    int currentTotal = baseRefinedCount + countResolved(results) + 1;
                                    double pct = ((double) currentTotal / totalLines) * 100.0;
                                    listener.onProgress(currentTotal, totalLines, pct);
                                }

                                results.set(id, text);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            process.waitFor();
            writeToLog("llama-cli exit code: " + process.exitValue());
            writeToLog("========== BATCH END ==========\n");

        } catch (Exception e) {
            writeToLog("EXCEPTION CAUGHT: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanupProcess(process);

            if (tempPromptFile != null && tempPromptFile.exists()) {
                tempPromptFile.delete();
            }
        }

        for (int i = 0; i < results.size(); i++) {
            if (results.get(i) == null || results.get(i).isBlank()) {
                writeToLog("WARNING: Line ID " + i + " was dropped or skipped by AI. Falling back to original text.");
                results.set(i, subtitleTexts.get(i));
            }
        }

        return results;
    }

    private File findModelFile(Path aiDirectory) {
        File aiDir = aiDirectory.toFile();
        if (aiDir.exists() && aiDir.isDirectory()) {
            File[] ggufFiles = aiDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".gguf"));
            if (ggufFiles != null && ggufFiles.length > 0) {
                return ggufFiles[0];
            }
        }
        return null;
    }

    private List<String> fallback(List<String> texts) {
        return new ArrayList<>(texts);
    }

    private String resolvePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return DEFAULT_PROMPT;
        }

        return prompt.strip();
    }

    public static void destroyActiveProcesses() {
        List<Process> snapshot;

        synchronized (PROCESS_LOCK) {
            snapshot = new ArrayList<>(ACTIVE_PROCESSES);
            ACTIVE_PROCESSES.clear();
        }

        for (Process process : snapshot) {
            destroyProcess(process);
        }
    }

    private static void registerProcess(Process process) {
        synchronized (PROCESS_LOCK) {
            ACTIVE_PROCESSES.add(process);
        }
    }

    private static void cleanupProcess(Process process) {
        if (process == null) {
            return;
        }

        synchronized (PROCESS_LOCK) {
            ACTIVE_PROCESSES.remove(process);
        }

        if (process.isAlive()) {
            destroyProcess(process);
        }
    }

    private static void destroyProcess(Process process) {
        long pid = process.pid();

        if (isWindows()) {
            killWindowsProcessTree(pid);
        }

        process.descendants().forEach(child -> child.destroyForcibly());
        process.destroy();

        try {
            if (!process.waitFor(800, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (process.isAlive()) {
            process.destroyForcibly();

            try {
                process.waitFor(800, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void killWindowsProcessTree(long pid) {
        try {
            Process killer = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid), "/T", "/F")
                    .redirectErrorStream(true)
                    .start();
            killer.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
        }
    }

    private int getThreadLimit(RunMode runMode) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int configuredLimit = runMode.getThreadLimit();

        if (configuredLimit <= 0) {
            return 0;
        }

        return Math.max(1, Math.min(configuredLimit, availableProcessors));
    }

    private int countResolved(List<String> results) {
        int count = 0;

        for (String result : results) {
            if (result != null) {
                count++;
            }
        }

        return count;
    }

    public synchronized boolean isGpuBackendAvailable() {
        if (gpuBackendAvailable != null) {
            return gpuBackendAvailable.booleanValue();
        }

        gpuBackendAvailable = Boolean.FALSE;

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    llamaCliFile.getAbsolutePath(),
                    "--list-devices"
            );
            builder.directory(projectDirectory);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase();

                    if (lower.contains("cuda")
                            || lower.contains("vulkan")
                            || lower.contains("metal")
                            || lower.contains("opencl")) {
                        gpuBackendAvailable = Boolean.TRUE;
                    }
                }
            }

            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            writeToLog("GPU detection failed: " + e.getMessage());
        }

        return gpuBackendAvailable.booleanValue();
    }
}

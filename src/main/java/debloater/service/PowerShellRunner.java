package debloater.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import debloater.model.ActionExecutionResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Executes a generated PowerShell script using ProcessBuilder.
 * Runs in a background thread to avoid freezing the JavaFX UI.
 *
 * Reads stdout line by line:
 * - Lines prefixed with ACTION_RESULT: are parsed as structured JSON results.
 * - All other lines are passed to the raw log callback.
 *
 * stderr is captured and appended to the raw log.
 */
public class PowerShellRunner {

    private static final String ACTION_RESULT_PREFIX = "ACTION_RESULT:";
    private static final Gson gson = new Gson();

    // Callbacks set by the caller (AppBridge) before execution
    private Consumer<String> onRawLogLine;
    private Consumer<ActionExecutionResult> onActionResult;
    private Runnable onComplete;
    private Consumer<String> onError;

    // Collected results for final report
    private final List<ActionExecutionResult> results = new ArrayList<>();
    private final StringBuilder fullLog = new StringBuilder();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void setOnRawLogLine(Consumer<String> callback) { this.onRawLogLine = callback; }
    public void setOnActionResult(Consumer<ActionExecutionResult> callback) { this.onActionResult = callback; }
    public void setOnComplete(Runnable callback) { this.onComplete = callback; }
    public void setOnError(Consumer<String> callback) { this.onError = callback; }

    public List<ActionExecutionResult> getResults() { return results; }
    public String getFullLog() { return fullLog.toString(); }
    public boolean isRunning() { return running.get(); }

    /**
     * Executes the given PowerShell script content.
     * Writes the script to a temp file, then runs it via powershell.exe.
     * This method runs on a background thread — do not call from the FX thread.
     */
    public void execute(String scriptContent) {
        results.clear();
        fullLog.setLength(0);
        running.set(true);

        Path tempScript = null;
        try {
            // Write the generated script to a temporary .ps1 file
            tempScript = Files.createTempFile("debloater_", ".ps1");
            Files.writeString(tempScript, scriptContent, StandardCharsets.UTF_8);

            logLine("[INFO] Generated script written to: " + tempScript.toAbsolutePath());
            logLine("[INFO] Starting PowerShell execution...");
            logLine("");

            // Build the process: powershell.exe -NoProfile -ExecutionPolicy Bypass -File <script>
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", tempScript.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(false); // Keep stderr separate

            Process process = pb.start();

            // Read stdout in this thread
            Thread stderrThread = new Thread(() -> readStderr(process), "ps-stderr-reader");
            stderrThread.setDaemon(true);
            stderrThread.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processStdoutLine(line);
                }
            }

            // Wait for stderr thread and process to finish
            stderrThread.join(5000);
            int exitCode = process.waitFor();

            logLine("");
            logLine("[INFO] PowerShell exited with code: " + exitCode);

            if (exitCode != 0 && onError != null) {
                onError.accept("PowerShell exited with code: " + exitCode);
            }

        } catch (Exception e) {
            String errorMsg = "Execution error: " + e.getMessage();
            logLine("[ERROR] " + errorMsg);
            if (onError != null) {
                onError.accept(errorMsg);
            }
        } finally {
            running.set(false);
            // Clean up temp file
            if (tempScript != null) {
                try { Files.deleteIfExists(tempScript); } catch (IOException ignored) {}
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    /**
     * Processes a single line from stdout.
     * Detects ACTION_RESULT: prefixed lines and parses them as JSON.
     */
    private void processStdoutLine(String line) {
        if (line.startsWith(ACTION_RESULT_PREFIX)) {
            String json = line.substring(ACTION_RESULT_PREFIX.length());
            try {
                ActionExecutionResult result = gson.fromJson(json, ActionExecutionResult.class);
                if (result != null && result.getId() != null) {
                    results.add(result);
                    logLine("[RESULT] " + result.getStatus() + ": " + result.getTitle());
                    if (onActionResult != null) {
                        onActionResult.accept(result);
                    }
                    return;
                }
            } catch (JsonSyntaxException e) {
                logLine("[WARN] Failed to parse action result JSON: " + e.getMessage());
            }
        }
        // Regular output line
        logLine(line);
    }

    /**
     * Reads stderr from the process and logs each line.
     */
    private void readStderr(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logLine("[STDERR] " + line);
            }
        } catch (IOException e) {
            logLine("[ERROR] Failed to read stderr: " + e.getMessage());
        }
    }

    /**
     * Logs a line to both the internal buffer and the raw log callback.
     */
    private void logLine(String line) {
        fullLog.append(line).append(System.lineSeparator());
        if (onRawLogLine != null) {
            onRawLogLine.accept(line);
        }
    }
}

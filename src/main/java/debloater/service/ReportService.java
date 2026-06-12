package debloater.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import debloater.model.ActionExecutionResult;
import debloater.model.ExecutionSummary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates structured reports from execution results.
 * Supports JSON and plain text formats.
 * Groups results by category for readable output.
 */
public class ReportService {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Builds an ExecutionSummary from a list of results.
     */
    public ExecutionSummary buildSummary(List<ActionExecutionResult> results) {
        int success = 0, failed = 0, skipped = 0, partial = 0, alreadyApplied = 0;
        boolean restartRecommended = false;

        for (ActionExecutionResult r : results) {
            switch (r.getStatus()) {
                case "Success" -> success++;
                case "Failed" -> failed++;
                case "Skipped" -> skipped++;
                case "Partially completed" -> partial++;
                case "Already applied" -> alreadyApplied++;
            }
            if (r.isRestartRecommended()) {
                restartRecommended = true;
            }
        }

        return new ExecutionSummary(
            results.size(), success, failed, skipped, partial, alreadyApplied, restartRecommended
        );
    }

    /**
     * Generates a JSON report containing the summary and all results.
     */
    public String generateJsonReport(List<ActionExecutionResult> results) {
        ExecutionSummary summary = buildSummary(results);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", summary);
        report.put("results", results);
        return gson.toJson(report);
    }

    /**
     * Generates a plain text report grouped by category.
     */
    public String generateTextReport(List<ActionExecutionResult> results) {
        ExecutionSummary summary = buildSummary(results);
        StringBuilder sb = new StringBuilder();

        sb.append("============================================================\n");
        sb.append("  Windows 11 Debloater — Execution Report\n");
        sb.append("============================================================\n\n");

        // Summary section
        sb.append("SUMMARY\n");
        sb.append("-------\n");
        sb.append(String.format("  Total actions:      %d\n", summary.getTotal()));
        sb.append(String.format("  Successful:         %d\n", summary.getSuccessCount()));
        sb.append(String.format("  Failed:             %d\n", summary.getFailedCount()));
        sb.append(String.format("  Skipped:            %d\n", summary.getSkippedCount()));
        sb.append(String.format("  Partially completed:%d\n", summary.getPartialCount()));
        sb.append(String.format("  Already applied:    %d\n", summary.getAlreadyAppliedCount()));
        sb.append(String.format("  Restart recommended:%s\n", summary.isRestartRecommended() ? "Yes" : "No"));
        sb.append("\n");

        // Group results by category
        Map<String, List<ActionExecutionResult>> byCategory = results.stream()
            .collect(Collectors.groupingBy(
                ActionExecutionResult::getCategory,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        for (Map.Entry<String, List<ActionExecutionResult>> entry : byCategory.entrySet()) {
            sb.append("------------------------------------------------------------\n");
            sb.append("  ").append(entry.getKey()).append("\n");
            sb.append("------------------------------------------------------------\n");

            for (ActionExecutionResult r : entry.getValue()) {
                String icon = switch (r.getStatus()) {
                    case "Success" -> "[OK]";
                    case "Failed" -> "[FAIL]";
                    case "Skipped" -> "[SKIP]";
                    case "Partially completed" -> "[PARTIAL]";
                    case "Already applied" -> "[APPLIED]";
                    default -> "[?]";
                };

                sb.append(String.format("\n  %s %s\n", icon, r.getTitle()));
                sb.append(String.format("    Status:  %s\n", r.getStatus()));
                sb.append(String.format("    Message: %s\n", r.getMessage()));
                if (r.getDetails() != null && !r.getDetails().isEmpty()) {
                    sb.append(String.format("    Details: %s\n", r.getDetails()));
                }
                if (r.getError() != null && !r.getError().isEmpty()) {
                    sb.append(String.format("    Error:   %s\n", r.getError()));
                }
            }
            sb.append("\n");
        }

        // Footer notes
        if (summary.isRestartRecommended()) {
            sb.append("============================================================\n");
            sb.append("  Restart recommended for all changes to fully apply.\n");
            sb.append("  Major Windows feature updates may restore some of these\n");
            sb.append("  settings. Re-run the selected profile after major Windows\n");
            sb.append("  updates if needed.\n");
            sb.append("============================================================\n");
        }

        return sb.toString();
    }

    /**
     * Saves a JSON report to a file.
     */
    public void saveJsonReport(List<ActionExecutionResult> results, Path filePath) throws IOException {
        Files.writeString(filePath, generateJsonReport(results), StandardCharsets.UTF_8);
    }

    /**
     * Saves a text report to a file.
     */
    public void saveTextReport(List<ActionExecutionResult> results, Path filePath) throws IOException {
        Files.writeString(filePath, generateTextReport(results), StandardCharsets.UTF_8);
    }

    /**
     * Saves the raw execution log to a file.
     */
    public void saveLog(String logContent, Path filePath) throws IOException {
        Files.writeString(filePath, logContent, StandardCharsets.UTF_8);
    }
}

package debloater.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import debloater.model.ActionExecutionResult;
import debloater.model.DebloatAction;
import debloater.model.ExecutionSummary;
import debloater.service.*;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Java-to-JavaScript bridge exposed to the WebView.
 * This is the ONLY interface between the HTML/JS UI and Java backend.
 *
 * SECURITY RULES:
 * - JavaScript only sends action IDs (strings), never raw PowerShell.
 * - Java validates ALL received IDs against the ActionRegistry.
 * - Unknown IDs are rejected and logged.
 * - No arbitrary command execution from JavaScript.
 * - All public methods here are callable from JavaScript via window.bridge.
 */
public class AppBridge {

    private final ActionRegistry registry;
    private final PowerShellScriptBuilder scriptBuilder;
    private final PowerShellRunner runner;
    private final ProfileService profileService;
    private final ReportService reportService;
    private final Gson gson;

    private final Stage stage;
    private WebEngine webEngine;

    public AppBridge(Stage stage) {
        this.stage = stage;
        this.registry = new ActionRegistry();
        this.scriptBuilder = new PowerShellScriptBuilder();
        this.runner = new PowerShellRunner();
        this.profileService = new ProfileService(registry);
        this.reportService = new ReportService();
        this.gson = new GsonBuilder().create();
    }

    public void setWebEngine(WebEngine webEngine) {
        this.webEngine = webEngine;
    }

    // ========================================================================
    //  Bridge Methods (called from JavaScript)
    // ========================================================================

    /**
     * Returns all registered actions as a JSON string.
     * Called once on page load to populate the UI.
     * JavaScript renders whatever actions Java provides — no hardcoded data in JS.
     */
    public String getActionsJson() {
        List<DebloatAction> actions = registry.getAllActions();
        // Build a list of serializable action objects (without the PS block for security)
        List<Map<String, Object>> actionMaps = new ArrayList<>();
        for (DebloatAction a : actions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("title", a.getTitle());
            map.put("description", a.getDescription());
            map.put("category", a.getCategory().getLabel());
            map.put("categoryOrder", a.getCategory().getOrder());
            map.put("riskLevel", a.getRiskLevel().getLabel());
            map.put("selectedByDefault", a.isSelectedByDefault());
            map.put("restartRecommended", a.isRestartRecommended());
            map.put("requiresAdmin", a.isRequiresAdmin());
            map.put("tags", a.getTags());
            // PowerShell block is NOT sent to JavaScript — it stays in Java
            actionMaps.add(map);
        }
        return gson.toJson(actionMaps);
    }

    /**
     * Returns admin status as a JSON string.
     * {"isAdmin": true/false}
     */
    public String getAdminStatus() {
        boolean isAdmin = AdminChecker.isAdmin();
        return gson.toJson(Map.of("isAdmin", isAdmin));
    }

    /**
     * Preview the generated PowerShell script for the given selected action IDs.
     * Returns the script content as a plain string.
     * JavaScript displays it in a read-only preview panel.
     */
    public String previewScript(String selectedIdsJson) {
        List<String> ids = parseIdList(selectedIdsJson);
        List<String> validIds = registry.validateIds(ids);

        if (validIds.isEmpty()) {
            return "# No valid actions selected.";
        }

        List<DebloatAction> actions = validIds.stream()
            .map(registry::getAction)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return scriptBuilder.buildScript(actions);
    }

    /**
     * Executes the selected actions. Blocks if not admin.
     * Returns immediately — execution runs on a background thread.
     * Live results are pushed to JavaScript via callbacks.
     * The selectedIdsJson is a JSON array of action ID strings.
     */
    public String runSelected(String selectedIdsJson) {
        // Block execution if not admin
        if (!AdminChecker.isAdmin()) {
            return gson.toJson(Map.of(
                "success", false,
                "error", "Administrator privileges required. Please restart the app as Administrator."
            ));
        }

        if (runner.isRunning()) {
            return gson.toJson(Map.of(
                "success", false,
                "error", "An execution is already in progress."
            ));
        }

        List<String> ids = parseIdList(selectedIdsJson);
        List<String> validIds = registry.validateIds(ids);

        if (validIds.isEmpty()) {
            return gson.toJson(Map.of(
                "success", false,
                "error", "No valid actions selected."
            ));
        }

        // Check for aggressive actions and return a warning list
        List<String> aggressiveActions = validIds.stream()
            .map(registry::getAction)
            .filter(Objects::nonNull)
            .filter(a -> a.getRiskLevel() == debloater.model.RiskLevel.AGGRESSIVE)
            .map(DebloatAction::getTitle)
            .collect(Collectors.toList());

        List<DebloatAction> actions = validIds.stream()
            .map(registry::getAction)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        String script = scriptBuilder.buildScript(actions);

        // Set up callbacks — these push live updates to the WebView
        runner.setOnRawLogLine(line -> {
            Platform.runLater(() -> callJs("onRawLogLine", escapeForJs(line)));
        });

        runner.setOnActionResult(result -> {
            String json = gson.toJson(result);
            Platform.runLater(() -> callJs("onActionResult", json));
        });

        runner.setOnComplete(() -> {
            List<ActionExecutionResult> results = runner.getResults();
            ExecutionSummary summary = reportService.buildSummary(results);
            String summaryJson = gson.toJson(summary);
            String resultsJson = gson.toJson(results);
            Platform.runLater(() -> callJs("onExecutionComplete", summaryJson, resultsJson));
        });

        runner.setOnError(error -> {
            Platform.runLater(() -> callJs("onExecutionError", escapeForJs(error)));
        });

        // Run on a background thread so the UI doesn't freeze
        Thread execThread = new Thread(() -> runner.execute(script), "ps-executor");
        execThread.setDaemon(true);
        execThread.start();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("totalActions", validIds.size());
        response.put("aggressiveActions", aggressiveActions);
        return gson.toJson(response);
    }

    /**
     * Exports the selected action IDs as a profile JSON.
     * Opens a file save dialog.
     */
    public String exportProfile(String selectedIdsJson) {
        List<String> ids = parseIdList(selectedIdsJson);
        List<String> validIds = registry.validateIds(ids);

        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Profile");
            chooser.setInitialFileName("debloat_profile.json");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json")
            );
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                profileService.exportProfile(validIds, file.toPath());
                return gson.toJson(Map.of("success", true, "path", file.getAbsolutePath()));
            }
            return gson.toJson(Map.of("success", false, "error", "Export cancelled."));
        } catch (Exception e) {
            return gson.toJson(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Imports a profile from a JSON file.
     * Opens a file open dialog. Returns the valid action IDs.
     * Warns about unknown IDs.
     */
    public String importProfile() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Profile");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json")
            );
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                ProfileService.ImportResult result = profileService.importProfile(file.toPath());
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("validIds", result.validIds);
                response.put("unknownIds", result.unknownIds);
                response.put("path", file.getAbsolutePath());
                return gson.toJson(response);
            }
            return gson.toJson(Map.of("success", false, "error", "Import cancelled."));
        } catch (Exception e) {
            return gson.toJson(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Saves the raw execution log to a file.
     */
    public String saveLog() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Execution Log");
            chooser.setInitialFileName("debloater_log.txt");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt")
            );
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                reportService.saveLog(runner.getFullLog(), file.toPath());
                return gson.toJson(Map.of("success", true, "path", file.getAbsolutePath()));
            }
            return gson.toJson(Map.of("success", false, "error", "Save cancelled."));
        } catch (Exception e) {
            return gson.toJson(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Exports the structured execution report as JSON.
     */
    public String exportReportJson() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Report (JSON)");
            chooser.setInitialFileName("debloater_report.json");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json")
            );
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                reportService.saveJsonReport(runner.getResults(), file.toPath());
                return gson.toJson(Map.of("success", true, "path", file.getAbsolutePath()));
            }
            return gson.toJson(Map.of("success", false, "error", "Export cancelled."));
        } catch (Exception e) {
            return gson.toJson(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Saves the structured execution report as plain text.
     */
    public String saveReportTxt() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Report (Text)");
            chooser.setInitialFileName("debloater_report.txt");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt")
            );
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                reportService.saveTextReport(runner.getResults(), file.toPath());
                return gson.toJson(Map.of("success", true, "path", file.getAbsolutePath()));
            }
            return gson.toJson(Map.of("success", false, "error", "Save cancelled."));
        } catch (Exception e) {
            return gson.toJson(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Attempts to restart the application as Administrator.
     */
    public void restartAsAdmin() {
        AdminChecker.restartAsAdmin();
    }

    // ========================================================================
    //  Internal Helpers
    // ========================================================================

    /**
     * Parses a JSON array string into a list of action ID strings.
     * Returns empty list on parse failure.
     */
    private List<String> parseIdList(String json) {
        try {
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> ids = gson.fromJson(json, listType);
            return ids != null ? ids : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("AppBridge: Failed to parse action ID list: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Calls a JavaScript function in the WebView with the given arguments.
     * Must be called on the FX Application Thread.
     */
    private void callJs(String functionName, String... args) {
        if (webEngine == null) return;
        StringBuilder call = new StringBuilder(functionName).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) call.append(",");
            // Wrap each arg as a JS string literal
            call.append("'").append(args[i].replace("\\", "\\\\").replace("'", "\\'")).append("'");
        }
        call.append(")");
        try {
            webEngine.executeScript(call.toString());
        } catch (Exception e) {
            System.err.println("AppBridge: Failed to call JS: " + functionName + " - " + e.getMessage());
        }
    }

    /**
     * Escapes a string for safe embedding in a JavaScript string literal.
     */
    private String escapeForJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

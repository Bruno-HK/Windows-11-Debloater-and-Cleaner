package debloater.model;

import java.util.List;

/**
 * Represents a single selectable debloat operation.
 * Each action maps to a specific operation from the original win11_debloat.ps1 script.
 * Actions are registered in ActionRegistry and never generated from user input.
 */
public class DebloatAction {

    // Unique identifier used for selection, validation, and PowerShell result matching
    private final String id;

    // Human-readable title shown in the UI
    private final String title;

    // Short description explaining what this action does
    private final String description;

    // Category for grouping in the sidebar/main area
    private final ActionCategory category;

    // Risk level displayed as a badge
    private final RiskLevel riskLevel;

    // Whether this action is toggled ON by default (conservative recommended = true)
    private final boolean selectedByDefault;

    // Whether a system restart is recommended after this action
    private final boolean restartRecommended;

    // Whether this action requires administrator privileges
    private final boolean requiresAdmin;

    // Searchable tags for the filter/search box
    private final List<String> tags;

    // The PowerShell command block that this action generates.
    // This is the trusted command — never derived from user input.
    private final String powerShellBlock;

    // Whether this action needs the Ensure-RegistryPath helper function
    private final boolean needsRegistryHelper;

    public DebloatAction(String id, String title, String description,
                         ActionCategory category, RiskLevel riskLevel,
                         boolean selectedByDefault, boolean restartRecommended,
                         boolean requiresAdmin, List<String> tags,
                         String powerShellBlock, boolean needsRegistryHelper) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.riskLevel = riskLevel;
        this.selectedByDefault = selectedByDefault;
        this.restartRecommended = restartRecommended;
        this.requiresAdmin = requiresAdmin;
        this.tags = tags;
        this.powerShellBlock = powerShellBlock;
        this.needsRegistryHelper = needsRegistryHelper;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ActionCategory getCategory() { return category; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isSelectedByDefault() { return selectedByDefault; }
    public boolean isRestartRecommended() { return restartRecommended; }
    public boolean isRequiresAdmin() { return requiresAdmin; }
    public List<String> getTags() { return tags; }
    public String getPowerShellBlock() { return powerShellBlock; }
    public boolean isNeedsRegistryHelper() { return needsRegistryHelper; }
}

package debloater.service;

import debloater.model.ActionCategory;
import debloater.model.DebloatAction;
import debloater.model.RiskLevel;

import java.util.*;

/**
 * Central registry of all debloat actions.
 * Every action maps directly to an operation in the original win11_debloat.ps1 script.
 *
 * IMPORTANT: Xbox packages are intentionally excluded — required for Forza Horizon and Xbox Game Pass.
 * IMPORTANT: No invented actions — only operations present in the original script.
 *
 * To add a new action:
 * 1. Create a new DebloatAction with a unique ID in the appropriate register method.
 * 2. The PowerShell block must use the structured result helpers (New-ActionResult, etc.).
 * 3. Add it to the registry via registerAction().
 */
public class ActionRegistry {

    // All registered actions, keyed by their unique ID
    private final Map<String, DebloatAction> actions = new LinkedHashMap<>();

    public ActionRegistry() {
        registerSafetyActions();
        registerBloatAppActions();
        registerWidgetActions();
        registerCopilotAiAppActions();
        registerAiFeaturesRegistryActions();
        registerOneDriveActions();
        registerAdsActions();
        registerPrivacyTelemetryActions();
        registerServiceActions();
        registerScheduledTaskActions();
    }

    /**
     * Returns an action by its ID, or null if not found.
     * Used to validate IDs received from the WebView.
     */
    public DebloatAction getAction(String id) {
        return actions.get(id);
    }

    /**
     * Returns all registered actions in registration order.
     */
    public List<DebloatAction> getAllActions() {
        return new ArrayList<>(actions.values());
    }

    /**
     * Returns all valid action IDs. Used for validation.
     */
    public Set<String> getAllActionIds() {
        return Collections.unmodifiableSet(actions.keySet());
    }

    /**
     * Validates a list of action IDs. Returns only the valid ones.
     * Unknown IDs are logged and rejected.
     */
    public List<String> validateIds(List<String> ids) {
        List<String> valid = new ArrayList<>();
        for (String id : ids) {
            if (actions.containsKey(id)) {
                valid.add(id);
            } else {
                System.err.println("ActionRegistry: Rejected unknown action ID: " + id);
            }
        }
        return valid;
    }

    private void registerAction(DebloatAction action) {
        actions.put(action.getId(), action);
    }

    // ========================================================================
    //  SAFETY
    // ========================================================================

    private void registerSafetyActions() {
        registerAction(new DebloatAction(
            "create_restore_point",
            "Create System Restore Point",
            "Creates a Windows System Restore Point before making any changes. Allows you to roll back if needed.",
            ActionCategory.SAFETY, RiskLevel.SAFE,
            true, false, true,
            List.of("restore", "backup", "safety", "rollback"),
            // PowerShell block: enable restore on C: drive, then create checkpoint
            buildRestorePointBlock(),
            false
        ));
    }

    private String buildRestorePointBlock() {
        return """
            $actionId = "create_restore_point"
            $result = New-ActionResult -id $actionId -title "Create System Restore Point" -category "Safety" -riskLevel "Safe"
            try {
                Enable-ComputerRestore -Drive "C:\\" -ErrorAction SilentlyContinue
                Checkpoint-Computer -Description "Pre-Debloat Restore Point" -RestorePointType "MODIFY_SETTINGS" -ErrorAction Stop
                Complete-ActionResult -result $result -message "Restore point created successfully." -details "System restore point created on C: drive."
            } catch {
                if ($_.Exception.Message -like "*A restore point cannot be created*" -or $_.Exception.Message -like "*restore point was created*") {
                    AlreadyApplied-ActionResult -result $result -message "A recent restore point already exists." -details $_.Exception.Message
                } else {
                    Fail-ActionResult -result $result -message "Failed to create restore point." -error $_.Exception.Message
                }
            }
            """;
    }

    // ========================================================================
    //  BLOAT APPS — one action per app pattern from the original script
    //  Xbox is intentionally excluded.
    // ========================================================================

    private void registerBloatAppActions() {
        // Each entry: id, title, description, pattern, riskLevel, selectedByDefault, tags
        Object[][] apps = {
            {"remove_teams",            "Remove Microsoft Teams",       "Removes the pre-installed Microsoft Teams app.",                   "*teams*",            RiskLevel.MODERATE, true,  new String[]{"teams", "chat", "communication"}},
            {"remove_cortana",          "Remove Cortana",               "Removes the Cortana voice assistant app.",                         "*cortana*",          RiskLevel.MODERATE, true,  new String[]{"cortana", "voice", "assistant"}},
            {"remove_clipchamp",        "Remove Clipchamp",             "Removes the Clipchamp video editor app.",                          "*clipchamp*",        RiskLevel.MODERATE, true,  new String[]{"clipchamp", "video", "editor"}},
            {"remove_bing",             "Remove Bing Apps",             "Removes Bing-related apps and search integrations.",               "*bing*",             RiskLevel.MODERATE, true,  new String[]{"bing", "search", "web"}},
            {"remove_solitaire",        "Remove Solitaire Collection",  "Removes the Microsoft Solitaire Collection game.",                 "*solitaire*",        RiskLevel.SAFE,     true,  new String[]{"solitaire", "game", "cards"}},
            {"remove_disney",           "Remove Disney+",               "Removes the pre-installed Disney+ app.",                           "*disney*",           RiskLevel.SAFE,     true,  new String[]{"disney", "streaming", "app"}},
            {"remove_spotify",          "Remove Spotify",               "Removes the pre-installed Spotify app.",                           "*spotify*",          RiskLevel.SAFE,     true,  new String[]{"spotify", "music", "streaming"}},
            {"remove_skype",            "Remove Skype",                 "Removes the pre-installed Skype app.",                             "*skype*",            RiskLevel.SAFE,     true,  new String[]{"skype", "communication", "voip"}},
            {"remove_news",             "Remove News",                  "Removes the Microsoft News app.",                                  "*news*",             RiskLevel.SAFE,     true,  new String[]{"news", "msn", "feed"}},
            {"remove_weather",          "Remove Weather",               "Removes the Microsoft Weather app.",                               "*weather*",          RiskLevel.SAFE,     true,  new String[]{"weather", "forecast"}},
            {"remove_maps",             "Remove Maps",                  "Removes the Windows Maps app.",                                    "*maps*",             RiskLevel.MODERATE, true,  new String[]{"maps", "navigation", "bing"}},
            {"remove_gethelp",          "Remove Get Help",              "Removes the Get Help support app.",                                "*gethelp*",          RiskLevel.SAFE,     true,  new String[]{"gethelp", "help", "support"}},
            {"remove_feedbackhub",      "Remove Feedback Hub",          "Removes the Windows Feedback Hub app.",                            "*feedbackhub*",      RiskLevel.SAFE,     true,  new String[]{"feedback", "hub", "telemetry"}},
            {"remove_mixedreality",     "Remove Mixed Reality",         "Removes the Mixed Reality Portal and viewer.",                     "*mixedreality*",     RiskLevel.SAFE,     true,  new String[]{"mixed reality", "vr", "portal"}},
            {"remove_officehub",        "Remove Office Hub",            "Removes the Get Office / Office Hub app.",                         "*officehub*",        RiskLevel.MODERATE, true,  new String[]{"office", "hub", "promotion"}},
            {"remove_onenote",          "Remove OneNote",               "Removes the OneNote app. Does not affect OneNote in Office suite.","*onenote*",          RiskLevel.MODERATE, true,  new String[]{"onenote", "notes", "office"}},
            {"remove_zune",             "Remove Zune Music",            "Removes the Groove Music / Zune Music app.",                       "*zune*",             RiskLevel.SAFE,     true,  new String[]{"zune", "groove", "music"}},
            {"remove_zunevideo",        "Remove Zune Video",            "Removes the Movies & TV / Zune Video app.",                        "*zunevideo*",        RiskLevel.SAFE,     true,  new String[]{"zune", "video", "movies", "tv"}},
            {"remove_people",           "Remove People",                "Removes the People contacts app.",                                 "*people*",           RiskLevel.SAFE,     true,  new String[]{"people", "contacts"}},
            {"remove_wallet",           "Remove Wallet",                "Removes the Microsoft Wallet / Pay app.",                          "*wallet*",           RiskLevel.SAFE,     true,  new String[]{"wallet", "pay", "payment"}},
            {"remove_soundrecorder",    "Remove Sound Recorder",        "Removes the Windows Sound Recorder app.",                          "*soundrecorder*",    RiskLevel.MODERATE, true,  new String[]{"sound", "recorder", "audio"}},
            {"remove_devhome",          "Remove Dev Home",              "Removes the Dev Home app added in Windows 23H2+.",                 "*devhome*",          RiskLevel.SAFE,     true,  new String[]{"dev", "home", "developer"}},
            {"remove_windowsterminal",  "Remove Windows Terminal",      "Removes Windows Terminal. Only if you prefer a third-party terminal.", "*windowsterminal*", RiskLevel.MODERATE, false, new String[]{"terminal", "console", "cmd"}},
        };

        for (Object[] app : apps) {
            String id = (String) app[0];
            String title = (String) app[1];
            String desc = (String) app[2];
            String pattern = (String) app[3];
            RiskLevel risk = (RiskLevel) app[4];
            boolean defaultOn = (boolean) app[5];
            String[] tags = (String[]) app[6];

            registerAction(new DebloatAction(
                id, title, desc,
                ActionCategory.BLOAT_APPS, risk,
                defaultOn, false, true,
                List.of(tags),
                buildAppRemovalBlock(id, title, pattern, risk.getLabel()),
                false
            ));
        }
    }

    /**
     * Generates a PowerShell block that removes an app by pattern.
     * Handles both installed (Get-AppxPackage) and provisioned packages.
     * Distinguishes between "not found" (Skipped) and "error" (Failed).
     */
    private String buildAppRemovalBlock(String id, String title, String pattern, String riskLevel) {
        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "Bloat Apps" -riskLevel "%s"
            try {
                $installedCount = 0
                $provisionedCount = 0
                $installed = Get-AppxPackage "%s" -AllUsers -ErrorAction SilentlyContinue
                if ($installed) {
                    $installed | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue
                    $installedCount = @($installed).Count
                }
                $provisioned = Get-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue | Where-Object DisplayName -like "%s"
                if ($provisioned) {
                    $provisioned | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue
                    $provisionedCount = @($provisioned).Count
                }
                if ($installedCount -eq 0 -and $provisionedCount -eq 0) {
                    Skip-ActionResult -result $result -message "Package not found on this system." -details "No installed or provisioned packages matching '%s' were found."
                } elseif ($installedCount -gt 0 -and $provisionedCount -gt 0) {
                    Complete-ActionResult -result $result -message "Removed installed and provisioned packages." -details "Installed packages removed: $installedCount. Provisioned packages removed: $provisionedCount."
                } elseif ($installedCount -gt 0) {
                    Complete-ActionResult -result $result -message "Removed installed package." -details "Installed packages removed: $installedCount. No provisioned package found."
                } else {
                    Complete-ActionResult -result $result -message "Removed provisioned package." -details "No installed package found. Provisioned packages removed: $provisionedCount."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to remove package." -error $_.Exception.Message
            }
            """, id, title, riskLevel, pattern, pattern, pattern);
    }

    // ========================================================================
    //  WIDGETS
    // ========================================================================

    private void registerWidgetActions() {
        registerAction(new DebloatAction(
            "remove_webexperience",
            "Remove Windows Web Experience Pack",
            "Removes the Web Experience Pack that powers the Widgets panel.",
            ActionCategory.WIDGETS, RiskLevel.MODERATE,
            true, false, true,
            List.of("widgets", "web experience", "news"),
            buildAppRemovalBlockCustomCategory("remove_webexperience", "Remove Windows Web Experience Pack",
                "*WebExperience*", "Moderate", "Widgets"),
            false
        ));

        registerAction(new DebloatAction(
            "hide_widgets_taskbar",
            "Hide Widgets Taskbar Button",
            "Hides the Widgets button from the Windows taskbar.",
            ActionCategory.WIDGETS, RiskLevel.SAFE,
            true, false, false,
            List.of("widgets", "taskbar", "button"),
            buildRegistrySetBlock("hide_widgets_taskbar", "Hide Widgets Taskbar Button", "Widgets", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
                "TaskbarDa", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "disable_widgets_feed",
            "Disable Widgets News/MSN Feed",
            "Disables the Widgets news and MSN feed via Group Policy.",
            ActionCategory.WIDGETS, RiskLevel.SAFE,
            true, false, true,
            List.of("widgets", "news", "msn", "feed", "policy"),
            buildRegistrySetBlock("disable_widgets_feed", "Disable Widgets News/MSN Feed", "Widgets", "Safe",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Dsh",
                "AllowNewsAndInterests", "0", true),
            true
        ));
    }

    private String buildAppRemovalBlockCustomCategory(String id, String title, String pattern, String riskLevel, String category) {
        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "%s" -riskLevel "%s"
            try {
                $installedCount = 0
                $provisionedCount = 0
                $installed = Get-AppxPackage "%s" -AllUsers -ErrorAction SilentlyContinue
                if ($installed) {
                    $installed | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue
                    $installedCount = @($installed).Count
                }
                $provisioned = Get-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue | Where-Object DisplayName -like "%s"
                if ($provisioned) {
                    $provisioned | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue
                    $provisionedCount = @($provisioned).Count
                }
                if ($installedCount -eq 0 -and $provisionedCount -eq 0) {
                    Skip-ActionResult -result $result -message "Package not found on this system." -details "No installed or provisioned packages matching '%s' were found."
                } elseif ($installedCount -gt 0 -and $provisionedCount -gt 0) {
                    Complete-ActionResult -result $result -message "Removed installed and provisioned packages." -details "Installed packages removed: $installedCount. Provisioned packages removed: $provisionedCount."
                } elseif ($installedCount -gt 0) {
                    Complete-ActionResult -result $result -message "Removed installed package." -details "Installed packages removed: $installedCount. No provisioned package found."
                } else {
                    Complete-ActionResult -result $result -message "Removed provisioned package." -details "No installed package found. Provisioned packages removed: $provisionedCount."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to remove package." -error $_.Exception.Message
            }
            """, id, title, category, riskLevel, pattern, pattern, pattern);
    }

    // ========================================================================
    //  COPILOT & AI APPS
    // ========================================================================

    private void registerCopilotAiAppActions() {
        registerAction(new DebloatAction(
            "remove_copilot_packages",
            "Remove Copilot Packages",
            "Removes all packages matching *copilot* pattern.",
            ActionCategory.COPILOT_AI_APPS, RiskLevel.MODERATE,
            true, false, true,
            List.of("copilot", "ai", "assistant"),
            buildAppRemovalBlockCustomCategory("remove_copilot_packages", "Remove Copilot Packages",
                "*copilot*", "Moderate", "Copilot & AI Apps"),
            false
        ));

        registerAction(new DebloatAction(
            "remove_windowsai",
            "Remove Windows AI Packages",
            "Removes all packages matching *windowsai* pattern.",
            ActionCategory.COPILOT_AI_APPS, RiskLevel.MODERATE,
            true, false, true,
            List.of("windowsai", "ai", "recall"),
            buildAppRemovalBlockCustomCategory("remove_windowsai", "Remove Windows AI Packages",
                "*windowsai*", "Moderate", "Copilot & AI Apps"),
            false
        ));

        registerAction(new DebloatAction(
            "remove_windows_copilot",
            "Remove Microsoft.Windows.Copilot",
            "Removes the Microsoft.Windows.Copilot package by exact name.",
            ActionCategory.COPILOT_AI_APPS, RiskLevel.MODERATE,
            true, false, true,
            List.of("copilot", "windows", "ai"),
            buildAppRemovalBlockCustomCategory("remove_windows_copilot", "Remove Microsoft.Windows.Copilot",
                "Microsoft.Windows.Copilot", "Moderate", "Copilot & AI Apps"),
            false
        ));

        registerAction(new DebloatAction(
            "remove_microsoft_copilot",
            "Remove Microsoft.Copilot",
            "Removes the Microsoft.Copilot package by exact name (newer builds).",
            ActionCategory.COPILOT_AI_APPS, RiskLevel.MODERATE,
            true, false, true,
            List.of("copilot", "microsoft", "ai"),
            buildAppRemovalBlockCustomCategory("remove_microsoft_copilot", "Remove Microsoft.Copilot",
                "Microsoft.Copilot", "Moderate", "Copilot & AI Apps"),
            false
        ));
    }

    // ========================================================================
    //  AI FEATURES (REGISTRY)
    // ========================================================================

    private void registerAiFeaturesRegistryActions() {
        registerAction(new DebloatAction(
            "disable_copilot_hklm",
            "Disable Copilot (HKLM Policy)",
            "Disables Windows Copilot via machine-level Group Policy registry key.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, true,
            List.of("copilot", "policy", "hklm", "disable"),
            buildRegistrySetBlock("disable_copilot_hklm", "Disable Copilot (HKLM Policy)", "AI Features (Registry)", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsCopilot",
                "TurnOffWindowsCopilot", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_copilot_hkcu",
            "Disable Copilot (HKCU Policy)",
            "Disables Windows Copilot via user-level Group Policy registry key.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, false,
            List.of("copilot", "policy", "hkcu", "disable"),
            buildRegistrySetBlock("disable_copilot_hkcu", "Disable Copilot (HKCU Policy)", "AI Features (Registry)", "Moderate",
                "HKCU:\\Software\\Policies\\Microsoft\\Windows\\WindowsCopilot",
                "TurnOffWindowsCopilot", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_recall_hklm",
            "Disable Windows Recall (HKLM Policy)",
            "Disables Windows Recall / AI screenshot data analysis via machine-level policy.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, true,
            List.of("recall", "ai", "screenshot", "hklm", "policy"),
            buildRegistrySetBlock("disable_recall_hklm", "Disable Windows Recall (HKLM Policy)", "AI Features (Registry)", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsAI",
                "DisableAIDataAnalysis", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_recall_hkcu",
            "Disable Windows Recall (HKCU Policy)",
            "Disables Windows Recall / AI screenshot data analysis via user-level policy.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, false,
            List.of("recall", "ai", "screenshot", "hkcu", "policy"),
            buildRegistrySetBlock("disable_recall_hkcu", "Disable Windows Recall (HKCU Policy)", "AI Features (Registry)", "Moderate",
                "HKCU:\\SOFTWARE\\Policies\\Microsoft\\Windows\\WindowsAI",
                "DisableAIDataAnalysis", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "hide_copilot_taskbar",
            "Hide Copilot Taskbar Button",
            "Hides the Copilot button from the Windows taskbar.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.SAFE,
            true, false, false,
            List.of("copilot", "taskbar", "button", "hide"),
            buildRegistrySetBlock("hide_copilot_taskbar", "Hide Copilot Taskbar Button", "AI Features (Registry)", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
                "ShowCopilotButton", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "remove_copilot_contextmenu",
            "Remove Copilot from Context Menu",
            "Removes the 'Ask Copilot' option from the right-click context menu.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.SAFE,
            true, false, false,
            List.of("copilot", "context menu", "right-click"),
            buildRemoveRegistryKeyBlock("remove_copilot_contextmenu", "Remove Copilot from Context Menu",
                "AI Features (Registry)", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\Shell\\ContextMenuHandlers\\AskCopilot"),
            false
        ));

        registerAction(new DebloatAction(
            "disable_bing_search",
            "Disable Bing Web Search",
            "Disables Bing web search integration in Windows Search.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, false,
            List.of("bing", "search", "web", "disable"),
            buildRegistrySetBlock("disable_bing_search", "Disable Bing Web Search", "AI Features (Registry)", "Moderate",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Search",
                "BingSearchEnabled", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "disable_cortana_consent",
            "Disable Cortana Consent",
            "Disables Cortana consent/integration in Windows Search.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.SAFE,
            true, false, false,
            List.of("cortana", "consent", "search"),
            buildRegistrySetBlock("disable_cortana_consent", "Disable Cortana Consent", "AI Features (Registry)", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Search",
                "CortanaConsent", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "disable_web_search_policy",
            "Disable Web Search (Policy)",
            "Disables web search in Windows Search via Group Policy.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.MODERATE,
            true, false, true,
            List.of("web", "search", "policy", "disable"),
            buildRegistrySetBlock("disable_web_search_policy", "Disable Web Search (Policy)", "AI Features (Registry)", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\Windows Search",
                "DisableWebSearch", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_dynamic_searchbox",
            "Disable Dynamic Search Box",
            "Disables the AI-powered dynamic search box suggestions.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.SAFE,
            true, false, false,
            List.of("search", "dynamic", "ai", "suggestions"),
            buildRegistrySetBlock("disable_dynamic_searchbox", "Disable Dynamic Search Box", "AI Features (Registry)", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\SearchSettings",
                "IsDynamicSearchBoxEnabled", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "disable_smart_clipboard",
            "Disable Smart Clipboard Actions",
            "Disables AI-powered suggested/smart clipboard actions.",
            ActionCategory.AI_FEATURES_REGISTRY, RiskLevel.SAFE,
            true, false, false,
            List.of("clipboard", "smart", "ai", "suggestions"),
            buildRegistrySetBlock("disable_smart_clipboard", "Disable Smart Clipboard Actions", "AI Features (Registry)", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\SmartActionPlatform\\SmartClipboard",
                "Disabled", "1", true),
            true
        ));
    }

    // ========================================================================
    //  ONEDRIVE
    // ========================================================================

    private void registerOneDriveActions() {
        registerAction(new DebloatAction(
            "stop_onedrive",
            "Stop OneDrive Process",
            "Stops the running OneDrive process (taskkill).",
            ActionCategory.ONEDRIVE, RiskLevel.SAFE,
            false, false, false,
            List.of("onedrive", "process", "stop", "kill"),
            buildStopOneDriveBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "uninstall_onedrive",
            "Uninstall OneDrive",
            "Uninstalls OneDrive from the system. This may cause data sync to stop. Back up your OneDrive files first.",
            ActionCategory.ONEDRIVE, RiskLevel.AGGRESSIVE,
            false, true, true,
            List.of("onedrive", "uninstall", "remove"),
            buildUninstallOneDriveBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "remove_onedrive_startup",
            "Remove OneDrive Startup Entry",
            "Removes OneDrive from the Windows startup registry key.",
            ActionCategory.ONEDRIVE, RiskLevel.MODERATE,
            false, false, false,
            List.of("onedrive", "startup", "boot", "autorun"),
            buildRemoveOneDriveStartupBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "disable_onedrive_explorer",
            "Disable OneDrive in File Explorer",
            "Hides OneDrive integration in File Explorer via Group Policy.",
            ActionCategory.ONEDRIVE, RiskLevel.MODERATE,
            false, false, true,
            List.of("onedrive", "explorer", "sidebar", "file"),
            buildRegistrySetBlock("disable_onedrive_explorer", "Disable OneDrive in File Explorer", "OneDrive", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\OneDrive",
                "DisableFileSyncNGSC", "1", true),
            true
        ));
    }

    private String buildStopOneDriveBlock() {
        return """
            $actionId = "stop_onedrive"
            $result = New-ActionResult -id $actionId -title "Stop OneDrive Process" -category "OneDrive" -riskLevel "Safe"
            try {
                $proc = Get-Process -Name "OneDrive" -ErrorAction SilentlyContinue
                if ($proc) {
                    taskkill /f /im OneDrive.exe 2>$null | Out-Null
                    Start-Sleep -Seconds 1
                    $procAfter = Get-Process -Name "OneDrive" -ErrorAction SilentlyContinue
                    if (-not $procAfter) {
                        Complete-ActionResult -result $result -message "OneDrive process stopped." -details "Process terminated successfully."
                    } else {
                        Partial-ActionResult -result $result -message "OneDrive process may still be running." -details "taskkill was executed but process is still detected."
                    }
                } else {
                    Skip-ActionResult -result $result -message "OneDrive is not running." -details "No OneDrive process found."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to stop OneDrive." -error $_.Exception.Message
            }
            """;
    }

    private String buildUninstallOneDriveBlock() {
        return """
            $actionId = "uninstall_onedrive"
            $result = New-ActionResult -id $actionId -title "Uninstall OneDrive" -category "OneDrive" -riskLevel "Aggressive"
            try {
                $onedrivePath = "$env:SystemRoot\\SysWOW64\\OneDriveSetup.exe"
                if (!(Test-Path $onedrivePath)) { $onedrivePath = "$env:SystemRoot\\System32\\OneDriveSetup.exe" }
                if (Test-Path $onedrivePath) {
                    $proc = Start-Process $onedrivePath "/uninstall" -NoNewWindow -Wait -PassThru
                    if ($proc.ExitCode -eq 0) {
                        Complete-ActionResult -result $result -message "OneDrive uninstalled." -details "OneDriveSetup.exe /uninstall completed with exit code 0."
                    } else {
                        Partial-ActionResult -result $result -message "OneDrive uninstall completed with non-zero exit code." -details "Exit code: $($proc.ExitCode)"
                    }
                } else {
                    Skip-ActionResult -result $result -message "OneDrive installer not found." -details "Neither SysWOW64 nor System32 contain OneDriveSetup.exe. OneDrive may already be removed."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to uninstall OneDrive." -error $_.Exception.Message
            }
            """;
    }

    private String buildRemoveOneDriveStartupBlock() {
        return """
            $actionId = "remove_onedrive_startup"
            $result = New-ActionResult -id $actionId -title "Remove OneDrive Startup Entry" -category "OneDrive" -riskLevel "Moderate"
            try {
                $regPath = "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run"
                $existing = Get-ItemProperty -Path $regPath -Name "OneDrive" -ErrorAction SilentlyContinue
                if ($existing) {
                    Remove-ItemProperty -Path $regPath -Name "OneDrive" -ErrorAction Stop
                    Complete-ActionResult -result $result -message "OneDrive startup entry removed." -details "Removed 'OneDrive' from HKCU Run key."
                } else {
                    AlreadyApplied-ActionResult -result $result -message "OneDrive startup entry not found." -details "No 'OneDrive' value in HKCU Run key."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to remove OneDrive startup entry." -error $_.Exception.Message
            }
            """;
    }

    // ========================================================================
    //  ADS & SPONSORED CONTENT
    // ========================================================================

    private void registerAdsActions() {
        String cdm = "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\ContentDeliveryManager";

        // Each CDM registry value mapped to an individual action
        Object[][] cdmEntries = {
            {"disable_tips_suggestions",           "Disable Tips and Suggestions",              "Disables Windows tips and suggestions notifications.",              "SubscribedContent-338389Enabled",  "0"},
            {"disable_spotlight_tips",             "Disable Spotlight Tips",                    "Disables Spotlight tips on the lock screen.",                       "SubscribedContent-310093Enabled",  "0"},
            {"disable_startmenu_suggestions",      "Disable Start Menu Suggestions",            "Disables suggested content in the Start menu.",                     "SubscribedContent-338388Enabled",  "0"},
            {"disable_timeline_suggestions",       "Disable Timeline Suggestions",              "Disables Timeline activity suggestions.",                           "SubscribedContent-353698Enabled",  "0"},
            {"disable_lockscreen_spotlight_tips",   "Disable Lock Screen Spotlight Tips",        "Disables Spotlight tips on the lock screen.",                       "SubscribedContent-338387Enabled",  "0"},
            {"disable_app_suggestions",            "Disable App Suggestions",                   "Disables app installation suggestions.",                            "SoftLandingEnabled",               "0"},
            {"disable_startmenu_recommended",      "Disable Start Menu Recommended Section",    "Disables the Recommended section in the Start menu.",               "SystemPaneSuggestionsEnabled",     "0"},
            {"disable_content_delivery",           "Disable Content Delivery",                  "Blocks all Windows content delivery (sponsored content pipeline).", "ContentDeliveryAllowed",           "0"},
            {"disable_oem_promoted",               "Disable OEM Promoted Apps",                 "Disables OEM pre-installed app promotions.",                        "OemPreInstalledAppsEnabled",       "0"},
            {"disable_preinstalled_promotions",    "Disable Preinstalled App Promotions",       "Disables promotions for pre-installed apps.",                       "PreInstalledAppsEnabled",          "0"},
            {"disable_preinstalled_ever",          "Disable Preinstalled Apps Ever Enabled",    "Prevents pre-installed app promotions from ever being enabled.",    "PreInstalledAppsEverEnabled",      "0"},
            {"disable_silently_installed",         "Disable Silently Installed Apps",           "Prevents Windows from silently installing promoted apps.",          "SilentInstalledAppsEnabled",       "0"},
            {"disable_rotating_lockscreen",        "Disable Rotating Lock Screen",              "Disables rotating Spotlight images on the lock screen.",            "RotatingLockScreenEnabled",        "0"},
            {"disable_rotating_lockscreen_overlay","Disable Rotating Lock Screen Overlay",      "Disables the overlay on rotating lock screen images.",              "RotatingLockScreenOverlayEnabled", "0"},
        };

        for (Object[] entry : cdmEntries) {
            String id = (String) entry[0];
            String title = (String) entry[1];
            String desc = (String) entry[2];
            String valueName = (String) entry[3];
            String valueData = (String) entry[4];

            registerAction(new DebloatAction(
                id, title, desc,
                ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
                true, false, false,
                List.of("ads", "sponsored", "content delivery", valueName.toLowerCase()),
                buildRegistrySetBlock(id, title, "Ads & Sponsored Content", "Safe", cdm, valueName, valueData, true),
                true
            ));
        }

        // Lock screen slideshow (different registry path)
        registerAction(new DebloatAction(
            "disable_lockscreen_slideshow",
            "Disable Lock Screen Spotlight Slideshow",
            "Disables the Windows Spotlight slideshow on the lock screen.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, false,
            List.of("lockscreen", "spotlight", "slideshow"),
            buildRegistrySetBlock("disable_lockscreen_slideshow", "Disable Lock Screen Spotlight Slideshow",
                "Ads & Sponsored Content", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Lock Screen",
                "SlideshowEnabled", "0", false),
            false
        ));

        // Recently added apps (HKLM policy)
        registerAction(new DebloatAction(
            "disable_recently_added",
            "Disable Recently Added Apps Suggestions",
            "Hides 'Recently added' apps in the Start menu via Group Policy.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, true,
            List.of("recently added", "start menu", "suggestions", "policy"),
            buildRegistrySetBlock("disable_recently_added", "Disable Recently Added Apps Suggestions",
                "Ads & Sponsored Content", "Safe",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\Explorer",
                "HideRecentlyAddedApps", "1", true),
            true
        ));

        // File Explorer sync provider ads
        registerAction(new DebloatAction(
            "disable_explorer_syncprovider_ads",
            "Disable File Explorer Sync Provider Ads",
            "Disables OneDrive sync banners and 'Discover' section in File Explorer.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, false,
            List.of("explorer", "sync", "onedrive", "ads", "banner"),
            buildRegistrySetBlock("disable_explorer_syncprovider_ads", "Disable File Explorer Sync Provider Ads",
                "Ads & Sponsored Content", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
                "ShowSyncProviderNotifications", "0", false),
            false
        ));

        // Meet Now / Chat icon
        registerAction(new DebloatAction(
            "disable_meetnow_chat",
            "Disable Meet Now / Chat Icon",
            "Hides the Meet Now / Chat icon from the taskbar.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, false,
            List.of("meet now", "chat", "taskbar", "teams"),
            buildRegistrySetBlock("disable_meetnow_chat", "Disable Meet Now / Chat Icon",
                "Ads & Sponsored Content", "Safe",
                "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced",
                "TaskbarMn", "0", false),
            false
        ));

        // Search highlights
        registerAction(new DebloatAction(
            "disable_search_highlights",
            "Disable Search Highlights",
            "Disables trending/news content in Windows Search.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, false,
            List.of("search", "highlights", "trending", "news"),
            buildRegistrySetBlock("disable_search_highlights", "Disable Search Highlights",
                "Ads & Sponsored Content", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Feeds\\DSB",
                "ShowDynamicContent", "0", false),
            false
        ));

        // Dynamic search box from SearchSettings (duplicate in script Section 6)
        registerAction(new DebloatAction(
            "disable_dynamic_searchbox_ads",
            "Disable Dynamic Search Box (Ads)",
            "Disables the dynamic search box from SearchSettings to remove ad-like suggestions.",
            ActionCategory.ADS_SPONSORED, RiskLevel.SAFE,
            true, false, false,
            List.of("search", "dynamic", "searchbox", "ads"),
            buildRegistrySetBlock("disable_dynamic_searchbox_ads", "Disable Dynamic Search Box (Ads)",
                "Ads & Sponsored Content", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\SearchSettings",
                "IsDynamicSearchBoxEnabled", "0", true),
            true
        ));
    }

    // ========================================================================
    //  PRIVACY & TELEMETRY
    // ========================================================================

    private void registerPrivacyTelemetryActions() {
        registerAction(new DebloatAction(
            "disable_telemetry",
            "Disable Telemetry",
            "Disables Windows telemetry data collection via Group Policy.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.MODERATE,
            true, false, true,
            List.of("telemetry", "data collection", "privacy", "policy"),
            buildRegistrySetBlock("disable_telemetry", "Disable Telemetry", "Privacy & Telemetry", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\DataCollection",
                "AllowTelemetry", "0", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_advertising_id",
            "Disable Advertising ID",
            "Disables the Windows advertising ID used for targeted ads.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.SAFE,
            true, false, false,
            List.of("advertising", "id", "privacy", "tracking"),
            buildRegistrySetBlock("disable_advertising_id", "Disable Advertising ID", "Privacy & Telemetry", "Safe",
                "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\AdvertisingInfo",
                "Enabled", "0", false),
            false
        ));

        registerAction(new DebloatAction(
            "disable_activity_feed",
            "Disable Activity History Feed",
            "Disables the Windows activity history (Timeline) feed.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.SAFE,
            true, false, true,
            List.of("activity", "history", "timeline", "feed"),
            buildRegistrySetBlock("disable_activity_feed", "Disable Activity History Feed", "Privacy & Telemetry", "Safe",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\System",
                "EnableActivityFeed", "0", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_publish_activities",
            "Disable Publishing User Activities",
            "Prevents Windows from publishing user activity data.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.SAFE,
            true, false, true,
            List.of("activity", "publish", "privacy"),
            buildRegistrySetBlock("disable_publish_activities", "Disable Publishing User Activities", "Privacy & Telemetry", "Safe",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\System",
                "PublishUserActivities", "0", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_upload_activities",
            "Disable Uploading User Activities",
            "Prevents Windows from uploading user activity data to Microsoft.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.SAFE,
            true, false, true,
            List.of("activity", "upload", "privacy", "cloud"),
            buildRegistrySetBlock("disable_upload_activities", "Disable Uploading User Activities", "Privacy & Telemetry", "Safe",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\System",
                "UploadUserActivities", "0", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_location_tracking",
            "Disable Location Tracking",
            "Disables Windows location tracking via Group Policy. Some apps may lose location features.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.MODERATE,
            false, false, true,
            List.of("location", "tracking", "gps", "privacy"),
            buildRegistrySetBlock("disable_location_tracking", "Disable Location Tracking", "Privacy & Telemetry", "Moderate",
                "HKLM:\\SOFTWARE\\Policies\\Microsoft\\Windows\\LocationAndSensors",
                "DisableLocation", "1", true),
            true
        ));

        registerAction(new DebloatAction(
            "disable_app_diagnostics",
            "Disable App Diagnostics Access",
            "Prevents apps from reading other apps' diagnostic information.",
            ActionCategory.PRIVACY_TELEMETRY, RiskLevel.SAFE,
            true, false, false,
            List.of("diagnostics", "apps", "privacy", "access"),
            buildAppDiagnosticsBlock(),
            false
        ));
    }

    private String buildAppDiagnosticsBlock() {
        return """
            $actionId = "disable_app_diagnostics"
            $result = New-ActionResult -id $actionId -title "Disable App Diagnostics Access" -category "Privacy & Telemetry" -riskLevel "Safe"
            try {
                $regPath = "HKCU:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\DeviceAccess\\Global\\{2297E4E2-5DBE-466D-A12B-0F8286F0D9CA}"
                if (Test-Path $regPath) {
                    $current = (Get-ItemProperty -Path $regPath -Name "Value" -ErrorAction SilentlyContinue).Value
                    if ($current -eq "Deny") {
                        AlreadyApplied-ActionResult -result $result -message "App diagnostics already denied." -details "Value is already set to 'Deny'."
                    } else {
                        Set-ItemProperty -Path $regPath -Name "Value" -Type String -Value "Deny" -ErrorAction Stop
                        Complete-ActionResult -result $result -message "App diagnostics access denied." -details "Set value to 'Deny' at device access path."
                    }
                } else {
                    Skip-ActionResult -result $result -message "Device access registry path not found." -details "Path does not exist on this system: $regPath"
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to disable app diagnostics." -error $_.Exception.Message
            }
            """;
    }

    // ========================================================================
    //  SERVICES
    // ========================================================================

    private void registerServiceActions() {
        Object[][] services = {
            {"disable_sysmain",         "Disable SysMain Service",          "Stops and disables the SysMain (Superfetch) service. Less useful on SSDs.", "SysMain",        RiskLevel.MODERATE, false, new String[]{"sysmain", "superfetch", "service", "ssd"}},
            {"disable_fax",             "Disable Fax Service",              "Stops and disables the Windows Fax service.",                                "Fax",            RiskLevel.SAFE,     true,  new String[]{"fax", "service"}},
            {"disable_remote_registry", "Disable Remote Registry Service",  "Stops and disables the Remote Registry service (security hardening).",       "RemoteRegistry", RiskLevel.SAFE,     true,  new String[]{"remote registry", "service", "security"}},
        };

        for (Object[] svc : services) {
            String id = (String) svc[0];
            String title = (String) svc[1];
            String desc = (String) svc[2];
            String svcName = (String) svc[3];
            RiskLevel risk = (RiskLevel) svc[4];
            boolean defaultOn = (boolean) svc[5];
            String[] tags = (String[]) svc[6];

            registerAction(new DebloatAction(
                id, title, desc,
                ActionCategory.SERVICES, risk,
                defaultOn, false, true,
                List.of(tags),
                buildServiceDisableBlock(id, title, svcName, risk.getLabel()),
                false
            ));
        }
    }

    /**
     * Generates a PowerShell block that stops and disables a Windows service.
     * Handles "service not found" as Skipped, not Failed.
     */
    private String buildServiceDisableBlock(String id, String title, String serviceName, String riskLevel) {
        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "Services" -riskLevel "%s"
            try {
                $svc = Get-Service -Name "%s" -ErrorAction SilentlyContinue
                if ($svc) {
                    if ($svc.StartType -eq 'Disabled' -and $svc.Status -ne 'Running') {
                        AlreadyApplied-ActionResult -result $result -message "Service already disabled." -details "Service '%s' is already set to Disabled and is not running."
                    } else {
                        if ($svc.Status -eq 'Running') {
                            Stop-Service -Name "%s" -Force -ErrorAction SilentlyContinue
                        }
                        Set-Service -Name "%s" -StartupType Disabled -ErrorAction Stop
                        Complete-ActionResult -result $result -message "Service stopped and disabled." -details "Service '%s' stopped and startup type changed to Disabled."
                    }
                } else {
                    Skip-ActionResult -result $result -message "Service not found." -details "Service '%s' does not exist on this Windows installation."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to disable service." -error $_.Exception.Message
            }
            """, id, title, riskLevel, serviceName, serviceName, serviceName, serviceName, serviceName, serviceName);
    }

    // ========================================================================
    //  SCHEDULED TASKS
    // ========================================================================

    private void registerScheduledTaskActions() {
        Object[][] tasks = {
            {"disable_task_compat_appraiser",  "Disable Compatibility Appraiser",        "Disables the Microsoft Compatibility Appraiser scheduled task.",       "\\Microsoft\\Windows\\Application Experience\\Microsoft Compatibility Appraiser"},
            {"disable_task_programdata",       "Disable ProgramDataUpdater",             "Disables the ProgramDataUpdater scheduled task.",                      "\\Microsoft\\Windows\\Application Experience\\ProgramDataUpdater"},
            {"disable_task_autochk_proxy",     "Disable Autochk Proxy",                  "Disables the Autochk Proxy scheduled task.",                           "\\Microsoft\\Windows\\Autochk\\Proxy"},
            {"disable_task_ceip_consolidator", "Disable CEIP Consolidator",              "Disables the Customer Experience Improvement Program Consolidator.",   "\\Microsoft\\Windows\\Customer Experience Improvement Program\\Consolidator"},
            {"disable_task_usbceip",           "Disable USB CEIP",                       "Disables the USB Customer Experience Improvement Program task.",       "\\Microsoft\\Windows\\Customer Experience Improvement Program\\UsbCeip"},
            {"disable_task_disk_diagnostic",   "Disable Disk Diagnostic Data Collector", "Disables the Disk Diagnostic Data Collector scheduled task.",          "\\Microsoft\\Windows\\DiskDiagnostic\\Microsoft-Windows-DiskDiagnosticDataCollector"},
            {"disable_task_dmclient",          "Disable Feedback DmClient",              "Disables the Feedback DmClient scheduled task.",                       "\\Microsoft\\Windows\\Feedback\\Siuf\\DmClient"},
            {"disable_task_dmclient_scenario", "Disable Feedback DmClient (Scenario)",   "Disables the Feedback DmClientOnScenarioDownload scheduled task.",     "\\Microsoft\\Windows\\Feedback\\Siuf\\DmClientOnScenarioDownload"},
            {"disable_task_error_reporting",   "Disable Error Reporting Queue",          "Disables the Windows Error Reporting QueueReporting scheduled task.",  "\\Microsoft\\Windows\\Windows Error Reporting\\QueueReporting"},
        };

        for (Object[] task : tasks) {
            String id = (String) task[0];
            String title = (String) task[1];
            String desc = (String) task[2];
            String taskPath = (String) task[3];

            registerAction(new DebloatAction(
                id, title, desc,
                ActionCategory.SCHEDULED_TASKS, RiskLevel.SAFE,
                true, false, true,
                List.of("scheduled task", "telemetry", "privacy", taskPath.substring(taskPath.lastIndexOf('\\') + 1).toLowerCase()),
                buildScheduledTaskBlock(id, title, taskPath),
                false
            ));
        }
    }

    /**
     * Generates a PowerShell block that disables a scheduled task.
     * Handles "task not found" as Skipped, not Failed.
     */
    private String buildScheduledTaskBlock(String id, String title, String taskPath) {
        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "Scheduled Tasks" -riskLevel "Safe"
            try {
                $task = Get-ScheduledTask -TaskName "%s" -ErrorAction SilentlyContinue
                if ($task) {
                    if ($task.State -eq 'Disabled') {
                        AlreadyApplied-ActionResult -result $result -message "Scheduled task already disabled." -details "Task '%s' is already in Disabled state."
                    } else {
                        Disable-ScheduledTask -TaskName "%s" -ErrorAction Stop | Out-Null
                        Complete-ActionResult -result $result -message "Scheduled task disabled." -details "Task '%s' has been disabled."
                    }
                } else {
                    Skip-ActionResult -result $result -message "Scheduled task not found." -details "Task '%s' does not exist on this system."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to disable scheduled task." -error $_.Exception.Message
            }
            """, id, title, taskPath, taskPath, taskPath, taskPath, taskPath);
    }

    // ========================================================================
    //  SHARED POWERSHELL BLOCK BUILDERS
    // ========================================================================

    /**
     * Builds a PowerShell block that sets a single registry value.
     * Checks if the value already matches (Already applied), creates path if needed.
     */
    private String buildRegistrySetBlock(String id, String title, String category, String riskLevel,
                                          String regPath, String valueName, String valueData,
                                          boolean ensurePath) {
        String ensurePathCode = ensurePath
            ? String.format("Ensure-RegistryPath \"%s\"\n        ", regPath)
            : "";

        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "%s" -riskLevel "%s"
            try {
                %s$currentVal = $null
                try { $currentVal = (Get-ItemProperty -Path "%s" -Name "%s" -ErrorAction SilentlyContinue)."%s" } catch {}
                if ($currentVal -ne $null -and "$currentVal" -eq "%s") {
                    AlreadyApplied-ActionResult -result $result -message "Registry value already set." -details "'%s' at '%s' is already %s."
                } else {
                    Set-ItemProperty -Path "%s" -Name "%s" -Value %s -ErrorAction Stop
                    Complete-ActionResult -result $result -message "Registry value set successfully." -details "Set '%s' = %s at '%s'."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to set registry value." -error $_.Exception.Message
            }
            """, id, title, category, riskLevel,
            ensurePathCode,
            regPath, valueName, valueName,
            valueData,
            valueName, regPath, valueData,
            regPath, valueName, valueData,
            valueName, valueData, regPath);
    }

    /**
     * Builds a PowerShell block that removes a registry key (recursively).
     */
    private String buildRemoveRegistryKeyBlock(String id, String title, String category, String riskLevel, String regPath) {
        return String.format("""
            $actionId = "%s"
            $result = New-ActionResult -id $actionId -title "%s" -category "%s" -riskLevel "%s"
            try {
                if (Test-Path "%s") {
                    Remove-Item -Path "%s" -Recurse -ErrorAction Stop
                    Complete-ActionResult -result $result -message "Registry key removed." -details "Removed '%s' and all subkeys."
                } else {
                    AlreadyApplied-ActionResult -result $result -message "Registry key not found." -details "Key '%s' does not exist (already removed or never existed)."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to remove registry key." -error $_.Exception.Message
            }
            """, id, title, category, riskLevel, regPath, regPath, regPath, regPath);
    }
}

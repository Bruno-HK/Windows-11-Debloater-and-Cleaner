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
        registerWindowsCleanupActions();
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

    // ========================================================================
    //  WINDOWS CLEANUP
    //  For users who upgraded from Windows 10 to Windows 11 and want to
    //  remove old upgrade leftovers and reduce residual files safely.
    //  NOTE: This does NOT make an upgraded system identical to a fresh install.
    //  A real fresh install is still different.
    //  NOTE: The "Create System Restore Point" action in the Safety category
    //  is recommended ON by default and should be enabled before running
    //  Windows Cleanup actions.
    // ========================================================================

    private void registerWindowsCleanupActions() {
        // --- Safe actions (recommended ON by default) ---

        registerAction(new DebloatAction(
            "cleanup_windows_temp",
            "Remove Windows Temporary Files",
            "Clears safe system temp locations (C:\\Windows\\Temp). Skips locked files. Does not touch user documents or personal folders.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "temp", "temporary", "windows", "cache"),
            buildCleanupWindowsTempBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_user_temp",
            "Remove Current User Temp Files",
            "Clears the current user's temp folder ($env:TEMP). Skips locked files. Reports removed and skipped counts.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "temp", "user", "cache"),
            buildCleanupUserTempBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_wu_cache",
            "Remove Windows Update Download Cache",
            "Stops Windows Update services, clears SoftwareDistribution\\Download, then restarts services. Only clears the Download subfolder, not the entire SoftwareDistribution folder.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "windows update", "cache", "download", "softwaredistribution"),
            buildCleanupWuCacheBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_do_cache",
            "Remove Delivery Optimization Cache",
            "Clears the Delivery Optimization cache used for peer-to-peer Windows Update distribution. Skips if the cache does not exist.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "delivery optimization", "cache", "peer"),
            buildCleanupDoCacheBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_upgrade_temp",
            "Remove Windows Upgrade Temporary Files",
            "Removes upgrade temp leftovers ($env:SystemDrive\\$Windows.~BT, $Windows.~WS). Checks that Windows setup is not currently running before removing.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "upgrade", "temp", "windows.bt", "windows.ws"),
            buildCleanupUpgradeTempBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_setup_logs",
            "Remove Setup Logs Older Than 30 Days",
            "Removes Windows setup/update log files older than 30 days from C:\\Windows\\Logs. Skips recent logs. Reports skipped if none found.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            true, false, true,
            List.of("cleanup", "logs", "setup", "update", "old"),
            buildCleanupSetupLogsBlock(),
            false
        ));

        // --- Moderate actions (OFF by default) ---

        registerAction(new DebloatAction(
            "cleanup_dism_startcomponentcleanup",
            "Run DISM StartComponentCleanup",
            "Runs DISM.exe /Online /Cleanup-Image /StartComponentCleanup to clean up superseded components. A restart may be recommended after completion.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.MODERATE,
            false, true, true,
            List.of("cleanup", "dism", "component", "winsxs"),
            buildDismStartComponentCleanupBlock(),
            false
        ));

        // --- Aggressive actions (OFF by default) ---

        registerAction(new DebloatAction(
            "cleanup_dism_resetbase",
            "Run DISM StartComponentCleanup with ResetBase",
            "Runs DISM.exe /Online /Cleanup-Image /StartComponentCleanup /ResetBase. WARNING: This removes superseded component versions. Installed Windows updates may no longer be uninstallable after this.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.AGGRESSIVE,
            false, true, true,
            List.of("cleanup", "dism", "resetbase", "component", "winsxs"),
            buildDismResetBaseBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_empty_recycle_bin",
            "Empty Recycle Bin",
            "Permanently removes all items from the Recycle Bin. WARNING: This permanently removes items from the Recycle Bin. They cannot be recovered.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.AGGRESSIVE,
            false, false, true,
            List.of("cleanup", "recycle bin", "trash", "empty"),
            buildEmptyRecycleBinBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_previous_windows",
            "Remove Previous Windows Installation Files",
            "Removes Windows.old and previous Windows installation leftovers using the Windows built-in Disk Cleanup mechanism. WARNING: Removing previous Windows installation files means you may not be able to roll back to the previous Windows version.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.AGGRESSIVE,
            false, false, true,
            List.of("cleanup", "windows.old", "previous", "upgrade", "rollback"),
            buildCleanupPreviousWindowsBlock(),
            false
        ));

        // --- Optional but useful (OFF by default) ---

        registerAction(new DebloatAction(
            "cleanup_crash_dumps",
            "Clear Old Crash Dump Files",
            "Removes system crash dump files from C:\\Windows\\Minidump and the main memory dump. Does not delete dumps currently in use.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.MODERATE,
            false, false, true,
            List.of("cleanup", "crash", "dump", "minidump", "memory"),
            buildCleanupCrashDumpsBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_thumbnail_cache",
            "Clear Thumbnail Cache",
            "Clears the Windows Explorer thumbnail cache. Thumbnails will be regenerated by Windows as needed.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            false, false, true,
            List.of("cleanup", "thumbnail", "cache", "explorer", "thumbcache"),
            buildCleanupThumbnailCacheBlock(),
            false
        ));

        registerAction(new DebloatAction(
            "cleanup_shader_cache",
            "Clear DirectX Shader Cache",
            "Clears the DirectX shader cache. Shaders will be regenerated by applications as needed. May briefly increase load times for games/apps on first launch.",
            ActionCategory.WINDOWS_CLEANUP, RiskLevel.SAFE,
            false, false, true,
            List.of("cleanup", "shader", "directx", "dx", "cache", "gpu"),
            buildCleanupShaderCacheBlock(),
            false
        ));
    }

    // --- Windows Cleanup PowerShell block builders ---

    private String buildCleanupWindowsTempBlock() {
        return """
            $actionId = "cleanup_windows_temp"
            $result = New-ActionResult -id $actionId -title "Remove Windows Temporary Files" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $tempPath = "$env:SystemRoot\\Temp"
                if (!(Test-Path $tempPath)) {
                    Skip-ActionResult -result $result -message "Windows temp folder not found." -details "Path does not exist: $tempPath"
                } else {
                    $items = Get-ChildItem -Path $tempPath -Recurse -Force -ErrorAction SilentlyContinue
                    if ($items.Count -eq 0) {
                        AlreadyApplied-ActionResult -result $result -message "Windows temp folder is already empty." -details "No files found in $tempPath."
                    } else {
                        $removed = 0
                        $skipped = 0
                        $sizeRemoved = 0
                        foreach ($item in $items) {
                            try {
                                $itemSize = 0
                                if (!$item.PSIsContainer) { $itemSize = $item.Length }
                                Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction Stop
                                $removed++
                                $sizeRemoved += $itemSize
                            } catch {
                                $skipped++
                            }
                        }
                        $sizeMB = [math]::Round($sizeRemoved / 1MB, 1)
                        if ($removed -eq 0) {
                            Partial-ActionResult -result $result -message "Could not remove any files (all locked)." -details "Skipped $skipped locked files in $tempPath."
                        } elseif ($skipped -gt 0) {
                            Partial-ActionResult -result $result -message "Removed $removed items ($sizeMB MB). Skipped $skipped locked files." -details "Cleaned $tempPath. Some files were in use."
                        } else {
                            Complete-ActionResult -result $result -message "Removed $removed items ($sizeMB MB)." -details "Cleaned $tempPath completely."
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean Windows temp files." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupUserTempBlock() {
        return """
            $actionId = "cleanup_user_temp"
            $result = New-ActionResult -id $actionId -title "Remove Current User Temp Files" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $tempPath = $env:TEMP
                if (!(Test-Path $tempPath)) {
                    Skip-ActionResult -result $result -message "User temp folder not found." -details "Path does not exist: $tempPath"
                } else {
                    $items = Get-ChildItem -Path $tempPath -Recurse -Force -ErrorAction SilentlyContinue
                    if ($items.Count -eq 0) {
                        AlreadyApplied-ActionResult -result $result -message "User temp folder is already empty." -details "No files found in $tempPath."
                    } else {
                        $removed = 0
                        $skipped = 0
                        $sizeRemoved = 0
                        foreach ($item in $items) {
                            try {
                                $itemSize = 0
                                if (!$item.PSIsContainer) { $itemSize = $item.Length }
                                Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction Stop
                                $removed++
                                $sizeRemoved += $itemSize
                            } catch {
                                $skipped++
                            }
                        }
                        $sizeMB = [math]::Round($sizeRemoved / 1MB, 1)
                        if ($removed -eq 0) {
                            Partial-ActionResult -result $result -message "Could not remove any files (all locked)." -details "Skipped $skipped locked files in $tempPath."
                        } elseif ($skipped -gt 0) {
                            Partial-ActionResult -result $result -message "Removed $removed items ($sizeMB MB). Skipped $skipped locked files." -details "Cleaned $tempPath. Some files were in use."
                        } else {
                            Complete-ActionResult -result $result -message "Removed $removed items ($sizeMB MB)." -details "Cleaned $tempPath completely."
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean user temp files." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupWuCacheBlock() {
        return """
            $actionId = "cleanup_wu_cache"
            $result = New-ActionResult -id $actionId -title "Remove Windows Update Download Cache" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $downloadPath = "$env:SystemRoot\\SoftwareDistribution\\Download"
                if (!(Test-Path $downloadPath)) {
                    Skip-ActionResult -result $result -message "Windows Update download cache not found." -details "Path does not exist: $downloadPath"
                } else {
                    $items = Get-ChildItem -Path $downloadPath -Recurse -Force -ErrorAction SilentlyContinue
                    if ($items.Count -eq 0) {
                        AlreadyApplied-ActionResult -result $result -message "Windows Update download cache is already empty." -details "No files in $downloadPath."
                    } else {
                        # Track previous service states
                        $servicesToStop = @("wuauserv", "bits", "dosvc")
                        $previousStates = @{}
                        foreach ($svcName in $servicesToStop) {
                            $svc = Get-Service -Name $svcName -ErrorAction SilentlyContinue
                            if ($svc) {
                                $previousStates[$svcName] = $svc.Status
                            }
                        }

                        # Stop services that are running
                        $stopFailed = $false
                        foreach ($svcName in $servicesToStop) {
                            $svc = Get-Service -Name $svcName -ErrorAction SilentlyContinue
                            if ($svc -and $svc.Status -eq 'Running') {
                                try {
                                    Stop-Service -Name $svcName -Force -ErrorAction Stop
                                } catch {
                                    $stopFailed = $true
                                }
                            }
                        }

                        if ($stopFailed) {
                            # Try to restart services that were running before
                            foreach ($svcName in $servicesToStop) {
                                if ($previousStates.ContainsKey($svcName) -and $previousStates[$svcName] -eq 'Running') {
                                    Start-Service -Name $svcName -ErrorAction SilentlyContinue
                                }
                            }
                            Fail-ActionResult -result $result -message "Could not stop Windows Update services." -error "One or more services could not be stopped. Cache was not cleared."
                        } else {
                            Start-Sleep -Seconds 2
                            $removed = 0
                            $skipped = 0
                            $sizeRemoved = 0
                            $dlItems = Get-ChildItem -Path $downloadPath -Recurse -Force -ErrorAction SilentlyContinue
                            foreach ($item in $dlItems) {
                                try {
                                    $itemSize = 0
                                    if (!$item.PSIsContainer) { $itemSize = $item.Length }
                                    Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction Stop
                                    $removed++
                                    $sizeRemoved += $itemSize
                                } catch {
                                    $skipped++
                                }
                            }
                            $sizeMB = [math]::Round($sizeRemoved / 1MB, 1)

                            # Restart services that were previously running
                            $restartDetails = @()
                            foreach ($svcName in $servicesToStop) {
                                if ($previousStates.ContainsKey($svcName) -and $previousStates[$svcName] -eq 'Running') {
                                    try {
                                        Start-Service -Name $svcName -ErrorAction Stop
                                        $restartDetails += "$svcName restarted"
                                    } catch {
                                        $restartDetails += "$svcName failed to restart"
                                    }
                                }
                            }
                            $restartInfo = ($restartDetails -join "; ")

                            if ($removed -eq 0 -and $skipped -gt 0) {
                                Partial-ActionResult -result $result -message "Could not remove any cached files." -details "Skipped $skipped locked files. Services: $restartInfo."
                            } elseif ($skipped -gt 0) {
                                Partial-ActionResult -result $result -message "Removed $removed items ($sizeMB MB). Skipped $skipped locked files." -details "Cleaned $downloadPath. $restartInfo."
                            } else {
                                Complete-ActionResult -result $result -message "Removed $removed items ($sizeMB MB)." -details "Cleaned $downloadPath. $restartInfo."
                            }
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean Windows Update cache." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupDoCacheBlock() {
        return """
            $actionId = "cleanup_do_cache"
            $result = New-ActionResult -id $actionId -title "Remove Delivery Optimization Cache" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $doPath = "$env:SystemRoot\\ServiceProfiles\\NetworkService\\AppData\\Local\\Microsoft\\Windows\\DeliveryOptimization\\Cache"
                if (!(Test-Path $doPath)) {
                    Skip-ActionResult -result $result -message "Delivery Optimization cache not found." -details "Path does not exist: $doPath. Cache may already be empty or Delivery Optimization is not used."
                } else {
                    $items = Get-ChildItem -Path $doPath -Recurse -Force -ErrorAction SilentlyContinue
                    if ($items.Count -eq 0) {
                        AlreadyApplied-ActionResult -result $result -message "Delivery Optimization cache is already empty." -details "No files in $doPath."
                    } else {
                        $removed = 0
                        $skipped = 0
                        $sizeRemoved = 0
                        foreach ($item in $items) {
                            try {
                                $itemSize = 0
                                if (!$item.PSIsContainer) { $itemSize = $item.Length }
                                Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction Stop
                                $removed++
                                $sizeRemoved += $itemSize
                            } catch {
                                $skipped++
                            }
                        }
                        $sizeMB = [math]::Round($sizeRemoved / 1MB, 1)
                        if ($removed -eq 0) {
                            Partial-ActionResult -result $result -message "Could not remove any cache files." -details "Skipped $skipped locked files in $doPath."
                        } elseif ($skipped -gt 0) {
                            Partial-ActionResult -result $result -message "Removed $removed items ($sizeMB MB). Skipped $skipped locked files." -details "Cleaned $doPath."
                        } else {
                            Complete-ActionResult -result $result -message "Removed $removed items ($sizeMB MB)." -details "Cleaned $doPath completely."
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean Delivery Optimization cache." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupUpgradeTempBlock() {
        return """
            $actionId = "cleanup_upgrade_temp"
            $result = New-ActionResult -id $actionId -title "Remove Windows Upgrade Temporary Files" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                # Check if Windows setup/update appears to be running
                $setupRunning = Get-Process -Name "SetupHost","SetupPrep","Windows10UpgraderApp" -ErrorAction SilentlyContinue
                if ($setupRunning) {
                    Skip-ActionResult -result $result -message "Windows setup appears to be running." -details "Active setup processes detected: $(($setupRunning | Select-Object -ExpandProperty Name) -join ', '). Skipping to avoid interfering with an active upgrade."
                } else {
                    $drive = $env:SystemDrive
                    $paths = @("$drive\\\u0024Windows.~BT", "$drive\\\u0024Windows.~WS")
                    $totalRemoved = 0
                    $totalSize = 0
                    $details = @()
                    $anyFound = $false

                    foreach ($path in $paths) {
                        if (Test-Path $path) {
                            $anyFound = $true
                            try {
                                $folderSize = (Get-ChildItem -Path $path -Recurse -Force -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum -ErrorAction SilentlyContinue).Sum
                                if ($null -eq $folderSize) { $folderSize = 0 }
                                Remove-Item -Path $path -Recurse -Force -ErrorAction Stop
                                $sizeMB = [math]::Round($folderSize / 1MB, 1)
                                $totalRemoved++
                                $totalSize += $folderSize
                                $details += "Removed $path ($sizeMB MB)"
                            } catch {
                                $details += "Failed to remove ${path}: $($_.Exception.Message)"
                            }
                        }
                    }

                    if (-not $anyFound) {
                        AlreadyApplied-ActionResult -result $result -message "No upgrade temp folders found." -details "Neither `$Windows.~BT nor `$Windows.~WS exist on $drive."
                    } elseif ($totalRemoved -eq 0) {
                        Partial-ActionResult -result $result -message "Found upgrade folders but could not remove them." -details ($details -join " | ")
                    } else {
                        $totalMB = [math]::Round($totalSize / 1MB, 1)
                        Complete-ActionResult -result $result -message "Removed $totalRemoved upgrade temp folder(s) ($totalMB MB)." -details ($details -join " | ")
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean upgrade temp files." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupSetupLogsBlock() {
        return """
            $actionId = "cleanup_setup_logs"
            $result = New-ActionResult -id $actionId -title "Remove Setup Logs Older Than 30 Days" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $logPaths = @(
                    "$env:SystemRoot\\Logs\\CBS",
                    "$env:SystemRoot\\Logs\\DISM",
                    "$env:SystemRoot\\Logs\\WindowsUpdate"
                )
                $cutoffDate = (Get-Date).AddDays(-30)
                $totalRemoved = 0
                $totalSkipped = 0
                $totalSize = 0

                foreach ($logPath in $logPaths) {
                    if (Test-Path $logPath) {
                        $oldFiles = Get-ChildItem -Path $logPath -File -Force -ErrorAction SilentlyContinue | Where-Object { $_.LastWriteTime -lt $cutoffDate }
                        foreach ($file in $oldFiles) {
                            try {
                                $totalSize += $file.Length
                                Remove-Item -Path $file.FullName -Force -ErrorAction Stop
                                $totalRemoved++
                            } catch {
                                $totalSkipped++
                            }
                        }
                    }
                }

                $sizeMB = [math]::Round($totalSize / 1MB, 1)
                if ($totalRemoved -eq 0 -and $totalSkipped -eq 0) {
                    Skip-ActionResult -result $result -message "No setup logs older than 30 days found." -details "Checked CBS, DISM, and WindowsUpdate log folders."
                } elseif ($totalRemoved -eq 0 -and $totalSkipped -gt 0) {
                    Partial-ActionResult -result $result -message "Found old logs but could not remove them." -details "Skipped $totalSkipped locked log files."
                } elseif ($totalSkipped -gt 0) {
                    Partial-ActionResult -result $result -message "Removed $totalRemoved log files ($sizeMB MB). Skipped $totalSkipped." -details "Cleaned logs older than 30 days from CBS, DISM, WindowsUpdate."
                } else {
                    Complete-ActionResult -result $result -message "Removed $totalRemoved log files ($sizeMB MB)." -details "Cleaned logs older than 30 days from CBS, DISM, WindowsUpdate."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean setup logs." -error $_.Exception.Message
            }
            """;
    }

    private String buildDismStartComponentCleanupBlock() {
        return """
            $actionId = "cleanup_dism_startcomponentcleanup"
            $result = New-ActionResult -id $actionId -title "Run DISM StartComponentCleanup" -category "Windows Cleanup" -riskLevel "Moderate"
            $result.restartRecommended = $true
            try {
                $dismOutput = & DISM.exe /Online /Cleanup-Image /StartComponentCleanup 2>&1
                $exitCode = $LASTEXITCODE
                $outputText = ($dismOutput | Out-String).Trim()
                if ($exitCode -eq 0) {
                    Complete-ActionResult -result $result -message "DISM StartComponentCleanup completed successfully." -details $outputText
                } else {
                    Fail-ActionResult -result $result -message "DISM exited with code $exitCode." -error $outputText
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to run DISM StartComponentCleanup." -error $_.Exception.Message
            }
            """;
    }

    private String buildDismResetBaseBlock() {
        return """
            $actionId = "cleanup_dism_resetbase"
            $result = New-ActionResult -id $actionId -title "Run DISM StartComponentCleanup with ResetBase" -category "Windows Cleanup" -riskLevel "Aggressive"
            $result.restartRecommended = $true
            try {
                $dismOutput = & DISM.exe /Online /Cleanup-Image /StartComponentCleanup /ResetBase 2>&1
                $exitCode = $LASTEXITCODE
                $outputText = ($dismOutput | Out-String).Trim()
                if ($exitCode -eq 0) {
                    Complete-ActionResult -result $result -message "DISM ResetBase completed successfully. Installed updates may no longer be uninstallable." -details $outputText
                } else {
                    Fail-ActionResult -result $result -message "DISM exited with code $exitCode." -error $outputText
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to run DISM ResetBase." -error $_.Exception.Message
            }
            """;
    }

    private String buildEmptyRecycleBinBlock() {
        return """
            $actionId = "cleanup_empty_recycle_bin"
            $result = New-ActionResult -id $actionId -title "Empty Recycle Bin" -category "Windows Cleanup" -riskLevel "Aggressive"
            try {
                $shell = New-Object -ComObject Shell.Application
                $recycleBin = $shell.Namespace(0x0A)
                $itemCount = $recycleBin.Items().Count
                if ($itemCount -eq 0) {
                    Skip-ActionResult -result $result -message "Recycle Bin is already empty." -details "No items found in the Recycle Bin."
                } else {
                    Clear-RecycleBin -Force -ErrorAction Stop
                    Complete-ActionResult -result $result -message "Recycle Bin emptied." -details "Removed $itemCount item(s) from the Recycle Bin."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to empty Recycle Bin." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupPreviousWindowsBlock() {
        return """
            $actionId = "cleanup_previous_windows"
            $result = New-ActionResult -id $actionId -title "Remove Previous Windows Installation Files" -category "Windows Cleanup" -riskLevel "Aggressive"
            try {
                $windowsOld = "$env:SystemDrive\\Windows.old"
                if (!(Test-Path $windowsOld)) {
                    Skip-ActionResult -result $result -message "No previous Windows installation found." -details "C:\\Windows.old does not exist on this system."
                } else {
                    # Use Windows built-in Disk Cleanup (cleanmgr) with the Previous Installations flag.
                    # StateFlags 5000 is a custom flag index we set to mark "Previous Windows installation(s)" for cleanup.
                    $regPath = "HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Explorer\\VolumeCaches\\Previous Installations"
                    if (Test-Path $regPath) {
                        Set-ItemProperty -Path $regPath -Name "StateFlags0065" -Value 2 -Type DWord -ErrorAction SilentlyContinue
                    }

                    $cleanmgrProcess = Start-Process -FilePath "cleanmgr.exe" -ArgumentList "/sagerun:65" -Wait -PassThru -NoNewWindow -ErrorAction Stop
                    $exitCode = $cleanmgrProcess.ExitCode

                    # Clean up the StateFlags registry value
                    if (Test-Path $regPath) {
                        Remove-ItemProperty -Path $regPath -Name "StateFlags0065" -ErrorAction SilentlyContinue
                    }

                    if (!(Test-Path $windowsOld)) {
                        Complete-ActionResult -result $result -message "Previous Windows installation files removed." -details "Windows.old has been cleaned up via Disk Cleanup (cleanmgr sagerun). Rollback to previous Windows version is no longer possible."
                    } else {
                        # Check if folder size decreased significantly
                        $remainingItems = (Get-ChildItem -Path $windowsOld -Recurse -Force -ErrorAction SilentlyContinue).Count
                        if ($remainingItems -eq 0) {
                            Complete-ActionResult -result $result -message "Previous Windows installation files removed." -details "Windows.old folder is now empty."
                        } else {
                            Partial-ActionResult -result $result -message "Disk Cleanup ran but Windows.old still has $remainingItems items." -details "cleanmgr exited with code $exitCode. Some files may require a reboot to fully remove, or may be protected."
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to remove previous Windows installation files." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupCrashDumpsBlock() {
        return """
            $actionId = "cleanup_crash_dumps"
            $result = New-ActionResult -id $actionId -title "Clear Old Crash Dump Files" -category "Windows Cleanup" -riskLevel "Moderate"
            try {
                $dumpPaths = @(
                    "$env:SystemRoot\\Minidump",
                    "$env:SystemRoot\\MEMORY.DMP"
                )
                $totalRemoved = 0
                $totalSize = 0
                $details = @()

                # Minidump folder
                $minidumpPath = "$env:SystemRoot\\Minidump"
                if (Test-Path $minidumpPath) {
                    $dumpFiles = Get-ChildItem -Path $minidumpPath -File -Force -ErrorAction SilentlyContinue
                    foreach ($file in $dumpFiles) {
                        try {
                            $totalSize += $file.Length
                            Remove-Item -Path $file.FullName -Force -ErrorAction Stop
                            $totalRemoved++
                        } catch {
                            $details += "Skipped (in use): $($file.Name)"
                        }
                    }
                }

                # Main memory dump
                $memoryDmp = "$env:SystemRoot\\MEMORY.DMP"
                if (Test-Path $memoryDmp) {
                    try {
                        $fileSize = (Get-Item $memoryDmp -Force).Length
                        Remove-Item -Path $memoryDmp -Force -ErrorAction Stop
                        $totalSize += $fileSize
                        $totalRemoved++
                    } catch {
                        $details += "Skipped MEMORY.DMP (in use or locked)"
                    }
                }

                $sizeMB = [math]::Round($totalSize / 1MB, 1)
                if ($totalRemoved -eq 0 -and $details.Count -eq 0) {
                    Skip-ActionResult -result $result -message "No crash dump files found." -details "No minidump files or MEMORY.DMP found."
                } elseif ($totalRemoved -eq 0) {
                    Partial-ActionResult -result $result -message "Found dump files but could not remove them." -details ($details -join " | ")
                } elseif ($details.Count -gt 0) {
                    Partial-ActionResult -result $result -message "Removed $totalRemoved dump file(s) ($sizeMB MB). Some skipped." -details ($details -join " | ")
                } else {
                    Complete-ActionResult -result $result -message "Removed $totalRemoved crash dump file(s) ($sizeMB MB)." -details "Cleaned minidump files and MEMORY.DMP."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clean crash dump files." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupThumbnailCacheBlock() {
        return """
            $actionId = "cleanup_thumbnail_cache"
            $result = New-ActionResult -id $actionId -title "Clear Thumbnail Cache" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $thumbCachePath = "$env:LOCALAPPDATA\\Microsoft\\Windows\\Explorer"
                if (!(Test-Path $thumbCachePath)) {
                    Skip-ActionResult -result $result -message "Thumbnail cache folder not found." -details "Path does not exist: $thumbCachePath"
                } else {
                    $thumbFiles = Get-ChildItem -Path $thumbCachePath -Filter "thumbcache_*.db" -Force -ErrorAction SilentlyContinue
                    if ($thumbFiles.Count -eq 0) {
                        AlreadyApplied-ActionResult -result $result -message "No thumbnail cache files found." -details "No thumbcache_*.db files in $thumbCachePath."
                    } else {
                        $removed = 0
                        $skipped = 0
                        $sizeRemoved = 0
                        foreach ($file in $thumbFiles) {
                            try {
                                $sizeRemoved += $file.Length
                                Remove-Item -Path $file.FullName -Force -ErrorAction Stop
                                $removed++
                            } catch {
                                $skipped++
                            }
                        }
                        $sizeMB = [math]::Round($sizeRemoved / 1MB, 1)
                        if ($removed -eq 0) {
                            Partial-ActionResult -result $result -message "Could not remove thumbnail cache (files in use)." -details "Skipped $skipped locked files. Thumbnails may be in use by Explorer."
                        } elseif ($skipped -gt 0) {
                            Partial-ActionResult -result $result -message "Removed $removed thumbnail cache files ($sizeMB MB). Skipped $skipped." -details "Thumbnails will be regenerated by Windows as needed."
                        } else {
                            Complete-ActionResult -result $result -message "Removed $removed thumbnail cache files ($sizeMB MB)." -details "Thumbnails will be regenerated by Windows as needed."
                        }
                    }
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clear thumbnail cache." -error $_.Exception.Message
            }
            """;
    }

    private String buildCleanupShaderCacheBlock() {
        return """
            $actionId = "cleanup_shader_cache"
            $result = New-ActionResult -id $actionId -title "Clear DirectX Shader Cache" -category "Windows Cleanup" -riskLevel "Safe"
            try {
                $shaderCachePaths = @(
                    "$env:LOCALAPPDATA\\D3DSCache",
                    "$env:LOCALAPPDATA\\NVIDIA\\DXCache",
                    "$env:LOCALAPPDATA\\NVIDIA\\GLCache",
                    "$env:LOCALAPPDATA\\AMD\\DxCache",
                    "$env:LOCALAPPDATA\\AMD\\GLCache"
                )
                $totalRemoved = 0
                $totalSkipped = 0
                $totalSize = 0
                $pathsCleaned = @()

                foreach ($cachePath in $shaderCachePaths) {
                    if (Test-Path $cachePath) {
                        $items = Get-ChildItem -Path $cachePath -Recurse -Force -ErrorAction SilentlyContinue
                        foreach ($item in $items) {
                            try {
                                $itemSize = 0
                                if (!$item.PSIsContainer) { $itemSize = $item.Length }
                                Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction Stop
                                $totalRemoved++
                                $totalSize += $itemSize
                            } catch {
                                $totalSkipped++
                            }
                        }
                        $pathsCleaned += $cachePath
                    }
                }

                $sizeMB = [math]::Round($totalSize / 1MB, 1)
                if ($pathsCleaned.Count -eq 0) {
                    Skip-ActionResult -result $result -message "No shader cache folders found." -details "No DirectX/GPU shader cache directories exist."
                } elseif ($totalRemoved -eq 0 -and $totalSkipped -eq 0) {
                    AlreadyApplied-ActionResult -result $result -message "Shader cache folders are already empty." -details "Checked: $($pathsCleaned -join ', ')"
                } elseif ($totalRemoved -eq 0) {
                    Partial-ActionResult -result $result -message "Could not remove shader cache files." -details "Skipped $totalSkipped locked files."
                } elseif ($totalSkipped -gt 0) {
                    Partial-ActionResult -result $result -message "Removed $totalRemoved items ($sizeMB MB). Skipped $totalSkipped." -details "Shaders will be regenerated by applications as needed."
                } else {
                    Complete-ActionResult -result $result -message "Removed $totalRemoved shader cache items ($sizeMB MB)." -details "Shaders will be regenerated by applications as needed."
                }
            } catch {
                Fail-ActionResult -result $result -message "Failed to clear shader cache." -error $_.Exception.Message
            }
            """;
    }
}

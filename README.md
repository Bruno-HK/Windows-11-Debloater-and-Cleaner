# Windows 11 Debloater

A safe, selective Windows 11 debloat launcher with a modern JavaFX WebView UI.

## GitHub Description

Selective Windows 11 debloater with a JavaFX + WebView UI, PowerShell preview, profile import/export, live logs, and structured execution reports.

## Detailed Documentation

Implementation-driven project documentation is available in [docs/APP_DOCUMENTATION.md](docs/APP_DOCUMENTATION.md).

This application wraps the `win11_debloat.ps1` script into a desktop tool that lets you choose exactly what gets removed, disabled, or left alone. It does not blindly run the full script, it maps every operation into individual selectable actions and generates a new temporary PowerShell script based only on your selections.

## Safety Warning

- This tool modifies Windows system settings, removes packages, and disables services.
- Always create a **System Restore Point** before running (enabled by default).
- Review the **PowerShell Preview** before executing.
- Run on a test system first if possible.
- Major Windows feature updates may restore some settings. Re-run your profile after major updates if needed.
- Xbox packages are intentionally excluded (required for Game Pass / Forza Horizon).

## Requirements

- **Java 21** (JDK 21+)
- **Maven 3.8+**
- **Windows 11** (the debloat operations target Windows 11)

## How to Build

```bash
mvn clean package
```

This compiles the project and produces a JAR in `target/`.

## How to Run

The application uses JavaFX, so run it via the Maven JavaFX plugin:

```bash
mvn javafx:run
```

Or run the JAR directly (ensure JavaFX modules are on the module path):

```bash
java --module-path <path-to-javafx-sdk>/lib --add-modules javafx.controls,javafx.web -jar target/win11-debloater-1.0.0.jar
```

### Running as Administrator

For the application to actually execute debloat actions, it **must** run as Administrator:

1. Open **Terminal** or **Command Prompt** as Administrator.
2. Navigate to the project directory.
3. Run: `mvn javafx:run`

Alternatively, the app has a **Restart as Admin** button that attempts UAC elevation.

## How to Use

### Preview PowerShell

1. Select/deselect actions using the toggle switches.
2. Click **Preview PS** in the bottom bar.
3. A read-only modal shows the exact PowerShell that will be generated.
4. You can copy it to clipboard.

### Run Selected Actions

1. Ensure you are running as Administrator (check the badge in the top bar).
2. Select the actions you want to apply.
3. Click **Run Selected**.
4. Confirm in the dialog (aggressive actions are highlighted with a warning).
5. Watch live progress in the Log panel.
6. When complete, a detailed report modal shows per-action results.

### Import / Export Profiles

- **Export**: Click **Export** in the top bar. Saves your current selection as a JSON file.
- **Import**: Click **Import** in the top bar. Loads a profile and applies the selection.
- Profiles contain only action IDs (no raw PowerShell). Unknown IDs are ignored with a warning.

### Export Logs and Reports

After execution:
- **Save Log**: Saves the raw execution log as a text file.
- **Report JSON**: Exports structured results (per-action status, summary) as JSON.
- **Report TXT**: Exports a human-readable text report grouped by category.

## Adding New DebloatAction Entries

To add a new action:

1. Open `src/main/java/debloater/service/ActionRegistry.java`.
2. Find the appropriate `register*Actions()` method for the category.
3. Add a new `registerAction(new DebloatAction(...))` call.
4. Each action needs:
   - Unique `id` (snake_case)
   - `title` and `description`
   - `category` (from `ActionCategory` enum)
   - `riskLevel` (SAFE, MODERATE, AGGRESSIVE)
   - `selectedByDefault` (true for recommended, false for aggressive/optional)
   - `restartRecommended` flag
   - `requiresAdmin` flag
   - `tags` for search
   - PowerShell block using the structured result helpers
   - `needsRegistryHelper` flag (true if the PS block calls `Ensure-RegistryPath`)
5. The PowerShell block must use the helper functions (`New-ActionResult`, `Complete-ActionResult`, etc.) to emit structured JSON results.
6. Rebuild with `mvn clean package`.

## Architecture

```
JavaFX Shell (MainApp)
  └── WebView (loads local HTML/CSS/JS)
        └── JavaScript UI (renders actions from Java, sends IDs only)
              └── window.bridge (AppBridge.java)
                    └── ActionRegistry (all actions defined in Java)
                    └── PowerShellScriptBuilder (generates PS from selected actions)
                    └── PowerShellRunner (executes PS, parses results)
                    └── ProfileService (import/export profiles)
                    └── ReportService (generate JSON/TXT reports)
```

**Security**: The WebView only loads bundled local resources. It cannot execute PowerShell directly. JavaScript sends only action IDs to Java. Java validates all IDs against the ActionRegistry and rejects unknowns.

## Project Structure

```
src/main/java/
  module-info.java
  debloater/
    MainApp.java                    — JavaFX application entry point
    bridge/
      AppBridge.java                — Java-to-JavaScript bridge (narrow API)
    model/
      RiskLevel.java                — Safe / Moderate / Aggressive enum
      ActionCategory.java           — Category enum with display order
      DebloatAction.java            — Action model (id, title, PS block, etc.)
      ActionExecutionResult.java    — Per-action result parsed from PS output
      ExecutionSummary.java         — Aggregate result counts
    service/
      ActionRegistry.java           — Central registry of all debloat actions
      PowerShellScriptBuilder.java  — Generates PS scripts from selections
      PowerShellRunner.java         — Executes PS, captures output, parses results
      AdminChecker.java             — Checks/elevates admin privileges
      ProfileService.java           — Import/export selection profiles
      ReportService.java            — Generate JSON/TXT reports

src/main/resources/web/
  index.html                        — Main UI layout
  styles.css                        — Dark theme styles
  app.js                            — UI logic (renders from Java data, sends IDs only)
```

## Known Limitations

- The app generates and runs PowerShell scripts. It cannot undo changes after execution (use the restore point).
- Some packages may be re-installed by Windows Update. Re-run the profile after major updates.
- The "Restart as Admin" button depends on the UAC prompt being accepted.
- The app requires JavaFX WebView, which includes a Chromium-based engine — this increases the JAR/runtime size.
- Profiles are simple JSON arrays of action IDs. They do not store toggle order or custom metadata.
- The application must be run on Windows 11 for the debloat operations to work.

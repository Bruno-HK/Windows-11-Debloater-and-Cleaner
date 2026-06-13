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

## Quick Start (No Java Required)

1. Download the latest release `.zip` from the [Releases](../../releases) page.
2. Unzip the archive.
3. Right-click **Win11Debloater.exe** and select **Run as administrator**.

That's it, no Java installation needed. The release includes a bundled JRE.

**Note:** This application was tested on a Windows 11 VM that was upgraded from Windows 10 (not a clean install). If you are running a clean Windows 11 installation, deselect the **Windows Cleanup** category — those actions target upgrade leftovers that won't exist on a fresh install.

## Requirements

### For development
- **Java 21+** (JDK)
- **Maven 3.8+**
- **Windows 11** (the debloat operations target Windows 11)

### For the portable build (no Java needed on target machine)
- Just copy the `Win11Debloater` folder and run `Win11Debloater.exe`

## How to Build

```bash
mvn clean package
```

This compiles the project and produces a shaded (fat) JAR in `target/`.

### Portable Build (Bundled JRE)

To build a self-contained portable app that includes its own JRE (no Java installation required on the target machine):

```bash
build-portable.bat
```

This runs Maven and then `jpackage` to produce a standalone app image at:

```
release/app-image/Win11Debloater/
├── Win11Debloater.exe
├── app/          (JAR + config)
└── runtime/      (bundled JRE)
```

Copy the entire `Win11Debloater` folder to any Windows machine and run the `.exe` directly.

## How to Run

### From source (development)

The application uses JavaFX, so run it via the Maven JavaFX plugin:

```bash
mvn javafx:run
```

Or run the shaded JAR directly:

```bash
java -jar target/win11-debloater-1.0.0.jar
```

### Portable (no Java required)

Double-click `Win11Debloater.exe` in the portable app folder, or run it from the command line.

### Running as Administrator

For the application to actually execute debloat actions, it **must** run as Administrator:

1. Open **Terminal** or **Command Prompt** as Administrator.
2. Navigate to the project directory.
3. Run: `mvn javafx:run` (dev) or launch `Win11Debloater.exe` (portable).

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
    Launcher.java                   — Non-JavaFX entry point for fat JAR / jpackage
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

## Windows Cleanup

The **Windows Cleanup** module is designed for users who upgraded from Windows 10 to Windows 11 and want to remove old upgrade leftovers and reduce residual files as much as safely possible.

### What It Does

- Clears Windows and user temp files (skipping locked files)
- Removes Windows Update download cache (safely stops/restarts services)
- Removes Delivery Optimization cache
- Removes Windows upgrade temporary folders (`$Windows.~BT`, `$Windows.~WS`)
- Removes old setup logs (older than 30 days only)
- Runs DISM component cleanup and ResetBase (optional, off by default)
- Empties the Recycle Bin (optional, off by default)
- Removes previous Windows installation files via Disk Cleanup (optional, off by default)
- Clears crash dumps, thumbnail cache, and DirectX shader cache (optional)

### What It Does NOT Do

- **This does not make an upgraded system identical to a true fresh install.** A real clean install is fundamentally different. This module gets the system closer by removing residual files, but registry entries, driver remnants, and other artifacts from the upgrade process may remain.
- Does **not** delete any user files (Downloads, Documents, Desktop, Pictures, Videos, Music, browser profiles, game saves, or user profile data).
- Does **not** modify Windows configuration or registry settings (unlike the debloat actions).

### Warnings

- **Previous Windows installation removal**: After removing `Windows.old`, you can no longer roll back to the previous Windows version. This action is Aggressive and OFF by default.
- **DISM ResetBase**: After running `/ResetBase`, installed Windows updates may no longer be uninstallable. This action is Aggressive and OFF by default.
- **A true fresh install is still different**: Even after running all Windows Cleanup actions, the system will not be identical to a fresh Windows 11 installation. If a clean state is critical, perform a fresh install instead.

### How to Use

1. Select the **Windows Cleanup** category in the left sidebar.
2. Safe cleanup actions are recommended ON by default.
3. Review and enable any optional/aggressive actions as needed.
4. **Create System Restore Point** (in the Safety category) is ON by default and strongly recommended before running any cleanup actions.
5. Click **Preview PS** to review the generated PowerShell before executing.
6. Click **Run Selected** (requires Administrator).

### Restore Point Handling

The "Create System Restore Point" action is defined in the **Safety** category and is selected by default. When running Windows Cleanup actions, it is strongly recommended to keep this action enabled. If System Protection is disabled on the C: drive, the restore point creation will fail and report a clear error — it will not continue silently.

## Known Limitations

- The app generates and runs PowerShell scripts. It cannot undo changes after execution (use the restore point).
- Some packages may be re-installed by Windows Update. Re-run the profile after major updates.
- The "Restart as Admin" button depends on the UAC prompt being accepted.
- The app requires JavaFX WebView, which includes a Chromium-based engine — this increases the JAR/runtime size.
- Profiles are simple JSON arrays of action IDs. They do not store toggle order or custom metadata.
- The application must be run on Windows 11 for the debloat operations to work.
- The portable build (`build-portable.bat`) has hardcoded paths to a local JDK and IntelliJ's bundled Maven. Edit the paths at the top of the script if your setup differs.

## Support

If this saved you time, stress, or one Windows reinstall, feel free to fund one of the medically questionable amounts of coffee I drink per day.

[Buy me a coffee on Ko-fi](https://ko-fi.com/hytsu)

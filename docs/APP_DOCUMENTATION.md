# Windows 11 Debloater Documentation

## Overview

Windows 11 Debloater is a desktop Java application that wraps a large Windows debloat PowerShell script in a safer, selective workflow. Instead of running one monolithic script, the app exposes each supported operation as a separate action, lets the user toggle only the changes they want, generates a temporary PowerShell script from those selections, previews that script, and then executes it with structured reporting.

The project is built as a Java 21 modular application with a JavaFX shell and a WebView-based local HTML/CSS/JavaScript interface. The UI never executes raw PowerShell and never receives trusted script blocks directly. All privileged logic stays in Java.

## What The App Does

The app currently exposes 85 debloat actions across 10 categories:

- `Safety`: 1 action
- `Bloat Apps`: 23 actions
- `Widgets`: 3 actions
- `Copilot & AI Apps`: 4 actions
- `AI Features (Registry)`: 11 actions
- `OneDrive`: 4 actions
- `Ads & Sponsored Content`: 20 actions
- `Privacy & Telemetry`: 7 actions
- `Services`: 3 actions
- `Scheduled Tasks`: 9 actions

These actions cover:

- Creating a restore point before changes
- Removing built-in Microsoft Store apps and promotional apps
- Disabling Widgets components and feeds
- Removing Copilot and Windows AI app packages
- Turning off Copilot, Recall, Bing search integration, and other AI-related shell features through registry or policy values
- Stopping, hiding, or uninstalling OneDrive
- Disabling Windows suggestions, spotlight content, recommended items, and search highlights
- Reducing telemetry, activity publishing, advertising ID usage, and app diagnostics access
- Disabling selected services and scheduled tasks tied to telemetry or legacy features

The registry intentionally excludes Xbox-related removals because the source action registry treats those packages as required for Game Pass and Forza-related use cases.

## User Experience

The app UI is a local web interface loaded inside JavaFX WebView. The user experience is organized around selection, preview, execution, and export.

### Main UI Areas

- Sidebar with category navigation and text search
- Top bar with admin status and profile controls
- Main content area with grouped action cards
- Bottom bar with preview, run, log, and export actions
- Modal dialogs for script preview, admin warning, execution confirmation, and final report

### Action Cards

Each action card includes:

- Title
- Description
- Risk badge: `Safe`, `Moderate`, or `Aggressive`
- Toggle state
- Result state after execution

During execution, selected cards move into a pending state. After execution, the toggle is replaced by a status icon and per-action outcome text.

### Search And Filtering

The frontend filters actions by:

- Title
- Description
- Category
- Action ID
- Tags

Categories can be viewed individually, and each category can be toggled in bulk from the content area.

## Execution Model

The key design choice in this app is that the user does not directly run the original `win11_debloat.ps1`. The app reads a curated registry of allowed actions and generates a new temporary script from the selected items.

### Flow

1. Java builds the in-memory action catalog from `ActionRegistry`.
2. Java serializes a safe subset of action metadata to JavaScript.
3. JavaScript renders the UI and stores only boolean selection state by action ID.
4. When the user previews or runs, JavaScript sends only selected IDs back to Java.
5. Java validates the IDs against `ActionRegistry`.
6. `PowerShellScriptBuilder` generates a temporary `.ps1` script containing only the chosen actions.
7. `PowerShellRunner` writes the script to a temp file and runs it through `powershell.exe`.
8. Action results are emitted as JSON lines, parsed back into Java objects, and streamed into the UI.
9. `ReportService` produces summary, JSON, text, and raw-log outputs.

### Admin Behavior

The app distinguishes between browsing and execution:

- Without elevation, users can browse actions, search, preview PowerShell, and import/export profiles.
- Running selected actions is blocked until the app is elevated.
- The UI exposes a restart-as-admin path, and Java also checks admin status before execution starts.

## Architecture

### High-Level Components

- `debloater.MainApp`
  Hosts the JavaFX window, creates the WebView, loads the local HTML UI, injects the Java bridge, and blocks external navigation.

- `debloater.bridge.AppBridge`
  The single public interface from JavaScript into Java. It returns action metadata, checks admin status, previews scripts, runs selected actions, imports and exports profiles, saves logs, exports reports, and triggers restart-as-admin.

- `debloater.service.ActionRegistry`
  The authoritative catalog of debloat actions. Every UI entry comes from here. Each action contains metadata plus a trusted PowerShell block.

- `debloater.service.PowerShellScriptBuilder`
  Builds the final execution script. It adds script headers, an admin check, optional helper functions, structured result helpers, and the selected action blocks.

- `debloater.service.PowerShellRunner`
  Writes the generated script to a temporary file, runs PowerShell in a background thread, reads stdout and stderr, parses structured result lines, and streams progress back to the UI.

- `debloater.service.ProfileService`
  Imports and exports selection profiles as JSON arrays of action IDs.

- `debloater.service.ReportService`
  Builds aggregate execution summaries and exports JSON, plain-text, and raw-log outputs.

- `debloater.service.AdminChecker`
  Determines whether the current process is elevated and attempts a UAC restart when requested.

### Frontend Structure

- `src/main/resources/web/index.html`
  Defines the UI shell and modal layout.

- `src/main/resources/web/app.js`
  Holds client-side state, renders cards and categories, manages filters, calls the Java bridge, shows logs, and updates action cards live during execution.

- `src/main/resources/web/styles.css`
  Styles the interface and state-driven visuals for risk and result badges.

## Data Model

### `DebloatAction`

Each action contains:

- `id`
- `title`
- `description`
- `category`
- `riskLevel`
- `selectedByDefault`
- `restartRecommended`
- `requiresAdmin`
- `tags`
- `powerShellBlock`
- `needsRegistryHelper`

`powerShellBlock` is never exposed to JavaScript.

### `ActionExecutionResult`

Each PowerShell action emits a structured JSON result with:

- `id`
- `title`
- `category`
- `riskLevel`
- `status`
- `message`
- `details`
- `error`
- `restartRecommended`
- `timestamp`

Supported result states are:

- `Success`
- `Failed`
- `Skipped`
- `Partially completed`
- `Already applied`

### `ExecutionSummary`

The summary aggregates:

- Total actions
- Success count
- Failed count
- Skipped count
- Partial count
- Already applied count
- Whether a restart is recommended

## Security Model

This project is designed around narrowing what the UI is allowed to ask the backend to do.

### Trust Boundaries

- The WebView only loads local bundled resources.
- External navigation is blocked in `MainApp`.
- JavaScript never sends raw PowerShell to Java.
- JavaScript sends only action IDs and simple UI requests.
- Java validates all incoming action IDs against the registry.
- Unknown IDs are rejected and logged.
- PowerShell blocks are stored in Java source, not user-editable profile data.

### Profiles

Profiles contain only action IDs. Importing a profile does not import scripts or arbitrary commands. Unknown IDs are ignored.

### Generated Script Safety

The generated script includes:

- An elevation check
- Shared helper functions
- Structured action result emitters
- Per-action try/catch logic
- Continuation after individual action failures

This means one failed action does not stop the rest of the selected actions from running.

## Action Categories

### Safety

Currently contains restore point creation. This is intended to be a first-line rollback mechanism before making system changes.

### Bloat Apps

Contains individual removals for built-in or promoted apps such as Teams, Cortana, Clipchamp, Bing-related packages, Solitaire, Disney+, Spotify, Skype, News, Weather, Maps, Get Help, Feedback Hub, Mixed Reality, Office Hub, OneNote, Groove Music, Movies & TV, People, Wallet, Sound Recorder, Dev Home, and optionally Windows Terminal.

### Widgets

Contains actions to:

- Remove Windows Web Experience Pack
- Hide the Widgets taskbar button
- Disable the Widgets news feed

### Copilot & AI Apps

Targets package removal for:

- Generic Copilot packages
- Windows AI packages
- `Microsoft.Windows.Copilot`
- `Microsoft.Copilot`

### AI Features (Registry)

Targets shell and policy changes for:

- Machine and user policy disablement for Copilot
- Machine and user policy disablement for Recall or AI data analysis
- Hiding the Copilot taskbar button
- Removing the Copilot context menu entry
- Disabling Bing search integration
- Disabling Cortana consent
- Disabling web search by policy
- Disabling the dynamic search box
- Disabling smart clipboard actions

### OneDrive

Contains separate actions to:

- Stop the OneDrive process
- Uninstall OneDrive
- Remove its startup entry
- Hide or disable Explorer integration

OneDrive uninstall is marked `Aggressive` and is not enabled by default.

### Ads & Sponsored Content

This category focuses on Content Delivery Manager and related shell promotion surfaces, including:

- Tips and suggestions
- Spotlight tips
- Start menu suggestions and recommended content
- OEM or preinstalled promotions
- Silent app installs
- Rotating lock screen content
- Lock screen slideshow
- Recently added app suggestions
- File Explorer sync provider ads
- Meet Now or Chat taskbar presence
- Search highlights
- Dynamic search box ad-like suggestions

### Privacy & Telemetry

Targets policy and privacy controls such as:

- Telemetry
- Advertising ID
- Activity history feed
- Publishing user activities
- Uploading user activities
- Location tracking
- App diagnostics access

### Services

Contains service-level toggles for:

- SysMain
- Fax
- Remote Registry

### Scheduled Tasks

Disables selected scheduled tasks tied to compatibility analysis, CEIP, diagnostics, feedback, and error reporting.

## Generated PowerShell Conventions

Each action block follows the same pattern:

- Create a new result object with metadata
- Attempt the operation
- Emit a structured outcome
- Treat not-found or already-configured cases as `Skipped` or `Already applied` rather than hard failures

Shared helper functions include:

- `New-ActionResult`
- `Write-ActionResultJson`
- `Complete-ActionResult`
- `Fail-ActionResult`
- `Skip-ActionResult`
- `Partial-ActionResult`
- `AlreadyApplied-ActionResult`
- `Ensure-RegistryPath` when any selected action requires it

This makes result parsing consistent across registry, package, service, process, and scheduled-task operations.

## Output And Reporting

The app produces three useful output forms:

- Live raw execution log
- Structured JSON report
- Human-readable text report

### Raw Log

The raw log contains:

- Informational script lifecycle lines
- Standard PowerShell output
- Stderr lines prefixed for visibility
- Parsed action-result notifications

### JSON Report

The JSON report contains:

- A top-level `summary`
- A `results` array with one object per completed action

This format is the most useful for machine processing or future integration.

### Text Report

The text report is grouped by category and shows:

- Status
- User-facing message
- Details
- Error text when present

## Build And Runtime Requirements

- Java 21
- Maven 3.8+
- Windows 11 for real execution targets
- JavaFX modules resolved through Maven or a local JavaFX SDK at runtime

The intended development run path is:

```bash
mvn javafx:run
```

Packaging is handled through Maven Shade for a fat JAR build, while JavaFX runtime requirements still need to be satisfied at launch time.

## Limitations

- The app performs real system modifications and cannot automatically undo them.
- Some Windows updates may restore certain settings or reinstall some components.
- The action list is curated from the existing source script, not discovered dynamically from the OS.
- The app is Windows-specific in practical use because the PowerShell actions target Windows 11 behavior and registry locations.
- JavaFX WebView increases runtime footprint compared with a pure native or pure console implementation.

## Suggested GitHub Positioning

Short repository description:

`Selective Windows 11 debloater with a JavaFX + WebView UI, PowerShell preview, profile import/export, live logs, and structured execution reports.`

Short project summary:

Windows 11 Debloater turns a large debloat PowerShell script into a safer desktop workflow. Instead of running everything at once, it exposes each supported operation as an individual action, validates selections in Java, generates a temporary script from trusted blocks only, and shows live results with exportable reports.

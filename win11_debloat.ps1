# ============================================================
#  Windows 11 Debloat + AI Removal Script
#  Updated: 2026 — compatible with 24H2 / 25H2
#  Run as Administrator
# ============================================================

# Require admin
If (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Warning "Please run this script as Administrator!"
    Exit
}

# Helper to safely create registry path
function Ensure-RegistryPath($path) {
    If (!(Test-Path $path)) {
        New-Item -Path $path -Force | Out-Null
    }
}

# ============================================================
#  SAFETY: Create a System Restore Point first
# ============================================================
Write-Host "`n[0/7] Creating System Restore Point..." -ForegroundColor Yellow
Enable-ComputerRestore -Drive "C:\" -ErrorAction SilentlyContinue
Checkpoint-Computer -Description "Pre-Debloat Restore Point" -RestorePointType "MODIFY_SETTINGS" -ErrorAction SilentlyContinue
Write-Host "  Restore point created (or already exists)." -ForegroundColor Gray

# ============================================================
#  SECTION 1: Remove Bloat Apps
# ============================================================
Write-Host "`n[1/7] Removing bloat apps..." -ForegroundColor Cyan

$apps = @(
    # "*xbox*" intentionally excluded — required for Forza Horizon and Xbox Game Pass
    "*teams*",
    "*cortana*",
    "*clipchamp*",
    "*bing*",
    "*solitaire*",
    "*disney*",
    "*spotify*",
    "*skype*",
    "*news*",
    "*weather*",
    "*maps*",
    "*gethelp*",
    "*feedbackhub*",
    "*mixedreality*",
    "*officehub*",
    "*onenote*",
    "*zune*",
    "*zunevideo*",
    "*people*",
    "*wallet*",
    "*soundrecorder*",
    "*devhome*",           # Dev Home (added in 23H2+)
    "*windowsterminal*"    # Optional: remove if you prefer a third-party terminal
)

foreach ($app in $apps) {
    Write-Host "  Removing $app..." -ForegroundColor Gray
    Get-AppxPackage $app -AllUsers | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue
    Get-AppxProvisionedPackage -Online | Where-Object DisplayName -like $app | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue
}

# ============================================================
#  SECTION 2: Remove Widgets (Windows Web Experience Pack)
# ============================================================
Write-Host "`n[2/7] Removing Widgets..." -ForegroundColor Cyan

# Remove the entire Web Experience Pack (kills Widgets completely)
Get-AppxPackage *WebExperience* -AllUsers | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue
Get-AppxProvisionedPackage -Online | Where-Object DisplayName -like "*WebExperience*" | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue

# Hide Widgets taskbar button (fallback if package removal fails)
Set-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced" -Name "TaskbarDa" -Value 0 -ErrorAction SilentlyContinue

# Disable Widgets news/MSN feed via policy
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Dsh"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Dsh" -Name "AllowNewsAndInterests" -Value 0

# ============================================================
#  SECTION 3: Remove Copilot & AI Apps
# ============================================================
Write-Host "`n[3/7] Removing Copilot and AI apps..." -ForegroundColor Cyan

$aiApps = @(
    "*copilot*",
    "*windowsai*",
    "Microsoft.Windows.Copilot",   # Exact package name (more reliable)
    "Microsoft.Copilot"            # Alternate package name used in newer builds
)

foreach ($app in $aiApps) {
    Write-Host "  Removing $app..." -ForegroundColor Gray
    Get-AppxPackage $app -AllUsers | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue
    Get-AppxProvisionedPackage -Online | Where-Object { $_.DisplayName -like $app -or $_.PackageName -like $app } | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue
}

# ============================================================
#  SECTION 4: Disable AI Features via Registry
# ============================================================
Write-Host "`n[4/7] Disabling AI features via registry..." -ForegroundColor Cyan

# Copilot policies
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsCopilot"
Ensure-RegistryPath "HKCU:\Software\Policies\Microsoft\Windows\WindowsCopilot"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsCopilot" -Name "TurnOffWindowsCopilot" -Value 1
Set-ItemProperty -Path "HKCU:\Software\Policies\Microsoft\Windows\WindowsCopilot" -Name "TurnOffWindowsCopilot" -Value 1

# Windows Recall (AI screenshot memory)
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsAI"
Ensure-RegistryPath "HKCU:\SOFTWARE\Policies\Microsoft\Windows\WindowsAI"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsAI" -Name "DisableAIDataAnalysis" -Value 1
Set-ItemProperty -Path "HKCU:\SOFTWARE\Policies\Microsoft\Windows\WindowsAI" -Name "DisableAIDataAnalysis" -Value 1

# Hide Copilot taskbar button
Set-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced" -Name "ShowCopilotButton" -Value 0

# Remove Copilot from right-click context menu
Remove-Item -Path "HKCU:\Software\Microsoft\Windows\Shell\ContextMenuHandlers\AskCopilot" -Recurse -ErrorAction SilentlyContinue

# Disable Bing/AI in Search
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Search" -Name "BingSearchEnabled" -Value 0
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Search" -Name "CortanaConsent" -Value 0

Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\Windows Search"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\Windows Search" -Name "DisableWebSearch" -Value 1

# Disable AI-powered dynamic search box
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\SearchSettings" -Name "IsDynamicSearchBoxEnabled" -Value 0 -ErrorAction SilentlyContinue

# Disable suggested/smart clipboard actions
Ensure-RegistryPath "HKCU:\Software\Microsoft\Windows\CurrentVersion\SmartActionPlatform\SmartClipboard"
Set-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\SmartActionPlatform\SmartClipboard" -Name "Disabled" -Value 1

# ============================================================
#  SECTION 5: Remove OneDrive
# ============================================================
Write-Host "`n[5/7] Removing OneDrive..." -ForegroundColor Cyan

# Stop OneDrive
taskkill /f /im OneDrive.exe 2>$null

# Uninstall OneDrive
$onedrive = "$env:SystemRoot\SysWOW64\OneDriveSetup.exe"
If (!(Test-Path $onedrive)) { $onedrive = "$env:SystemRoot\System32\OneDriveSetup.exe" }
If (Test-Path $onedrive) {
    Start-Process $onedrive "/uninstall" -NoNewWindow -Wait
    Write-Host "  OneDrive uninstalled." -ForegroundColor Gray
} Else {
    Write-Host "  OneDrive installer not found, may already be removed." -ForegroundColor Gray
}

# Remove OneDrive from startup
Remove-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Run" -Name "OneDrive" -ErrorAction SilentlyContinue

# Remove OneDrive from File Explorer sidebar
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\OneDrive"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\OneDrive" -Name "DisableFileSyncNGSC" -Value 1

# ============================================================
#  SECTION 6: Kill Ads & Sponsored Content
# ============================================================
Write-Host "`n[6/7] Removing ads and sponsored content..." -ForegroundColor Cyan

$cdm = "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\ContentDeliveryManager"
Ensure-RegistryPath $cdm

# Disable all tips, suggestions, and promoted apps
Set-ItemProperty -Path $cdm -Name "SubscribedContent-338389Enabled"  -Value 0  # Tips and suggestions
Set-ItemProperty -Path $cdm -Name "SubscribedContent-310093Enabled"  -Value 0  # Spotlight tips
Set-ItemProperty -Path $cdm -Name "SubscribedContent-338388Enabled"  -Value 0  # Start menu suggestions
Set-ItemProperty -Path $cdm -Name "SubscribedContent-353698Enabled"  -Value 0  # Timeline suggestions
Set-ItemProperty -Path $cdm -Name "SubscribedContent-338387Enabled"  -Value 0  # Lock screen Spotlight tips
Set-ItemProperty -Path $cdm -Name "SoftLandingEnabled"               -Value 0  # App suggestions
Set-ItemProperty -Path $cdm -Name "SystemPaneSuggestionsEnabled"     -Value 0  # Start menu recommended section
Set-ItemProperty -Path $cdm -Name "ContentDeliveryAllowed"           -Value 0  # Block all content delivery
Set-ItemProperty -Path $cdm -Name "OemPreInstalledAppsEnabled"       -Value 0  # OEM bloat
Set-ItemProperty -Path $cdm -Name "PreInstalledAppsEnabled"          -Value 0  # Pre-installed app promotions
Set-ItemProperty -Path $cdm -Name "PreInstalledAppsEverEnabled"      -Value 0
Set-ItemProperty -Path $cdm -Name "SilentInstalledAppsEnabled"       -Value 0  # Silently installed apps
Set-ItemProperty -Path $cdm -Name "RotatingLockScreenEnabled"        -Value 0  # Disable Spotlight on lock screen
Set-ItemProperty -Path $cdm -Name "RotatingLockScreenOverlayEnabled" -Value 0

# Disable lock screen Windows Spotlight
Set-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Lock Screen" -Name "SlideshowEnabled" -Value 0 -ErrorAction SilentlyContinue

# Disable "Show recommendations" in Start menu
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\Explorer"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\Explorer" -Name "HideRecentlyAddedApps" -Value 1

# Disable File Explorer ads (OneDrive sync banners, "Discover" section)
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\Advanced" -Name "ShowSyncProviderNotifications" -Value 0

# Disable "Meet Now" / Chat icon on taskbar
Set-ItemProperty -Path "HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\Advanced" -Name "TaskbarMn" -Value 0 -ErrorAction SilentlyContinue

# Disable Search highlights (trending/news in search)
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Feeds\DSB" -Name "ShowDynamicContent" -Value 0 -ErrorAction SilentlyContinue
Ensure-RegistryPath "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\SearchSettings"
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\SearchSettings" -Name "IsDynamicSearchBoxEnabled" -Value 0

# ============================================================
#  SECTION 7: Privacy, Telemetry & Services
# ============================================================
Write-Host "`n[7/7] Applying privacy, telemetry and service tweaks..." -ForegroundColor Cyan

# Disable telemetry
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\DataCollection"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\DataCollection" -Name "AllowTelemetry" -Value 0

# Disable advertising ID
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\AdvertisingInfo" -Name "Enabled" -Value 0

# Disable activity history (Timeline)
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\System"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\System" -Name "EnableActivityFeed" -Value 0
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\System" -Name "PublishUserActivities" -Value 0
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\System" -Name "UploadUserActivities" -Value 0

# Disable location tracking
Ensure-RegistryPath "HKLM:\SOFTWARE\Policies\Microsoft\Windows\LocationAndSensors"
Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\LocationAndSensors" -Name "DisableLocation" -Value 1

# Disable app diagnostics (apps reading other apps' diagnostics info)
Set-ItemProperty -Path "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\DeviceAccess\Global\{2297E4E2-5DBE-466D-A12B-0F8286F0D9CA}" -Name "Value" -Type String -Value "Deny" -ErrorAction SilentlyContinue

# Disable SysMain (Superfetch) - less useful on SSDs
Stop-Service -Name SysMain -Force -ErrorAction SilentlyContinue
Set-Service -Name SysMain -StartupType Disabled -ErrorAction SilentlyContinue

# Disable Fax service
Stop-Service -Name Fax -Force -ErrorAction SilentlyContinue
Set-Service -Name Fax -StartupType Disabled -ErrorAction SilentlyContinue

# Disable Remote Registry (security hardening)
Stop-Service -Name RemoteRegistry -Force -ErrorAction SilentlyContinue
Set-Service -Name RemoteRegistry -StartupType Disabled -ErrorAction SilentlyContinue

# Disable Scheduled Tasks that re-install bloat or push notifications
$tasksToDisable = @(
    "\Microsoft\Windows\Application Experience\Microsoft Compatibility Appraiser",
    "\Microsoft\Windows\Application Experience\ProgramDataUpdater",
    "\Microsoft\Windows\Autochk\Proxy",
    "\Microsoft\Windows\Customer Experience Improvement Program\Consolidator",
    "\Microsoft\Windows\Customer Experience Improvement Program\UsbCeip",
    "\Microsoft\Windows\DiskDiagnostic\Microsoft-Windows-DiskDiagnosticDataCollector",
    "\Microsoft\Windows\Feedback\Siuf\DmClient",
    "\Microsoft\Windows\Feedback\Siuf\DmClientOnScenarioDownload",
    "\Microsoft\Windows\Windows Error Reporting\QueueReporting"
)

foreach ($task in $tasksToDisable) {
    Disable-ScheduledTask -TaskName $task -ErrorAction SilentlyContinue | Out-Null
    Write-Host "  Disabled task: $task" -ForegroundColor Gray
}

# ============================================================
#  Done
# ============================================================
Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  All done! Please restart your PC for changes to take effect." -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  NOTES:" -ForegroundColor Yellow
Write-Host "  - Major Windows Updates (24H2, 25H2) may re-enable some of this." -ForegroundColor Yellow
Write-Host "  - Re-run this script after feature updates if needed." -ForegroundColor Yellow
Write-Host "  - A restore point was created at the start if you need to roll back." -ForegroundColor Yellow
Write-Host ""

@echo off
setlocal

:: ── Configuration ───────────────────────────────────────────────────
set "JDK_HOME=C:\Program Files\Java\jdk-25.0.2"
set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\plugins\maven\lib\maven3\bin\mvn.cmd"
set "APP_NAME=Win11Debloater"
set "MAIN_CLASS=debloater.Launcher"
set "MAIN_JAR=win11-debloater-1.0.0.jar"
set "OUTPUT_DIR=%~dp0release\app-image"

:: Use the configured JDK for both Maven and jpackage
set "JAVA_HOME=%JDK_HOME%"

:: ── Step 1: Build the shaded JAR ────────────────────────────────────
echo.
echo === Building shaded JAR ===
call "%MVN_CMD%" -f "%~dp0pom.xml" clean package -q
if errorlevel 1 (
    echo ERROR: Maven build failed.
    exit /b 1
)
echo Build successful: target\%MAIN_JAR%

:: ── Step 2: Clean previous app-image ────────────────────────────────
if exist "%OUTPUT_DIR%\%APP_NAME%" (
    echo Removing previous app-image...
    rmdir /s /q "%OUTPUT_DIR%\%APP_NAME%"
)

:: ── Step 3: Create app-image with jpackage ──────────────────────────
echo.
echo === Creating portable app-image with jpackage ===
"%JDK_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --input "%~dp0target" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --dest "%OUTPUT_DIR%" ^
    --java-options "--add-opens javafx.web/javafx.scene.web=ALL-UNNAMED"
if errorlevel 1 (
    echo ERROR: jpackage failed.
    exit /b 1
)

echo.
echo === Done ===
echo Portable app created at: %OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
echo Copy the entire "%APP_NAME%" folder to your target machine.

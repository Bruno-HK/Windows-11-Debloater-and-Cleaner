package debloater.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Checks whether the current Java process is running with Windows Administrator privileges.
 * Uses a PowerShell one-liner to query the Windows security principal.
 */
public class AdminChecker {

    private static Boolean cachedResult = null;

    /**
     * Returns true if the app is running as Administrator.
     * Caches the result after the first check since it cannot change during the session.
     */
    public static boolean isAdmin() {
        if (cachedResult != null) {
            return cachedResult;
        }
        try {
            // Use PowerShell to check if the current user has the Administrator role
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-Command",
                "([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]'Administrator')"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }
            process.waitFor();

            cachedResult = "True".equalsIgnoreCase(output != null ? output.trim() : "");
        } catch (Exception e) {
            System.err.println("AdminChecker: Failed to determine admin status: " + e.getMessage());
            cachedResult = false;
        }
        return cachedResult;
    }

    /**
     * Attempts to restart the application as Administrator using UAC elevation.
     * Launches a new elevated process and exits the current one.
     */
    public static void restartAsAdmin() {
        try {
            // Get the path to the current Java executable
            String javaBin = ProcessHandle.current().info().command().orElse("java");

            // Get the classpath and main class
            String classpath = System.getProperty("java.class.path");
            String mainClass = "debloater.MainApp";

            // Build the command to run elevated via PowerShell Start-Process -Verb RunAs
            String javaCommand = String.format(
                "\"%s\" -cp \"%s\" %s",
                javaBin, classpath, mainClass
            );

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-Command",
                "Start-Process -FilePath '" + javaBin + "' " +
                "-ArgumentList '-cp','" + classpath + "','" + mainClass + "' " +
                "-Verb RunAs"
            );
            pb.start();

            // Exit the current non-elevated instance
            System.exit(0);
        } catch (Exception e) {
            System.err.println("AdminChecker: Failed to restart as admin: " + e.getMessage());
        }
    }
}

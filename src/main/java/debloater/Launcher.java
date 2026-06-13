package debloater;

/**
 * Non-JavaFX entry point for fat JAR distribution.
 * JavaFX Application subclasses cannot be launched directly from a shaded JAR
 * because the JVM checks for JavaFX modules before calling main().
 * This class bypasses that check by not extending Application.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

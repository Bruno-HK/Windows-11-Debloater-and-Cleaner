package debloater;

import debloater.bridge.AppBridge;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;

/**
 * Main JavaFX application for the Windows 11 Debloater.
 *
 * Architecture:
 * - JavaFX provides the desktop shell (window, title bar, icon).
 * - A WebView loads a bundled local HTML/CSS/JS UI.
 * - The AppBridge is exposed to JavaScript as window.bridge.
 * - All PowerShell generation and execution happens in Java.
 * - The WebView never loads external URLs, CDNs, or remote resources.
 */
public class MainApp extends Application {

    private static final String APP_TITLE = "Windows 11 Debloater";
    private static final double DEFAULT_WIDTH = 1200;
    private static final double DEFAULT_HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) {
        // Create the WebView — this hosts the HTML UI
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Disable context menu in WebView (no "View Source" etc.)
        webView.setContextMenuEnabled(false);

        // Create the bridge between Java and JavaScript
        AppBridge bridge = new AppBridge(primaryStage);
        bridge.setWebEngine(webEngine);

        // Block external navigation — only allow local resource URLs
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && !newUrl.startsWith("file:") && !newUrl.startsWith("about:") && !newUrl.isEmpty()) {
                System.err.println("MainApp: Blocked external navigation to: " + newUrl);
                // Navigate back to the local page
                webEngine.loadContent("<html><body style='background:#1a1a2e;color:#fff;font-family:sans-serif;padding:40px;'>"
                    + "<h2>External navigation blocked</h2>"
                    + "<p>This application only loads local resources.</p></body></html>");
            }
        });

        // When the page finishes loading, inject the Java bridge into JavaScript
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Expose the AppBridge as window.bridge in JavaScript
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("bridge", bridge);

                // Signal to JS that the bridge is ready
                webEngine.executeScript("if (typeof onBridgeReady === 'function') onBridgeReady();");
            }
        });

        // Load the bundled HTML UI from resources
        URL htmlUrl = getClass().getResource("/web/index.html");
        if (htmlUrl != null) {
            webEngine.load(htmlUrl.toExternalForm());
        } else {
            System.err.println("MainApp: Could not find /web/index.html in resources!");
            webEngine.loadContent("<html><body style='background:#1a1a2e;color:#e94560;font-family:sans-serif;padding:40px;'>"
                + "<h1>Error</h1><p>Could not load UI resources. Ensure /web/index.html exists in the classpath.</p>"
                + "</body></html>");
        }

        // Set up the scene and stage
        StackPane root = new StackPane(webView);
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
